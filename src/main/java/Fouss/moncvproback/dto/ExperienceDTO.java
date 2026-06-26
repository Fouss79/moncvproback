package Fouss.moncvproback.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExperienceDTO {

    private String poste;
    private String entreprise;
    private String dates;
    private String duree;

    private List<String> responsabilites;
}