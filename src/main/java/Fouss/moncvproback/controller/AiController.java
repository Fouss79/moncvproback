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
     * ✅ NOUVEAU — Réservé aux abonnés Pro/Premium ("Suggestions IA
     * complètes" est un avantage payant). Comme pour l'import PDF, le
     * frontend bloque déjà ce bouton pour les FREE, mais rien n'empêchait
     * jusqu'ici d'appeler cet endpoint directement (Postman + token JWT
     * volé/copié) — cette vérification ferme cette porte côté serveur.
     *
     * ⚠️ Il fallait ajouter le paramètre "Authentication authentication" à
     * cette méthode, qui ne l'avait pas — sans lui, impossible de savoir
     * quel utilisateur appelle l'endpoint pour vérifier son abonnement.
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



        @PostMapping("/generate-cover-letter")
        public ResponseEntity<?> generateCoverLetter(
                @RequestBody CoverLetterRequestDTO request
        ) {

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



