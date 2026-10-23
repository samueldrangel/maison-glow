package com.maisonglow.service;

import com.maisonglow.dao.ProfessionalDao;
import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.util.Validator;

import java.util.List;

/**
 * Manages the professionals and the treatments they offer (RF-08, RF-09):
 * registration with a unique document and a valid working day, treatment
 * assignment and the list of professionals for a treatment.
 */
public class ProfessionalService {

    private final ProfessionalDao professionalDao;
    private final TreatmentDao treatmentDao;

    /**
     * Creates the service.
     *
     * @param professionalDao the professionals storage
     * @param treatmentDao    the treatments storage
     */
    public ProfessionalService(ProfessionalDao professionalDao, TreatmentDao treatmentDao) {
        this.professionalDao = professionalDao;
        this.treatmentDao = treatmentDao;
    }

    /**
     * Registers a new professional, active and without treatments.
     *
     * @param contact   the document, name, phone and email; the document must be unique
     * @param specialty the main area of work
     * @param schedule  the working day, with the start before the end
     * @return the stored professional, with its id
     * @throws ValidationException   if a field is missing or malformed, or the hours are reversed
     * @throws BusinessRuleException if another professional has the same document
     * @throws DataAccessException   if the professional cannot be stored
     */
    public Professional register(ContactData contact, String specialty, WorkSchedule schedule)
        throws ValidationException, BusinessRuleException, DataAccessException {
        contact.validate();
        Validator.requireText(specialty, "especialidad");
        schedule.validate();
        String document = contact.document().trim();
        if (professionalDao.findByDocument(document).isPresent()) {
            throw new BusinessRuleException("Ya existe un profesional con el documento " + document + ".");
        }
        return professionalDao.create(new Professional(0, document, contact.name().trim(), contact.phone().trim(),
            contact.email().trim(), specialty.trim(), schedule.start(), schedule.end()));
    }

    /**
     * Changes the contact data and specialty of a professional. The document
     * identifies the professional and cannot change.
     *
     * @param id        the professional id
     * @param contact   the new contact data, with the same document
     * @param specialty the new specialty
     * @return the updated professional
     * @throws ValidationException   if a field is missing or malformed
     * @throws BusinessRuleException if the professional does not exist or the document changed
     * @throws DataAccessException   if the professional cannot be stored
     */
    public Professional update(int id, ContactData contact, String specialty)
        throws ValidationException, BusinessRuleException, DataAccessException {
        contact.validate();
        Validator.requireText(specialty, "especialidad");
        Professional professional = findById(id);
        if (!professional.getDocument().equals(contact.document().trim())) {
            throw new BusinessRuleException("El documento de un profesional no se puede cambiar.");
        }
        professional.setName(contact.name().trim());
        professional.setPhone(contact.phone().trim());
        professional.setEmail(contact.email().trim());
        professional.setSpecialty(specialty.trim());
        professionalDao.update(professional);
        return professional;
    }

    /**
     * Changes the working day of a professional.
     *
     * @param id       the professional id
     * @param schedule the new working day, with the start before the end
     * @throws ValidationException   if an hour is missing or the hours are reversed
     * @throws BusinessRuleException if the professional does not exist
     * @throws DataAccessException   if the professional cannot be stored
     */
    public void changeWorkingHours(int id, WorkSchedule schedule)
        throws ValidationException, BusinessRuleException, DataAccessException {
        schedule.validate();
        Professional professional = findById(id);
        professional.setWorkingHours(schedule.start(), schedule.end());
        professionalDao.update(professional);
    }

    /**
     * Adds a treatment to the ones a professional offers.
     *
     * @param professionalId the professional id
     * @param treatmentId    the treatment id
     * @throws BusinessRuleException if either does not exist, the treatment is inactive
     *                               or the professional already offers it
     * @throws DataAccessException   if the change cannot be stored
     */
    public void assignTreatment(int professionalId, int treatmentId)
        throws BusinessRuleException, DataAccessException {
        Professional professional = findById(professionalId);
        Treatment treatment = findTreatment(treatmentId);
        if (!treatment.isActive()) {
            throw new BusinessRuleException("El tratamiento " + treatment.getName() + " no está activo.");
        }
        if (professional.offers(treatment)) {
            throw new BusinessRuleException(professional.getName() + " ya ofrece " + treatment.getName() + ".");
        }
        professional.addTreatment(treatment);
        professionalDao.update(professional);
    }

    /**
     * Removes a treatment from the ones a professional offers.
     *
     * @param professionalId the professional id
     * @param treatmentId    the treatment id
     * @throws BusinessRuleException if either does not exist or the professional does not offer it
     * @throws DataAccessException   if the change cannot be stored
     */
    public void removeTreatment(int professionalId, int treatmentId)
        throws BusinessRuleException, DataAccessException {
        Professional professional = findById(professionalId);
        Treatment treatment = findTreatment(treatmentId);
        if (!professional.offers(treatment)) {
            throw new BusinessRuleException(professional.getName() + " no ofrece " + treatment.getName() + ".");
        }
        professional.removeTreatment(treatment);
        professionalDao.update(professional);
    }

    /**
     * Returns the active professionals who offer a treatment (RF-09).
     *
     * @param treatmentId the treatment id
     * @return the professionals who can attend it
     * @throws DataAccessException if the professionals cannot be read
     */
    public List<Professional> listByTreatment(int treatmentId) throws DataAccessException {
        return professionalDao.findByTreatment(treatmentId).stream().filter(Professional::isActive).toList();
    }

    /**
     * Returns the professional with the given id.
     *
     * @param id the professional id
     * @return the professional
     * @throws BusinessRuleException if no professional has that id
     * @throws DataAccessException   if the lookup fails
     */
    public Professional findById(int id) throws BusinessRuleException, DataAccessException {
        return professionalDao.findById(id)
            .orElseThrow(() -> new BusinessRuleException("No existe el profesional con id " + id + "."));
    }

    /**
     * Returns every professional.
     *
     * @return the professionals
     * @throws DataAccessException if the professionals cannot be read
     */
    public List<Professional> listAll() throws DataAccessException {
        return professionalDao.findAll();
    }

    /**
     * Stops a professional from receiving new appointments.
     *
     * @param id the professional id
     * @throws BusinessRuleException if the professional does not exist
     * @throws DataAccessException   if the change cannot be stored
     */
    public void deactivate(int id) throws BusinessRuleException, DataAccessException {
        Professional professional = findById(id);
        professional.deactivate();
        professionalDao.update(professional);
    }

    /**
     * Allows a professional to receive appointments again.
     *
     * @param id the professional id
     * @throws BusinessRuleException if the professional does not exist
     * @throws DataAccessException   if the change cannot be stored
     */
    public void activate(int id) throws BusinessRuleException, DataAccessException {
        Professional professional = findById(id);
        professional.activate();
        professionalDao.update(professional);
    }

    private Treatment findTreatment(int treatmentId) throws BusinessRuleException, DataAccessException {
        return treatmentDao.findById(treatmentId)
            .orElseThrow(() -> new BusinessRuleException("No existe el tratamiento con id " + treatmentId + "."));
    }
}
