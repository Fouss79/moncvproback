package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CvRequestDTO;
import Fouss.moncvproback.service.MistralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint dédié aux suggestions de compétences générées par IA,
 * basées sur l'ensemble du profil du candidat (titre, profil,
 * expériences, formations, compétences déjà renseignées).
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class CompetenceSuggestionController {

    private final MistralService mistralService;

    @PostMapping("/suggest-competences")
    public ResponseEntity<List<String>> suggestCompetences(@RequestBody CvRequestDTO cv) {
        return ResponseEntity.ok(mistralService.suggestCompetences(cv));
    }
}