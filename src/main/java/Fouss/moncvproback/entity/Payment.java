package Fouss.moncvproback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence GeniusPay
    @Column(unique = true)
    private String reference;

    // Montant payé
    private Integer amount;

    // XOF
    private String currency;

    // orange_money, wave...
    private String paymentMethod;

    // GeniusPay
    private String gateway;

    // PENDING, COMPLETED, FAILED
    private String status;

    private String description;

    // Email client (utile si User n'est pas retrouvé)
    private String customerEmail;

    private String customerPhone;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}