package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.CvRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MistralService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mistral.api.key}")
    private String apiKey;

    @Value("${mistral.api.url}")
    private String apiUrl;

    @Value("${mistral.api.model}")
    private String model;


    public String generate(String prompt) {

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.7
        );


        JsonNode response = webClient.post()
                .uri(apiUrl)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();


        if (response == null) {
            throw new RuntimeException("Réponse Mistral vide");
        }


        String content = response
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        content = content
                .replace("```json", "")
                .replace("```", "")
                .trim();


// récupérer uniquement le JSON
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");

        if (start >= 0 && end >= 0) {
            content = content.substring(start, end + 1);
        }

        return content;
    }



    public String generateCv(CvRequestDTO cv) {


        List<CvRequestDTO.ExperienceDTO> experiencesList =
                Optional.ofNullable(cv.getExperiences())
                        .orElse(Collections.emptyList());


        String experiences = experiencesList.stream()
                .map(exp ->
                        """
                        Poste : %s
                        Entreprise : %s
                        Période : %s
                        Responsabilités :
                        %s
                        """
                                .formatted(
                                        exp.getPoste(),
                                        exp.getEntreprise(),
                                        exp.getDates(),
                                        String.join("\n- ",
                                                Optional.ofNullable(exp.getResponsabilites())
                                                        .orElse(Collections.emptyList()))
                                )
                )
                .reduce("", (a, b) -> a + "\n" + b);



        List<CvRequestDTO.FormationDTO> formationsList =
                Optional.ofNullable(cv.getFormations())
                        .orElse(Collections.emptyList());


        String formations = formationsList.stream()
                .map(f ->
                        "- %s (%s) - %s"
                                .formatted(
                                        f.getDiplome(),
                                        f.getEcole(),
                                        f.getAnnee()
                                )
                )
                .reduce("", (a,b) -> a + "\n" + b);



        List<CvRequestDTO.ProjetDTO> projetsList =
                Optional.ofNullable(cv.getProjets())
                        .orElse(Collections.emptyList());


        String projets = projetsList.stream()
                .map(p ->
                        """
                        Projet : %s
                        Description : %s
                        Technologies : %s
                        """
                                .formatted(
                                        p.getNom(),
                                        p.getDescription(),
                                        String.join(", ",
                                                Optional.ofNullable(p.getTechnologies())
                                                        .orElse(Collections.emptyList()))
                                )
                )
                .reduce("", (a,b) -> a + "\n" + b);


        // ✅ competences et langues sont désormais des List<SkillDTO> (nom + niveau)
        // et non plus des List<String> : on les transforme d'abord en texte.
        String competencesStr = Optional.ofNullable(cv.getCompetences())
                .orElse(Collections.emptyList())
                .stream()
                .map(c -> c.getNiveau() != null && !c.getNiveau().isBlank()
                        ? "%s (%s)".formatted(c.getNom(), c.getNiveau())
                        : c.getNom())
                .collect(Collectors.joining(", "));

        String languesStr = Optional.ofNullable(cv.getLangues())
                .orElse(Collections.emptyList())
                .stream()
                .map(l -> l.getNiveau() != null && !l.getNiveau().isBlank()
                        ? "%s (%s)".formatted(l.getNom(), l.getNiveau())
                        : l.getNom())
                .collect(Collectors.joining(", "));


        String posteVise = (cv.getTitre() != null && !cv.getTitre().isBlank())
                ? cv.getTitre()
                : "le poste actuel du candidat";

        String prompt = """
        Tu es un expert RH spécialisé dans l'optimisation de CV compatibles ATS,
        tous secteurs et métiers confondus (commercial, technique, marketing,
        RH, finance, etc.).

        Analyse le CV suivant, tel qu'il a été renseigné par le candidat dans
        son formulaire, et améliore-le pour le poste visé : "%s".

        Objectifs :
        - Générer un profil professionnel impactant et adapté au métier du
          candidat (ne bascule jamais vers un autre métier que celui indiqué
          par son titre/poste et ses expériences réelles).
        - Reformuler les expériences avec des verbes d'action et des résultats
          chiffrés quand l'information le permet, sans jamais inventer de
          chiffres, dates, entreprises ou faits absents du texte fourni.
        - Mettre en valeur les compétences et réalisations propres à son
          domaine (commercial, technique, ou autre selon le cas).
        - Corriger les erreurs linguistiques et de formulation.
        - Rester fidèle aux informations fournies : ne pas ajouter
          d'expérience, de formation ou de compétence qui ne figure pas dans
          les données ci-dessous.

        IMPORTANT :
        - Ne donne aucune explication.
        - Ne mets aucune phrase avant ou après.
        - Retourne uniquement un objet JSON valide.
        - Aucun Markdown.

        Retourne uniquement un JSON valide avec exactement cette structure :

        {
          "profil": "",
          "competences": [
            { "nom": "", "niveau": "" }
          ],
          "experiences": [
            {
              "poste": "",
              "entreprise": "",
              "dates": "",
              "responsabilites": [""]
            }
          ],
          "resume": ""
        }

        Où :
        - "competences" reprend et enrichit la liste de compétences fournie
          (conserve le "niveau" d'origine s'il existe, sinon estime-le à
          partir du contexte : "Débutant", "Intermédiaire", "Avancé" ou
          "Expert").
        - "experiences" reformule chaque expérience fournie (même nombre
          d'expériences en entrée qu'en sortie), en gardant poste, entreprise
          et dates identiques, et en réécrivant uniquement les
          responsabilités.
        - "resume" est une synthèse en 2-3 phrases des points forts du
          candidat pour le poste visé.


        INFORMATIONS DU CANDIDAT :

        Nom :
        %s

        Prénom :
        %s

        Titre / poste visé :
        %s


        Profil actuel :
        %s


        Compétences :
        %s


        Logiciels :
        %s


        Soft Skills :
        %s


        Langues :
        %s


        Formations :
        %s


        Expériences :
        %s


        Certifications :
        %s


        Projets :
        %s

        """
                .formatted(
                        posteVise,

                        cv.getNom(),
                        cv.getPrenom(),
                        cv.getTitre(),
                        cv.getProfil(),

                        competencesStr,

                        String.join(", ",
                                Optional.ofNullable(cv.getLogiciels())
                                        .orElse(Collections.emptyList())),

                        String.join(", ",
                                Optional.ofNullable(cv.getSoftSkills())
                                        .orElse(Collections.emptyList())),

                        languesStr,

                        formations,

                        experiences,

                        String.join(", ",
                                Optional.ofNullable(cv.getCertifications())
                                        .orElse(Collections.emptyList())),

                        projets
                );


        return generate(prompt);
    }


    /**
     * Suggère des compétences pertinentes à ajouter, en se basant sur
     * l'ensemble du profil du candidat (titre, profil, expériences,
     * formations) et en excluant celles déjà renseignées.
     */
    public List<String> suggestCompetences(CvRequestDTO cv) {

        List<CvRequestDTO.ExperienceDTO> experiencesList =
                Optional.ofNullable(cv.getExperiences()).orElse(Collections.emptyList());

        String experiencesStr = experiencesList.stream()
                .map(exp -> "%s chez %s (%s)".formatted(
                        Optional.ofNullable(exp.getPoste()).orElse(""),
                        Optional.ofNullable(exp.getEntreprise()).orElse(""),
                        Optional.ofNullable(exp.getDates()).orElse("")
                ))
                .collect(Collectors.joining("; "));

        List<CvRequestDTO.FormationDTO> formationsList =
                Optional.ofNullable(cv.getFormations()).orElse(Collections.emptyList());

        String formationsStr = formationsList.stream()
                .map(f -> "%s (%s)".formatted(
                        Optional.ofNullable(f.getDiplome()).orElse(""),
                        Optional.ofNullable(f.getEcole()).orElse("")
                ))
                .collect(Collectors.joining("; "));

        String competencesActuelles = Optional.ofNullable(cv.getCompetences())
                .orElse(Collections.emptyList())
                .stream()
                .map(CvRequestDTO.SkillDTO::getNom)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(", "));

        String prompt = """
        Tu es un expert RH. En te basant sur le profil complet du candidat
        ci-dessous (titre, profil, expériences, formations), propose des
        compétences pertinentes à AJOUTER à son CV, qu'il n'a pas encore
        renseignées.

        IMPORTANT :
        - Ne suggère JAMAIS une compétence déjà présente dans sa liste actuelle.
        - Reste cohérent avec son métier réel (ne propose pas de compétences
          d'un domaine différent du sien).
        - Propose entre 8 et 12 compétences précises et concrètes (outils,
          logiciels, méthodes, certifications...), pas de termes vagues comme
          "travail d'équipe" (réservé aux soft skills, pas ici).
        - Ne donne aucune explication, aucun texte avant ou après.
        - Retourne uniquement un objet JSON valide, sans Markdown.

        Format attendu exactement :
        { "suggestions": ["", "", "..."] }

        PROFIL DU CANDIDAT :

        Titre : %s
        Profil : %s
        Expériences : %s
        Formations : %s
        Compétences déjà renseignées (à ne surtout pas reproposer) : %s
        """.formatted(
                Optional.ofNullable(cv.getTitre()).orElse(""),
                Optional.ofNullable(cv.getProfil()).orElse(""),
                experiencesStr.isBlank() ? "Aucune" : experiencesStr,
                formationsStr.isBlank() ? "Aucune" : formationsStr,
                competencesActuelles.isBlank() ? "Aucune" : competencesActuelles
        );

        String json = generate(prompt);

        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            node.path("suggestions").forEach(n -> result.add(n.asText()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'analyser les suggestions IA de compétences", e);
        }
    }
}