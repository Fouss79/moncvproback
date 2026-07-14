package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.CvRequestDTO;
import Fouss.moncvproback.service.MistralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final MistralService mistralService;


    @PostMapping("/generate-profile")
    public ResponseEntity<String> generateProfile(
            @RequestBody CvRequestDTO cv
    ) {

        String result = mistralService.generateCv(cv);
        System.out.println("CV reçu = " + cv);

        return ResponseEntity.ok(result);
    }

}