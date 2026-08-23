package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.LoginRequest;
import Fouss.moncvproback.dto.RegisterRequest;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.repository.UserRepository;
import Fouss.moncvproback.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // Injecté depuis application.properties (ou variable d'environnement)
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/activate")
    public RedirectView activate(@RequestParam String token) {
        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Token invalide"));

            user.setEnabled(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return new RedirectView(frontendUrl + "/login?activated=true");

        } catch (RuntimeException e) {
            return new RedirectView(frontendUrl + "/login?activated=false");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}