package Fouss.moncvproback.dto;

import lombok.Data;

import java.util.List;

@Data
public class CvRequestDTO {

    private String nom;
    private String prenom;
    private String titre;
    private String profil;

    private String photo;

    private ContactDTO contact;

    private List<SkillDTO> competences;

    private List<String> logiciels;

    private List<String> softSkills;

    private List<SkillDTO> langues;

    private List<String> interets;

    private List<String> loisirs;

    private List<FormationDTO> formations;

    private List<String> certifications;

    private List<ExperienceDTO> experiences;

    private List<ProjetDTO> projets;



    @Data
    public static class ContactDTO {

        private String telephone;
        private String email;
        private String adresse;
        private String linkedin;
        private String github;
        private String site;
    }



    @Data
    public static class SkillDTO {

        private String nom;
        private String niveau;
    }



    @Data
    public static class FormationDTO {

        private String diplome;
        private String ecole;
        private String annee;
    }



    @Data
    public static class ExperienceDTO {

        private String poste;
        private String entreprise;
        private String dates;

        private List<String> responsabilites;
    }



    @Data
    public static class ProjetDTO {

        private String nom;
        private String description;

        private List<String> technologies;
    }
}