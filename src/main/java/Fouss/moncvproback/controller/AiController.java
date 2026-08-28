package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CoverLetterRequestDTO;
import Fouss.moncvproback.dto.CvRequestDTO;
import Fouss.moncvproback.exception.DownloadLimitExceededException;
import Fouss.moncvproback.service.MistralService;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final MistralService mistralService;
    private final PaymentService paymentService;

    /**
     * ✅ Réservé aux abonnés Pro/Premium ("Suggestions IA complètes" est un
     * avantage payant). Le frontend bloque déjà ce bouton pour les FREE,
     * mais rien n'empêchait jusqu'ici d'appeler cet endpoint directement
     * (Postman + token JWT volé/copié) — cette vérification ferme cette
     * porte côté serveur.
     */
    @PostMapping("/generate-profile")
    public ResponseEntity<?> generateProfile(
            @RequestBody CvRequestDTO cv,
            Authentication authentication
    ) {
        try {
            paymentService.requireActiveSubscription(authentication.getName());
        } catch (DownloadLimitExceededException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }

        String result = mistralService.generateCv(cv);
        System.out.println("CV reçu = " + cv);

        return ResponseEntity.ok(result);
    }

    /**
     * ✅ NOUVEAU — Réservé aux abonnés Pro/Premium également ("Lettres de
     * motivation IA" est désormais un avantage Pro sur la grille tarifaire).
     * Même vérification que generate-profile ; le paramètre "Authentication"
     * a dû être ajouté (il manquait dans la version d'origine, qui ne
     * vérifiait aucun abonnement).
     */
    @PostMapping("/generate-cover-letter")
    public ResponseEntity<?> generateCoverLetter(
            @RequestBody CoverLetterRequestDTO request,
            Authentication authentication
    ) {
        try {
            paymentService.requireActiveSubscription(authentication.getName());
        } catch (DownloadLimitExceededException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }

        if (request.getCv() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Les informations du CV sont obligatoires."
                    )
            );
        }

        String letter = mistralService.generateCoverLetter(
                request.getCv(),
                request.getPoste(),
                request.getEntreprise(),
                request.getOffre(),
                request.getTon(),
                request.getInformationsSupplementaires()
        );

        return ResponseEntity.ok(
                Map.of(
                        "letter",
                        letter
                )
        );
    }
}