package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.CvRequestDTO;
import Fouss.moncvproback.exception.AiServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import reactor.util.retry.Retry;

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


        JsonNode response;
        try {
            response = webClient.post()
                    .uri(apiUrl)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    // ⚠️ Absorbe les échecs réseau/DNS transitoires (ex: le
                    // cache DNS interne de Netty qui reste bloqué sur un
                    // échec ponctuel survenu au démarrage du serveur — le
                    // symptôme classique "ça marche après un redémarrage").
                    // 3 tentatives, backoff 2s/4s/8s. On ne retente PAS les
                    // erreurs HTTP 4xx (hors 429) : une clé API invalide ou
                    // une requête malformée ne sera jamais corrigée par un
                    // simple réessai.
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .filter(this::estErreurTransitoire)
                                    // Sans ceci, Reactor enveloppe l'échec final
                                    // dans un RetryExhaustedException générique,
                                    // et les catch WebClientRequestException /
                                    // WebClientResponseException ci-dessous ne
                                    // matcheraient plus : on repropage donc
                                    // l'exception d'origine telle quelle.
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                    )
                    .block();
        } catch (WebClientRequestException e) {
            // Erreur AVANT même d'atteindre Mistral : DNS injoignable,
            // pas de connexion internet, timeout réseau, pare-feu...
            // (ex: DnsNameResolverTimeoutException sur api.mistral.ai)
            // — persiste malgré les 3 tentatives de retry ci-dessus.
            throw new AiServiceUnavailableException(
                    "Le service IA est temporairement injoignable (problème de connexion réseau). Réessayez dans quelques instants.",
                    e
            );
        } catch (WebClientResponseException e) {
            // Mistral a répondu, mais avec un code d'erreur HTTP
            // (401 clé invalide, 429 quota dépassé, 5xx panne côté Mistral...)
            throw new AiServiceUnavailableException(
                    "Le service IA a renvoyé une erreur (code %d). Réessayez plus tard.".formatted(
                            e.getStatusCode().value()),
                    e
            );
        } catch (Exception e) {
            throw new AiServiceUnavailableException(
                    "Erreur inattendue lors de l'appel au service IA. Réessayez dans quelques instants.",
                    e
            );
        }


        if (response == null) {
            throw new AiServiceUnavailableException("Réponse vide du service IA. Réessayez dans quelques instants.");
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


    /**
     * Détermine si une erreur mérite un nouvel essai :
     * - Erreurs réseau/DNS (WebClientRequestException) : OUI — c'est
     *   exactement le cas du cache DNS Netty bloqué sur un échec ponctuel.
     * - 429 (quota dépassé) ou 5xx (panne côté Mistral) : OUI, ça peut se
     *   résorber tout seul en quelques secondes.
     * - 4xx hors 429 (401 clé invalide, 400 requête malformée...) : NON —
     *   réessayer ne changera rien, c'est une erreur de configuration/appel.
     */
    private boolean estErreurTransitoire(Throwable t) {
        if (t instanceof WebClientRequestException) {
            return true;
        }
        if (t instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
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
        String loisirsStr = Optional.ofNullable(cv.getLoisirs())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        String interetsStr = Optional.ofNullable(cv.getInterets())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        // ⚠️ Cette liste DOIT rester synchronisée avec VERBES_ACTION dans
        // lib/atsVerbes.js (frontend, utilisé par ExperienceList.jsx et
        // AnalyseCV.jsx pour le score ATS). Volontairement large (tous
        // domaines) pour ne pas contraindre l'IA à recycler toujours les
        // mêmes 20 verbes ; exclut délibérément les formulations faibles
        // ("participé à", "contribué à", "responsable de"...).
        String verbesAutorises = String.join(", ",
                // Développement / Technique
                "développé", "conçu", "créé", "implémenté", "déployé",
                "automatisé", "codé", "testé", "intégré", "migré",
                "architecturé", "optimisé", "sécurisé", "digitalisé",
                "modernisé", "maintenu",
                // Management / Leadership
                "dirigé", "géré", "managé", "encadré", "supervisé",
                "coordonné", "piloté", "animé", "fédéré", "mobilisé",
                "formé", "recruté", "mentoré", "délégué",
                // Commercial / Vente
                "négocié", "prospecté", "vendu", "fidélisé", "conquis",
                "signé", "conclu", "closé",
                // Marketing / Communication
                "lancé", "promu", "communiqué", "rédigé", "publié", "organisé",
                // Finance / Gestion
                "budgété", "audité", "analysé", "contrôlé", "réduit",
                "économisé", "arbitré",
                // Opérations / Process
                "structuré", "standardisé", "industrialisé", "rationalisé",
                "simplifié", "harmonisé", "fiabilisé",
                // Projet
                "planifié", "exécuté", "livré", "suivi", "mis en place",
                // Impact / Résultats
                "augmenté", "amélioré", "renforcé", "consolidé", "accéléré",
                "multiplié", "doublé", "triplé", "transformé",
                // Impulsion / Construction
                "établi", "instauré", "initié", "impulsé", "construit",
                "bâti", "conduit", "mené", "orchestré", "élaboré"
        );

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
        - Reformuler chaque ligne de responsabilité pour qu'elle commence
          IMPÉRATIVEMENT par l'un des verbes d'action suivants (aucun autre
          verbe de début de phrase n'est autorisé, pour rester compatible
          avec notre outil interne de notation ATS) :
          %s.
          Si aucun de ces verbes ne correspond exactement à l'action décrite,
          choisis le plus proche sémantiquement dans cette liste plutôt que
          d'en inventer un autre.
        - Ajoute des résultats chiffrés quand l'information le permet, sans
          jamais inventer de chiffres, dates, entreprises ou faits absents du
          texte fourni.
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
                      "duree": "",
                      "responsabilites": [""]
                    }
                  ],
                  "loisirs": [""],
                  "resume": ""
                }
        Où :
        - "competences" reprend et enrichit la liste de compétences fournie
          (conserve le "niveau" d'origine s'il existe, sinon estime-le à
          partir du contexte : "Débutant", "Intermédiaire", "Avancé" ou
          "Expert").
        - "experiences" reformule chaque expérience fournie (même nombre
          d'expériences en entrée qu'en sortie), en gardant poste, entreprise,
          dates et durée identiques, et en réécrivant uniquement les
          responsabilités.
          - "loisirs" reprend tous les loisirs / centres d'intérêt fournis par le candidat.
                  - Ne jamais inventer de loisirs.
                  - Ne supprimer aucun loisir fourni.
                  - Conserver le sens des loisirs renseignés.
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
                
                
        Loisirs / Centres d'intérêt :
        %s
                
        Intérêts :
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
                        verbesAutorises,

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

                        loisirsStr,
                        interetsStr,

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
            throw new AiServiceUnavailableException(
                    "Le service IA a renvoyé une réponse invalide pour les suggestions de compétences. Réessayez.",
                    e
            );
        }
    }
}