package Fouss.moncvproback.service;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

@Service
public class CvParserService {

    // ================= ENTRY =================
    public String parse(MultipartFile file) {

        try {
            String name = file.getOriginalFilename();

            if (name == null) throw new RuntimeException("Fichier invalide");

            if (name.endsWith(".pdf")) return parsePdf(file);
            if (name.endsWith(".docx")) return parseDocx(file);

            throw new RuntimeException("Format non supporté");

        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing CV: " + e.getMessage(), e);
        }
    }

    // ================= PDF =================
    private String parsePdf(MultipartFile file) throws Exception {

        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            System.out.println("PDF TEXT LENGTH = " + text.length());

            // ✔ PDF normal
            if (text != null && text.trim().length() > 20) {
                return clean(text);
            }

            // ❌ PDF image → OCR
            System.out.println("⚠ PDF IMAGE DETECTÉ → OCR");

            return runOcr(file);
        }
    }

    // ================= OCR SAFE =================
    private String runOcr(MultipartFile file) {

        try {
            File temp = File.createTempFile("cv-", ".pdf");
            file.transferTo(temp);

            Tesseract tesseract = new Tesseract();

            // ✔ IMPORTANT : datapath stable
            File tessBase = extractTessData();

            tesseract.setDatapath(tessBase.getAbsolutePath());
            tesseract.setLanguage("fra+eng");

            // ⚠ sécurité Windows / Linux
            tesseract.setOcrEngineMode(1);

            String result = tesseract.doOCR(temp);

            System.out.println("OCR LENGTH = " + result.length());

            return clean(result);

        } catch (Exception e) {
            System.out.println("OCR FAILED: " + e.getMessage());
            return "";
        }
    }

    // ================= DOCX =================
    private String parseDocx(MultipartFile file) throws Exception {

        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {

            return clean(extractor.getText());
        }
    }

    // ================= CLEAN TEXT =================
    private String clean(String text) {
        return text
                .replace("\u0000", "")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ================= TESSDATA =================
    private File extractTessData() throws IOException {

        File base = Files.createTempDirectory("tessdata").toFile();
        File tessDir = new File(base, "tessdata");

        if (!tessDir.exists()) tessDir.mkdirs();

        copy("tessdata/fra.traineddata", tessDir);
        copy("tessdata/eng.traineddata", tessDir);

        return base;
    }

    // ================= COPY RESOURCE =================
    private void copy(String resource, File dir) throws IOException {

        InputStream is = new ClassPathResource(resource).getInputStream();

        File out = new File(dir, resource.substring(resource.lastIndexOf("/") + 1));

        try (OutputStream os = new FileOutputStream(out)) {
            is.transferTo(os);
        }
    }
}