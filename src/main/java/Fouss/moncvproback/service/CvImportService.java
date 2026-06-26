package Fouss.moncvproback.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvImportService {

    private final CvParserService cvParserService;

    public CvImportService(CvParserService cvParserService) {
        this.cvParserService = cvParserService;
    }

    public String importCv(MultipartFile file) {

        System.out.println("=== CV IMPORT START ===");
        System.out.println("Nom : " + file.getOriginalFilename());
        System.out.println("Taille : " + file.getSize());

        String text = cvParserService.parse(file);

        System.out.println("=== TEXTE EXTRAIT ===");
        System.out.println(text);

        return text;
    }
}