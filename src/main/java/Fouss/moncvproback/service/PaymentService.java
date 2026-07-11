package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.entity.Payment;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.repository.PaymentRepository;
import Fouss.moncvproback.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WebClient webClient;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;


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
    @Value("${geniuspay.api.verify-url}")
    private String verifyUrl;


    public PaymentResponse createPayment(
            PaymentRequest request,
            Authentication authentication
    ){

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
                        PaymentResponse response =
                                mapper.readValue(json, PaymentResponse.class);


                        if(response.getData() != null){

                            User user = userRepository
                                    .findByEmail(authentication.getName())
                                    .orElseThrow();


                            Payment payment = new Payment();

                            payment.setReference(
                                    response.getData().getReference()
                            );

                            payment.setAmount(
                                    response.getData().getAmount()
                            );

                            payment.setCurrency(
                                    response.getData().getCurrency()
                            );

                            payment.setGateway(
                                    response.getData().getGateway()
                            );

                            payment.setPaymentMethod(
                                    request.getPaymentMethod()
                            );

                            payment.setDescription(
                                    request.getDescription()
                            );

                            payment.setStatus("PENDING");

                            payment.setCustomerEmail(
                                    request.getCustomer().getEmail()
                            );

                            payment.setUser(user);


                            paymentRepository.save(payment);
                        }


                        return response;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .block();
    }
    public void handleWebhook(String event, String payload) {

        try {

            ObjectMapper mapper = new ObjectMapper();

            JsonNode json = mapper.readTree(payload);

            JsonNode data = json.get("data");

            String reference = data.get("reference").asText();
            String status = data.get("status").asText();


            Payment payment = paymentRepository
                    .findByReference(reference)
                    .orElseThrow(() ->
                            new RuntimeException("Paiement introuvable")
                    );


            if ("payment.success".equals(event)
                    && "completed".equalsIgnoreCase(status)) {

                payment.setStatus("SUCCESS");

            } else if ("payment.failed".equals(event)) {

                payment.setStatus("FAILED");
            }


            paymentRepository.save(payment);


        } catch(Exception e) {

            throw new RuntimeException(e);
        }
    }
    public PaymentResponse verifyPayment(String reference) {

        return webClient.get()
                .uri(verifyUrl + "/" + reference)
                .header("X-API-Key", apiKey)
                .header("X-API-Secret", apiSecret)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {

                    System.out.println("VERIFICATION GENIUSPAY = " + json);

                    try {
                        ObjectMapper mapper = new ObjectMapper();

                        PaymentResponse response =
                                mapper.readValue(json, PaymentResponse.class);

                        if (!Boolean.TRUE.equals(response.getSuccess())) {
                            throw new RuntimeException("La vérification du paiement a échoué.");
                        }

                        if (response.getData() == null) {
                            throw new RuntimeException("Aucune information de paiement reçue.");
                        }
                        if (!"completed".equalsIgnoreCase(response.getData().getStatus())) {
                            throw new RuntimeException(
                                    "Le paiement est en état : " + response.getData().getStatus()
                            );
                        }

                        Payment payment = paymentRepository
                                .findByReference(reference)
                                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));


                        payment.setStatus("COMPLETED");
                        payment.setCompletedAt(LocalDateTime.now());

                        paymentRepository.save(payment);

                        return response;

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .block();
    }}