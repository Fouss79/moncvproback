package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CvFullDTO;
import Fouss.moncvproback.service.CvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint dédié à l'import d'un CV existant (PDF) pour pré-remplir le wizard.
 * Ne persiste rien en base : l'utilisateur passe toujours par le flux
 * habituel (wizard -> "Valider & enregistrer" -> PUT/POST /cv/...) pour
 * sauvegarder définitivement les données extraites.
 */
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvImportController {

    private final CvImportService cvImportService;

    @PostMapping(value = "/import-pdf", consumes = "multipart/form-data")
    public ResponseEntity<CvFullDTO> importPdf(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        CvFullDTO parsed = cvImportService.importFromPdf(file);
        return ResponseEntity.ok(parsed);
    }
}