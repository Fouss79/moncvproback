package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.LoginRequest;
import Fouss.moncvproback.dto.RegisterRequest;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.repository.UserRepository;
import Fouss.moncvproback.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,UserRepository userRepository) {
        this.authService = authService;
        this.userRepository=userRepository;
    }
    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println(user.getId());
        return ResponseEntity.ok(user);
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }


    @GetMapping("/activate")
    public RedirectView activate(@RequestParam String token) {

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        user.setEnabled(true);
        user.setVerificationToken(null);

        userRepository.save(user);

        // Redirige vers la page de login du frontend
        return new RedirectView("https://mon-cv-pro-dypd.vercel.app/login?activated=true");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}