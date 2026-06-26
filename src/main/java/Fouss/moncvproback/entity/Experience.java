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
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💼 Infos principales
    private String poste;
    private String entreprise;
    private String dates;
    private String duree;

    // 🔗 relation CV

    @ManyToOne
    @JoinColumn(name = "cv_id")
    @JsonIgnore
    private Cv cv;

    // 📌 responsabilités (liste simple)
    @ElementCollection
    @CollectionTable(
            name = "experience_responsabilites",
            joinColumns = @JoinColumn(name = "experience_id")
    )
    @Column(name = "responsabilite")
    private List<String> responsabilites;
}