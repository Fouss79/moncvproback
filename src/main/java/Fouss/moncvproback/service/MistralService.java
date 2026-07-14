package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.CvRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MistralService {

    private final WebClient webClient;

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



        String prompt = """
        Tu es un expert RH spécialisé dans la création de CV modernes compatibles ATS.

        Analyse le CV suivant et améliore-le.

        Objectifs :
        - Générer un profil professionnel impactant.
        - Reformuler les expériences avec des verbes d'action.
        - Valoriser les compétences techniques.
        - Corriger les erreurs linguistiques.
        - Adapter le CV pour un poste de Développeur Full Stack.
                IMPORTANT :
                - Ne donne aucune explication.
                - Ne mets aucune phrase avant ou après.
                - Retourne uniquement un objet JSON valide.
                - Aucun Markdown.

        Retourne uniquement un JSON valide avec cette structure :

        {
          "profil": "",
          "competences": [],
          "experiences": [],
          "resume": ""
        }


        INFORMATIONS DU CANDIDAT :

        Nom :
        %s

        Prénom :
        %s

        Titre :
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
                        cv.getNom(),
                        cv.getPrenom(),
                        cv.getTitre(),
                        cv.getProfil(),

                        String.join(", ",
                                Optional.ofNullable(cv.getCompetences())
                                        .orElse(Collections.emptyList())),

                        String.join(", ",
                                Optional.ofNullable(cv.getLogiciels())
                                        .orElse(Collections.emptyList())),

                        String.join(", ",
                                Optional.ofNullable(cv.getSoftSkills())
                                        .orElse(Collections.emptyList())),

                        String.join(", ",
                                Optional.ofNullable(cv.getLangues())
                                        .orElse(Collections.emptyList())),

                        formations,

                        experiences,

                        String.join(", ",
                                Optional.ofNullable(cv.getCertifications())
                                        .orElse(Collections.emptyList())),

                        projets
                );


        return generate(prompt);
    }
}