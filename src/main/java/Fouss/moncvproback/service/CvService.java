package Fouss.moncvproback.service;

import Fouss.moncvproback.dto.ContactDTO;
import Fouss.moncvproback.dto.CvFullDTO;
import Fouss.moncvproback.dto.ExperienceDTO;
import Fouss.moncvproback.dto.FormationDTO;
import Fouss.moncvproback.entity.*;
import Fouss.moncvproback.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CvService {

    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final CompetenceRepository competenceRepository;
    private final LangueRepository langueRepository;
    private final ExperienceRepository experienceRepository;
    private final LoisirRepository loisirRepository;
    private final FormationRepository formationRepository;
    private final SoftSkillRepository softSkillRepository;

    // ➕ Créer CV
    public Cv createCv(Long userId, Cv cv) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        cv.setUser(user);

        return cvRepository.save(cv);
    }

    // 📄 Récupérer tous les CV d'un user
    public List<Cv> getCvsByUser(Long userId) {
        return cvRepository.findByUserId(userId);
    }

    // 🔍 Récupérer un CV par ID
    public Cv getCvById(Long id) {
        return cvRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV introuvable"));
    }

    // ✏️ Update CV
    public Cv updateCv(Long id, Cv updatedCv) {

        Cv cv = getCvById(id);

        cv.setPrenom(updatedCv.getPrenom());
        cv.setNom(updatedCv.getNom());
        cv.setTitre(updatedCv.getTitre());
        cv.setProfil(updatedCv.getProfil());
        cv.setCouleur(updatedCv.getCouleur());
        cv.setTemplate(updatedCv.getTemplate());

        cv.setEmail(updatedCv.getEmail());
        cv.setTelephone(updatedCv.getTelephone());
        cv.setAdresse(updatedCv.getAdresse());
        cv.setLinkedin(updatedCv.getLinkedin());
        cv.setGithub(updatedCv.getGithub());


        return cvRepository.save(cv);
    }

    // ❌ Delete CV
    public void deleteCv(Long id) {
        cvRepository.deleteById(id);
    }

    @Transactional
    public Cv saveFullCv(Long userId, CvFullDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Cv cv = cvRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElse(new Cv());

        cv.setUser(user);

        cv.setNom(dto.getNom());
        cv.setPrenom(dto.getPrenom());
        cv.setTitre(dto.getTitre());
        cv.setProfil(dto.getProfil());
        cv.setCouleur(dto.getCouleur());
        cv.setTemplate(dto.getTemplate());
        cv.setPhotoUrl(dto.getPhotoUrl());



        Cv savedCv = cvRepository.save(cv);

        // 🔥 IMPORTANT: clean old data before re-insert
        competenceRepository.deleteByCvId(savedCv.getId());
        langueRepository.deleteByCvId(savedCv.getId());
        softSkillRepository.deleteByCvId(savedCv.getId());
        loisirRepository.deleteByCvId(savedCv.getId());
        formationRepository.deleteByCvId(savedCv.getId());
        experienceRepository.deleteByCvId(savedCv.getId());

        // re-save
        saveCompetences(savedCv, dto.getCompetences());
        saveSoftSkills(savedCv, dto.getSoftSkills());
        saveLangues(savedCv, dto.getLangues());
        saveLoisirs(savedCv, dto.getLoisirs());
        saveFormations(savedCv, dto.getFormations());
        saveExperiences(savedCv, dto.getExperiences());
        saveContact(cv, dto.getContact());

        return savedCv;
    }
    @Transactional
    public Cv updateFullCv(Long userId, CvFullDTO dto) {

        Cv cv = cvRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElse(null);

        if (cv == null) {
            return saveFullCv(userId, dto);
        }

        // update champs simples
        cv.setNom(dto.getNom());
        cv.setPrenom(dto.getPrenom());
        cv.setTitre(dto.getTitre());
        cv.setProfil(dto.getProfil());
        cv.setCouleur(dto.getCouleur());
        cv.setTemplate(dto.getTemplate());
        cv.setPhotoUrl(dto.getPhotoUrl());


        // ⚠️ IMPORTANT : supprimer anciennes relations
        cv.getCompetences().clear();
        cv.getLangues().clear();
        cv.getExperiences().clear();
        cv.getFormations().clear();
        cv.getSoftSkills().clear();
        cv.getLoisirs().clear();

        cvRepository.save(cv);

        // recréer
        saveCompetences(cv, dto.getCompetences());
        saveLangues(cv, dto.getLangues());
        saveExperiences(cv, dto.getExperiences());
        saveFormations(cv, dto.getFormations());
        saveSoftSkills(cv, dto.getSoftSkills());
        saveLoisirs(cv, dto.getLoisirs());
        System.out.println("CONTACT RECU = " + dto.getContact());
        saveContact(cv, dto.getContact());

        return cv;
    }
    @Transactional
    public CvFullDTO getCvFullByUser(Long userId) {

        Cv cv = cvRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("CV introuvable"));

        CvFullDTO dto = new CvFullDTO();

        dto.setNom(cv.getNom());
        dto.setPrenom(cv.getPrenom());
        dto.setTitre(cv.getTitre());
        dto.setProfil(cv.getProfil());
        dto.setCouleur(cv.getCouleur());
        dto.setTemplate(cv.getTemplate());
        dto.setPhotoUrl(cv.getPhotoUrl());

        // CONTACT
        ContactDTO contact = new ContactDTO();
        contact.setEmail(cv.getEmail());
        contact.setTelephone(cv.getTelephone());
        contact.setAdresse(cv.getAdresse());
        contact.setLinkedin(cv.getLinkedin());
        contact.setGithub(cv.getGithub());
        dto.setContact(contact);

        // COMPETENCES
        dto.setCompetences(
                cv.getCompetences()
                        .stream()
                        .map(Competence::getNom)
                        .toList()
        );

        // LANGUES
        dto.setLangues(
                cv.getLangues()
                        .stream()
                        .map(Langue::getNom)
                        .toList()
        );

        // SOFT SKILLS
        dto.setSoftSkills(
                cv.getSoftSkills()
                        .stream()
                        .map(SoftSkill::getNom)
                        .toList()
        );

        // LOISIRS
        dto.setLoisirs(
                cv.getLoisirs()
                        .stream()
                        .map(Loisir::getNom)
                        .toList()
        );

        // FORMATIONS
        dto.setFormations(
                cv.getFormations()
                        .stream()
                        .map(f -> {
                            FormationDTO fDto = new FormationDTO();
                            fDto.setDiplome(f.getDiplome());
                            fDto.setEcole(f.getEcole());
                            fDto.setAnnee(f.getAnnee());
                            return fDto;
                        })
                        .toList()
        );

        // EXPERIENCES
        dto.setExperiences(
                cv.getExperiences()
                        .stream()
                        .map(e -> {
                            ExperienceDTO eDto = new ExperienceDTO();
                            eDto.setPoste(e.getPoste());
                            eDto.setEntreprise(e.getEntreprise());
                            eDto.setDates(e.getDates());
                            eDto.setDuree(e.getDuree());
                            eDto.setResponsabilites(e.getResponsabilites());
                            return eDto;
                        })
                        .toList()
        );

        return dto;
    }

    private void saveCompetences(
            Cv cv,
            List<String> competences
    ) {

        if(competences == null) return;

        for(String nom : competences){

            Competence competence = new Competence();

            competence.setNom(nom);
            competence.setCv(cv);

            competenceRepository.save(competence);
        }
    }
    private void saveLangues(
            Cv cv,
            List<String> langues
    ) {

        if(langues == null) return;

        for(String nom : langues){

            Langue langue = new Langue();

            langue.setNom(nom);
            langue.setNiveau("Non précisé");

            langue.setCv(cv);

            langueRepository.save(langue);
        }
    }

    private void saveFormations(
            Cv cv,
            List<FormationDTO> formations
    ) {

        if(formations == null) return;

        for(FormationDTO dto : formations){

            Formation formation = new Formation();

            formation.setDiplome(dto.getDiplome());
            formation.setEcole(dto.getEcole());
            formation.setAnnee(dto.getAnnee());

            formation.setCv(cv);

            formationRepository.save(formation);
        }
    }
    private void saveExperiences(
            Cv cv,
            List<ExperienceDTO> experiences
    ) {

        if(experiences == null) return;

        for(ExperienceDTO dto : experiences){

            Experience exp = new Experience();

            exp.setPoste(dto.getPoste());
            exp.setEntreprise(dto.getEntreprise());
            exp.setDates(dto.getDates());
            exp.setDuree(dto.getDuree());

            exp.setResponsabilites(dto.getResponsabilites());

            exp.setCv(cv);

            experienceRepository.save(exp);
        }
    }
    private void saveSoftSkills(
            Cv cv,
            List<String> softSkills
    ) {
        if (softSkills == null || softSkills.isEmpty()) {
            return;
        }

        for (String nom : softSkills) {

            SoftSkill softSkill = new SoftSkill();

            softSkill.setNom(nom);
            softSkill.setCv(cv);

            softSkillRepository.save(softSkill);
        }
    }
    private void saveLoisirs(
            Cv cv,
            List<String> loisirs
    ) {
        if (loisirs == null || loisirs.isEmpty()) {
            return;
        }

        for (String nom : loisirs) {

            Loisir loisir = new Loisir();

            loisir.setNom(nom);
            loisir.setCv(cv);

            loisirRepository.save(loisir);
        }


    }
    private void saveContact(Cv cv, ContactDTO dto) {

        if (dto == null) return;

        Contact contact = cv.getContact();

        if (contact == null) {
            contact = new Contact();
            contact.setCv(cv);
        }

        contact.setEmail(dto.getEmail());
        contact.setTelephone(dto.getTelephone());
        contact.setAdresse(dto.getAdresse());
        contact.setLinkedin(dto.getLinkedin());
        contact.setGithub(dto.getGithub());

        cv.setContact(contact);
    }
}
