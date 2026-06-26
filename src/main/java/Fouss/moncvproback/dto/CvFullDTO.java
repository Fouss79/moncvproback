package Fouss.moncvproback.dto;

import lombok.Data;

import java.util.List;

@Data
public class CvFullDTO {

    private String nom;
    private String prenom;
    private String titre;
    private String profil;

    private String couleur;
    private String template;
    private String photoUrl;
    private ContactDTO contact;

    private List<String> competences;
    private List<String> softSkills;
    private List<String> langues;
    private List<String> loisirs;

    private List<FormationDTO> formations;
    private List<ExperienceDTO> experiences;
}