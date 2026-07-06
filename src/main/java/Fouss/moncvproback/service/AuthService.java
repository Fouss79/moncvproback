package Fouss.moncvproback.service;


import Fouss.moncvproback.dto.LoginRequest;
import Fouss.moncvproback.dto.RegisterRequest;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.repository.UserRepository;
import Fouss.moncvproback.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    // REGISTER
    public Map<String, Object> register(RegisterRequest request) {

        User user = new User();
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false); // ❗ important

        String token = java.util.UUID.randomUUID().toString();
        user.setVerificationToken(token);

        userRepository.save(user);

        // 🔥 envoi email activation
        String link = "https://ton-backend.com/api/auth/activate?token=" + token;

        emailService.sendEmail(
                user.getEmail(),
                "Activation de ton compte MonCVPro",
                "<h2>Bienvenue sur MonCVPro</h2>"
                        + "<p>Clique sur le lien pour activer ton compte :</p>"
                        + "<a href='" + link + "'>Activer mon compte</a>"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte créé. Vérifie ton email pour activer ton compte.");

        return response;
    }
    // LOGIN
    public Map<String, Object> login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte non activé. Vérifie ton email.");
        }

        String token = jwtService.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("token", token);

        return response;
    }
}