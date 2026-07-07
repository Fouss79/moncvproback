package Fouss.moncvproback.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;


    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<Cv> cvs;
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "verification_token")
    private String verificationToken;}