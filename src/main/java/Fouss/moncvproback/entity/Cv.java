package Fouss.moncvproback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 👤 infos principales du CV
    private String prenom;
    private String nom;
    private String titre;

    @Column(length = 2000)
    private String profil;

    // 🎨 personnalisation CV
    private String couleur;
    private String template;

    // 📞 contact (simple version)
    private String email;
    private String telephone;
    private String adresse;
    private String linkedin;
    private String github;

    @Column(length = 500)
    private String photoUrl;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;


    // 📚 sections CV
    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experiences;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Formation> formations;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Competence> competences;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Langue> langues;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Loisir> loisirs;
    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoftSkill> softSkills;

    @OneToOne(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private Contact contact;

}