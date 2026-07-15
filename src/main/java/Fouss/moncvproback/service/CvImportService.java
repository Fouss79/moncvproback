package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.CvFullDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CvImportService {

    private final MistralService mistralService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extrait le texte d'un CV PDF, le fait analyser/restructurer par Mistral,
     * puis désérialise directement le JSON obtenu dans un CvFullDTO.
     *
     * ⚠️ On réutilise volontairement CvFullDTO (au lieu d'un nouveau DTO
     * "import") : sa forme (competences/langues en {nom, niveau},
     * softSkills/loisirs en List<String>, contact/formations/experiences
     * imbriqués) correspond exactement à ce que le frontend sait déjà
     * consommer via loadCv(). Les champs non déductibles d'un CV texte
     * (couleur, template, photoUrl) resteront simplement à null : c'est
     * au frontend de les préserver en fusionnant avec le formData actuel.
     */
    public CvFullDTO importFromPdf(MultipartFile file) {

        String rawText = extractText(file);

        if (rawText == null || rawText.isBlank()) {
            throw new RuntimeException("Le PDF ne contient aucun texte exploitable (peut-être scanné en image ?)");
        }

        String prompt = buildPrompt(rawText);
        String json = mistralService.generate(prompt);

        try {
            return objectMapper.readValue(json, CvFullDTO.class);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'analyser le CV importé : réponse IA invalide", e);
        }
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier PDF", e);
        }
    }

    private String buildPrompt(String rawText) {
        return """
        Tu es un expert RH spécialisé dans l'extraction structurée de données de CV.

        Voici le texte brut extrait d'un CV au format PDF. Analyse-le et restructure
        toutes les informations dans le format JSON ci-dessous.

        IMPORTANT :
        - Ne donne aucune explication, aucune phrase avant ou après.
        - Retourne uniquement un objet JSON valide, sans Markdown, sans ```json.
        - Si une information est absente du texte, mets une chaîne vide "" ou un tableau vide [].
        - Le champ "niveau" (compétences et langues) doit être l'une de ces valeurs :
          "Débutant", "Intermédiaire", "Avancé", "Expert".
        - N'invente aucune information qui ne figure pas dans le texte.

        Format JSON attendu exactement :

        {
          "nom": "",
          "prenom": "",
          "titre": "",
          "profil": "",
          "contact": {
            "telephone": "",
            "email": "",
            "adresse": "",
            "linkedin": "",
            "github": "",
            "site": ""
          },
          "competences": [ { "nom": "", "niveau": "" } ],
          "langues": [ { "nom": "", "niveau": "" } ],
          "softSkills": [ "" ],
          "loisirs": [ "" ],
          "formations": [ { "diplome": "", "ecole": "", "annee": "" } ],
          "experiences": [
            {
              "poste": "",
              "entreprise": "",
              "dates": "",
              "duree": "",
              "responsabilites": [ "" ]
            }
          ]
        }

        TEXTE DU CV :
        %s
        """.formatted(rawText);
    }
}