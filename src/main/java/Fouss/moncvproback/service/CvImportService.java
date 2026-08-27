package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.CvFullDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CvImportService {

    private final MistralService mistralService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ NOUVEAU — Chemins candidats pour les données Tesseract selon la
    // distro/version. On prend le premier qui existe réellement sur le
    // système au démarrage, plutôt que de coder en dur un seul chemin fragile.
    private static final String[] TESSDATA_CANDIDATES = {
            System.getenv("TESSDATA_PREFIX"),
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tessdata"
    };

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

        // ✅ NOUVEAU — Si le PDF n'a pas de couche de texte exploitable (cas
        // typique d'un PDF généré par html2canvas/html2pdf.js : une image
        // collée dans le PDF, comme pour les CV exportés par notre propre
        // app), on tente l'OCR avant d'abandonner.
        if (rawText == null || rawText.isBlank()) {
            rawText = extractTextViaOcr(file);
        }

        if (rawText == null || rawText.isBlank()) {
            throw new RuntimeException(
                    "Le PDF ne contient aucun texte exploitable, même après OCR.");
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

    /**
     * ✅ NOUVEAU — Rasterise chaque page du PDF en image, puis fait lire le
     * texte par Tesseract (OCR). Nécessaire pour les PDF "image" (scans,
     * exports html2canvas) qui n'ont pas de couche de texte exploitable par
     * PDFTextStripper.
     *
     * Nécessite le binaire tesseract-ocr + les données de langue installées
     * au niveau du système (voir Dockerfile) — la dépendance Maven tess4j
     * seule ne suffit pas, elle n'est qu'un wrapper Java autour du binaire.
     */
    private String extractTextViaOcr(MultipartFile file) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolveTessdataPath());
        tesseract.setLanguage("fra+eng"); // CV majoritairement en français, parfois en anglais

        StringBuilder result = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);

            int pageCount = document.getNumberOfPages();
            // Sécurité : un CV fait rarement plus de quelques pages ; on
            // limite pour éviter qu'un fichier abusif ne fasse tourner l'OCR
            // indéfiniment et ne dépasse le timeout de la requête HTTP.
            int maxPages = Math.min(pageCount, 5);

            for (int i = 0; i < maxPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                try {
                    result.append(tesseract.doOCR(image)).append("\n");
                } catch (TesseractException e) {
                    throw new RuntimeException("Erreur OCR sur la page " + (i + 1), e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de rasteriser le PDF pour l'OCR", e);
        }

        return result.toString();
    }

    private String resolveTessdataPath() {
        for (String candidate : TESSDATA_CANDIDATES) {
            if (candidate != null && new File(candidate).isDirectory()) {
                return candidate;
            }
        }
        throw new RuntimeException(
                "Données Tesseract introuvables sur le serveur. Vérifie que "
                        + "tesseract-ocr et tesseract-ocr-fra sont bien installés (Dockerfile).");
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
        - Le texte peut provenir d'une reconnaissance optique de caractères (OCR) :
          corrige les erreurs de reconnaissance évidentes (lettres mal lues,
          espaces manquants) sans changer le sens des informations.

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