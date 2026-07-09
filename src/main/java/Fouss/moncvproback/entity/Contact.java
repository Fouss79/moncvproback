package Fouss.moncvproback.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @JsonIgnore   // ← ajoute ça
    private Cv cv;
}