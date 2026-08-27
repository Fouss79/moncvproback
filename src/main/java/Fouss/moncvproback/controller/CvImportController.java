package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CvFullDTO;
import Fouss.moncvproback.exception.DownloadLimitExceededException;
import Fouss.moncvproback.service.CvImportService;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoint dédié à l'import d'un CV existant (PDF) pour pré-remplir le wizard.
 * Ne persiste rien en base : l'utilisateur passe toujours par le flux
 * habituel (wizard -> "Valider & enregistrer" -> PUT/POST /cv/...) pour
 * sauvegarder définitivement les données extraites.
 *
 * ✅ NOUVEAU — Réservé aux abonnés Pro/Premium ("Import et analyse IA d'un CV
 * existant" est un avantage payant). Le frontend bloque déjà ce bouton pour
 * les utilisateurs FREE, mais rien n'empêchait jusqu'ici d'appeler cet
 * endpoint directement (ex: via Postman avec son propre token JWT) — cette
 * vérification ferme cette porte côté serveur, qui est la seule vraiment
 * fiable.
 */
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvImportController {

    private final CvImportService cvImportService;
    private final PaymentService paymentService;

    @PostMapping(value = "/import-pdf", consumes = "multipart/form-data")
    public ResponseEntity<?> importPdf(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            paymentService.requireActiveSubscription(authentication.getName());
        } catch (DownloadLimitExceededException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }

        CvFullDTO parsed = cvImportService.importFromPdf(file);
        return ResponseEntity.ok(parsed);
    }
}