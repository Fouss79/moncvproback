package Fouss.moncvproback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telephone;
    private String email;
    private String adresse;
    private String linkedin;
    private String github;
    private String site;
    @OneToOne
    @JoinColumn(name = "cv_id")
    private Cv cv;


}