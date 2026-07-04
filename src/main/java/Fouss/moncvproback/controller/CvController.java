package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CvFullDTO;
import Fouss.moncvproback.entity.Cv;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.repository.CvRepository;
import Fouss.moncvproback.repository.UserRepository;
import Fouss.moncvproback.service.CvImportService;
import Fouss.moncvproback.service.CvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;
    private final CvImportService cvImportService;
    private final UserRepository userRepository;
    private final CvRepository cvRepository;
    // ➕ CREATE CV
    @PostMapping("/{userId}")
    public ResponseEntity<Cv> createCv(
            @PathVariable Long userId,
            @RequestBody Cv cv
    ) {
        return ResponseEntity.ok(cvService.createCv(userId, cv));
    }

    @PostMapping("/import")
    public String importCv(@RequestParam MultipartFile file) {
        return cvImportService.importCv(file);
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<String> uploadPhoto(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path uploadPath = Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.copy(
                file.getInputStream(),
                uploadPath.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING
        );

        return ResponseEntity.ok("/uploads/" + fileName);
    }
    // 📄 GET ALL CV BY USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Cv>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(cvService.getCvsByUser(userId));
    }

    // 🔍 GET CV BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Cv> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cvService.getCvById(id));
    }

    // ✏️ UPDATE CV
    @PutMapping("/{id}")
    public ResponseEntity<Cv> updateCv(
            @PathVariable Long id,
            @RequestBody Cv cv
    ) {
        return ResponseEntity.ok(cvService.updateCv(id, cv));
    }

    // ❌ DELETE CV
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCv(@PathVariable Long id) {
        cvService.deleteCv(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/full/{userId}")
    public ResponseEntity<Cv> saveFullCv(
            @PathVariable Long userId,
            @RequestBody CvFullDTO dto
    ) {

        return ResponseEntity.ok(
                cvService.saveFullCv(userId, dto)
        );
    }
    @GetMapping("/my-cv-full/{userId}")
    public CvFullDTO getCvFull(@PathVariable Long userId) {
        return cvService.getCvFullByUser(userId);
    }
    @GetMapping("/my-cv/full")
    public CvFullDTO getMyCvFull(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return cvService.getCvFullByUser(user.getId());
    }
    @GetMapping("/my-cv")
    public Optional<Cv> getMyCv(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return cvRepository.findFirstByUserId(user.getId());
    }
    @PutMapping("/full/{id}")
    public Cv updateFullCv(@PathVariable Long id, @RequestBody CvFullDTO dto) {
        return cvService.updateFullCv(id, dto);
    }
    @PutMapping("/my-cv")
    public ResponseEntity<Cv> updateMyCv(
            Authentication authentication,
            @RequestBody CvFullDTO dto
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cv updated = cvService.updateFullCv(user.getId(), dto);

        return ResponseEntity.ok(updated);
    }
}