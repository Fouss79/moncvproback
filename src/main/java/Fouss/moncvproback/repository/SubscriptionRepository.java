package Fouss.moncvproback.repository;


import Fouss.moncvproback.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPaymentReference(String paymentReference);

    Optional<Subscription> findTopByUser_EmailAndStatusOrderByCreatedAtDesc(
            String email, String status);
}