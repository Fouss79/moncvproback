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
    private void saveContact(Cv cv, ContactDTO contact) {

        if (contact == null) return;

        cv.setEmail(contact.getEmail());
        cv.setTelephone(contact.getTelephone());
        cv.setAdresse(contact.getAdresse());
        cv.setLinkedin(contact.getLinkedin());
        cv.setGithub(contact.getGithub());

        System.out.println(contact.getEmail());
        System.out.println(contact.getTelephone());

        cvRepository.save(cv);
    }

}
