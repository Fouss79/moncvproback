package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WebClient webClient;


    @Value("${geniuspay.api.url}")
    private String apiUrl;

    @Value("${geniuspay.api.key}")
    private String apiKey;

    @Value("${geniuspay.api.secret}")
    private String apiSecret;

    @Value("${payment.success.url}")
    private String successUrl;

    @Value("${payment.error.url}")
    private String errorUrl;


    public PaymentResponse createPayment(PaymentRequest request) {

        // Ajout des URLs de retour avant l'envoi à GeniusPay
        request.setSuccess_url(successUrl);
        request.setError_url(errorUrl);

        System.out.println("PAYMENT REQUEST = " + request);

        return webClient.post()
                .uri(apiUrl)
                .header("X-API-Key", apiKey)
                .header("X-API-Secret", apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    System.out.println("REPONSE BRUTE GENIUSPAY = " + json);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(json, PaymentResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .block();
    }
}