package Fouss.moncvproback.repository;
import Fouss.moncvproback.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);
        boolean existsByUser_EmailAndStatus(String email, String status);
    }
