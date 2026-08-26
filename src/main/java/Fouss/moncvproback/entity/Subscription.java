package Fouss.moncvproback.entity;

import Fouss.moncvproback.enums.PlanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Représente l'état d'abonnement d'un utilisateur (Pro/Premium, actif ou non).
 * Distincte de "Payment" : Payment reste le journal de chaque transaction
 * GeniusPay ; Subscription est l'état métier qu'on dérive de ces transactions
 * (actif jusqu'à quelle date, etc.).
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    // PENDING | ACTIVE | FAILED | EXPIRED
    @Column(nullable = false)
    private String status = "PENDING";

    // Référence GeniusPay (ex: MTX-A1B2C3D4E5) — c'est elle que le webhook renvoie
    @Column(unique = true, nullable = false)
    private String paymentReference;

    private long amountXof;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Subscription(User user, PlanType planType, String paymentReference, long amountXof) {
        this.user = user;
        this.planType = planType;
        this.paymentReference = paymentReference;
        this.amountXof = amountXof;
        this.status = "PENDING";
    }
}