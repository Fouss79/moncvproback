package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.entity.Payment;
import Fouss.moncvproback.entity.Subscription;
import Fouss.moncvproback.entity.User;
import Fouss.moncvproback.enums.PlanType;
import Fouss.moncvproback.exception.DownloadLimitExceededException;
import Fouss.moncvproback.repository.PaymentRepository;
import Fouss.moncvproback.repository.SubscriptionRepository;
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
    private final SubscriptionRepository subscriptionRepository; // ✅ NOUVEAU


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

    @Value("${geniuspay.webhook.secret}")
    private String webhookSecret;

    public PaymentResponse createPayment(
            PaymentRequest request,
            Authentication authentication
    ) {

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

                // Capture les erreurs GeniusPay (400, 401, 500...)
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(error -> {
                                    System.out.println("ERREUR GENIUSPAY = " + error);
                                    return new RuntimeException(
                                            "Erreur GeniusPay : " + error
                                    );
                                })
                )

                .bodyToMono(String.class)

                .map(json -> {

                    System.out.println("REPONSE BRUTE GENIUSPAY = " + json);

                    try {

                        ObjectMapper mapper = new ObjectMapper();

                        PaymentResponse response =
                                mapper.readValue(json, PaymentResponse.class);


                        if (response.getData() != null) {

                            User user = userRepository
                                    .findByEmail(authentication.getName())
                                    .orElseThrow(() ->
                                            new RuntimeException("Utilisateur introuvable")
                                    );


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

                            System.out.println(
                                    "PAIEMENT ENREGISTRE : "
                                            + payment.getReference()
                            );
                        }


                        return response;


                    } catch (Exception e) {

                        System.out.println(
                                "ERREUR PARSING REPONSE GENIUSPAY = "
                                        + e.getMessage()
                        );

                        throw new RuntimeException(e);
                    }

                })
                .block();
    }

    /**
     * ✅ NOUVEAU — Crée un paiement d'abonnement (Pro/Premium) au lieu d'un
     * paiement à l'unité. Réutilise exactement le même appel GeniusPay que
     * createPayment(), mais :
     *  - le montant vient du PlanType (jamais du frontend)
     *  - on ne force pas de paymentMethod => page de checkout GeniusPay
     *    (le client choisit Wave/Orange/MTN/Carte lui-même)
     *  - on enregistre une Subscription en PENDING en plus du Payment
     */
    public PaymentResponse createSubscriptionPayment(
            String planTypeStr,
            Authentication authentication
    ) {

        PlanType planType;
        try {
            planType = PlanType.valueOf(planTypeStr.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Plan inconnu. Valeurs acceptées : PRO, PREMIUM");
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        PaymentRequest request = new PaymentRequest();
        request.setAmount((int) planType.getAmountXof());
        request.setDescription(planType.getDescription());
        // Pas de setPaymentMethod(...) : le client choisit sur la page GeniusPay

        PaymentRequest.Customer customer = new PaymentRequest.Customer();
        customer.setEmail(user.getEmail());
        customer.setName(user.getNom());
        customer.setPhone(user.getPhone());
        request.setCustomer(customer);

        request.setSuccess_url(successUrl);
        request.setError_url(errorUrl);

        System.out.println("SUBSCRIPTION PAYMENT REQUEST = " + request);

        return webClient.post()
                .uri(apiUrl)
                .header("X-API-Key", apiKey)
                .header("X-API-Secret", apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(error -> {
                                    System.out.println("ERREUR GENIUSPAY = " + error);
                                    return new RuntimeException("Erreur GeniusPay : " + error);
                                })
                )
                .bodyToMono(String.class)
                .map(json -> {

                    System.out.println("REPONSE BRUTE GENIUSPAY (abonnement) = " + json);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        PaymentResponse response = mapper.readValue(json, PaymentResponse.class);

                        if (response.getData() != null) {

                            Subscription subscription = new Subscription(
                                    user,
                                    planType,
                                    response.getData().getReference(),
                                    planType.getAmountXof()
                            );

                            subscriptionRepository.save(subscription);

                            System.out.println(
                                    "ABONNEMENT ENREGISTRE (PENDING) : "
                                            + subscription.getPaymentReference()
                            );
                        }

                        return response;

                    } catch (Exception e) {
                        System.out.println(
                                "ERREUR PARSING REPONSE GENIUSPAY (abonnement) = " + e.getMessage()
                        );
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

            if (data == null || data.get("reference") == null) {
                throw new RuntimeException("Référence paiement absente");
            }

            String reference = data.get("reference").asText();

            // ✅ On tente d'abord de trouver un Payment (paiement ponctuel existant),
            // PUIS un Subscription (nouveau flux abonnement). Une transaction ne
            // peut correspondre qu'à l'un des deux, selon comment elle a été créée.
            paymentRepository.findByReference(reference).ifPresent(payment -> {
                if ("payment.success".equals(event)) {
                    payment.setStatus("COMPLETED");
                    payment.setCompletedAt(LocalDateTime.now());
                } else if ("payment.failed".equals(event)) {
                    payment.setStatus("FAILED");
                } else if ("payment.refunded".equals(event)) {
                    payment.setStatus("REFUNDED");
                }
                paymentRepository.save(payment);
            });

            subscriptionRepository.findByPaymentReference(reference).ifPresent(subscription -> {
                if ("payment.success".equals(event)) {
                    subscription.setStatus("ACTIVE");
                    subscription.setStartDate(LocalDateTime.now());
                    subscription.setEndDate(
                            LocalDateTime.now().plusDays(subscription.getPlanType().getDurationDays())
                    );
                } else if ("payment.failed".equals(event)
                        || "payment.cancelled".equals(event)) {
                    subscription.setStatus("FAILED");
                } else if ("payment.refunded".equals(event)) {
                    // ✅ NOUVEAU — Un abonnement remboursé après coup ne doit
                    // plus rester ACTIVE : on coupe l'accès immédiatement.
                    // ("payment.expired" n'existe pas dans le catalogue réel
                    // d'événements GeniusPay — remplacé par payment.refunded,
                    // qui lui existe bien et couvre un cas réel à gérer.)
                    subscription.setStatus("REFUNDED");
                }
                subscriptionRepository.save(subscription);

                System.out.println(
                        "ABONNEMENT MIS A JOUR : " + subscription.getPaymentReference()
                                + " -> " + subscription.getStatus()
                );
            });

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Payment getPaymentStatus(String reference) {

        return paymentRepository.findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Paiement introuvable"));
    }

    public boolean hasCompletedPayment(String email) {
        return paymentRepository.existsByUser_EmailAndStatus(email, "COMPLETED");
    }

    /**
     * ✅ NOUVEAU — Vérification réutilisable côté serveur : à appeler en tout
     * début de n'importe quel endpoint réservé aux abonnés (assistant IA,
     * import de CV...). Lève une exception si l'utilisateur est en FREE,
     * qu'il soit passé par le frontend ou qu'il appelle l'API directement
     * (Postman, curl...) avec son propre token JWT.
     *
     * Contrairement à consumeDownload(), on ne touche à aucun compteur ici :
     * on vérifie juste "a-t-il un abonnement actif", sans notion de quota.
     */
    public void requireActiveSubscription(String email) {
        if (getActiveSubscription(email).isEmpty()) {
            throw new DownloadLimitExceededException(
                    "Cette fonctionnalité est réservée aux abonnés Pro ou Premium.");
        }
    }

    /**
     * ✅ NOUVEAU — Retourne le plan actif de l'utilisateur ("FREE" si aucun
     * abonnement ACTIVE en cours), à consommer par GET /api/subscriptions/me.
     */
    public String getCurrentPlan(String email) {
        return subscriptionRepository
                .findTopByUser_EmailAndStatusOrderByCreatedAtDesc(email, "ACTIVE")
                .filter(sub -> sub.getEndDate() == null || sub.getEndDate().isAfter(LocalDateTime.now()))
                .map(sub -> sub.getPlanType().name())
                .orElse("FREE");
    }

    /**
     * ✅ NOUVEAU — Abonnement actif en cours (non expiré), ou vide si FREE.
     * Point d'entrée unique utilisé pour le plan ET le quota de téléchargement,
     * pour être certain que les deux s'accordent toujours sur le même abonnement.
     */
    private java.util.Optional<Subscription> getActiveSubscription(String email) {
        return subscriptionRepository
                .findTopByUser_EmailAndStatusOrderByCreatedAtDesc(email, "ACTIVE")
                .filter(sub -> sub.getEndDate() == null || sub.getEndDate().isAfter(LocalDateTime.now()));
    }

    /**
     * ✅ NOUVEAU — Statut complet consommé par GET /api/subscriptions/me :
     * plan actif + quota de téléchargements restants (null si illimité ou FREE).
     */
    public Fouss.moncvproback.dto.SubscriptionStatusResponse getSubscriptionStatus(String email) {

        java.util.Optional<Subscription> active = getActiveSubscription(email);

        if (active.isEmpty()) {
            // FREE : aucun téléchargement possible, pas de notion de quota à afficher
            return new Fouss.moncvproback.dto.SubscriptionStatusResponse(
                    "FREE", false, null, null, null);
        }

        Subscription sub = active.get();
        PlanType plan = sub.getPlanType();

        if (plan.isUnlimitedDownloads()) {
            return new Fouss.moncvproback.dto.SubscriptionStatusResponse(
                    plan.name(), true, null, null, null);
        }

        int limit = plan.getDownloadLimit();
        int used = sub.getDownloadsUsed();
        int remaining = Math.max(0, limit - used);

        return new Fouss.moncvproback.dto.SubscriptionStatusResponse(
                plan.name(), false, used, limit, remaining);
    }

    /**
     * ✅ NOUVEAU — À appeler juste avant de générer le PDF. Incrémente le
     * compteur si le quota le permet, ou lève DownloadLimitExceededException
     * sinon. Le contrôle du montant/quota reste entièrement côté serveur :
     * le frontend ne fait que refléter ce que cette méthode autorise.
     *
     * ✅ Verrouillé contre la concurrence : l'incrémentation passe par un
     * UPDATE atomique conditionnel en base (voir
     * SubscriptionRepository.incrementDownloadsIfUnderLimit), donc deux
     * clics simultanés ne peuvent jamais faire dépasser le quota d'une unité.
     */
    @org.springframework.transaction.annotation.Transactional
    public void consumeDownload(String email) {

        Subscription sub = getActiveSubscription(email)
                .orElseThrow(() -> new DownloadLimitExceededException(
                        "Aucun abonnement actif. Passez à Pro ou Premium pour télécharger."));

        PlanType plan = sub.getPlanType();

        if (plan.isUnlimitedDownloads()) {
            return; // Premium : pas de compteur à vérifier
        }

        int limit = plan.getDownloadLimit();

        int updatedRows = subscriptionRepository.incrementDownloadsIfUnderLimit(sub.getId(), limit);

        if (updatedRows == 0) {
            throw new DownloadLimitExceededException(
                    "Limite de " + limit + " téléchargements atteinte pour ce mois. "
                            + "Passez à Premium pour un accès illimité.");
        }
    }

    /**
     * ✅ NOUVEAU — À appeler en tout début de n'importe quel endpoint réservé
     * aux abonnés Pro/Premium (import CV, assistant IA, etc.), en plus du
     * verrou déjà fait côté frontend. Ne fait rien si l'utilisateur a un
     * abonnement actif ; lève une exception sinon, à catcher dans le
     * contrôleur pour renvoyer un 403 avec un message clair.
     *
     * Volontairement permissif sur le plan (PRO comme PREMIUM suffisent) :
     * contrairement au téléchargement, l'import CV et l'assistant IA ne sont
     * pas limités en nombre, juste réservés aux abonnés.
     */

}