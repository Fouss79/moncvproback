package Fouss.moncvproback.dto;

import lombok.Data;

@Data
public class CoverLetterRequestDTO {

    private CvRequestDTO cv;

    private String poste;

    private String entreprise;

    private String offre;

    private String ton;

    private String informationsSupplementaires;
}