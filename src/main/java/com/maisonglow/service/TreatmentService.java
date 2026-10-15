package com.maisonglow.service;

import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Treatment;

import java.util.List;

/**
 * Manages the treatment catalog (RF-06, RF-07): registration with a unique
 * name, updates and availability.
 */
public class TreatmentService {

    private final TreatmentDao treatmentDao;

    /**
     * Creates the service.
     *
     * @param treatmentDao the treatments storage
     */
    public TreatmentService(TreatmentDao treatmentDao) {
        this.treatmentDao = treatmentDao;
    }

    /**
     * Registers a new treatment.
     *
     * @param data the name (unique in the catalog), description, price and duration
     * @return the stored treatment, with its id
     * @throws ValidationException   if a field is missing or out of range
     * @throws BusinessRuleException if the name is taken
     * @throws DataAccessException   if the treatment cannot be stored
     */
    public Treatment register(TreatmentData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        String name = data.name().trim();
        requireUniqueName(name, 0);
        return treatmentDao.create(new Treatment(0, name, data.cleanDescription(), data.price(),
            data.durationMinutes()));
    }

    /**
     * Changes the data of an existing treatment.
     *
     * @param id   the treatment id
     * @param data the new name (unique in the catalog), description, price and duration
     * @return the updated treatment
     * @throws ValidationException   if a field is missing or out of range
     * @throws BusinessRuleException if the treatment does not exist or the name is taken
     * @throws DataAccessException   if the treatment cannot be stored
     */
    public Treatment update(int id, TreatmentData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        Treatment treatment = findById(id);
        String name = data.name().trim();
        requireUniqueName(name, id);
        treatment.setName(name);
        treatment.setDescription(data.cleanDescription());
        treatment.setPrice(data.price());
        treatment.setDurationMinutes(data.durationMinutes());
        treatmentDao.update(treatment);
        return treatment;
    }

    /**
     * Offers a treatment again.
     *
     * @param id the treatment id
     * @throws BusinessRuleException if the treatment does not exist
     * @throws DataAccessException   if the treatment cannot be stored
     */
    public void activate(int id) throws BusinessRuleException, DataAccessException {
        Treatment treatment = findById(id);
        treatment.activate();
        treatmentDao.update(treatment);
    }

    /**
     * Stops offering a treatment without deleting its history.
     *
     * @param id the treatment id
     * @throws BusinessRuleException if the treatment does not exist
     * @throws DataAccessException   if the treatment cannot be stored
     */
    public void deactivate(int id) throws BusinessRuleException, DataAccessException {
        Treatment treatment = findById(id);
        treatment.deactivate();
        treatmentDao.update(treatment);
    }

    /**
     * Returns the treatment with the given id.
     *
     * @param id the treatment id
     * @return the treatment
     * @throws BusinessRuleException if no treatment has that id
     * @throws DataAccessException   if the lookup fails
     */
    public Treatment findById(int id) throws BusinessRuleException, DataAccessException {
        return treatmentDao.findById(id)
            .orElseThrow(() -> new BusinessRuleException("No existe el tratamiento con id " + id + "."));
    }

    /**
     * Returns every treatment in the catalog.
     *
     * @return the treatments
     * @throws DataAccessException if the treatments cannot be read
     */
    public List<Treatment> listAll() throws DataAccessException {
        return treatmentDao.findAll();
    }

    /**
     * Returns the treatments currently offered.
     *
     * @return the active treatments
     * @throws DataAccessException if the treatments cannot be read
     */
    public List<Treatment> listActive() throws DataAccessException {
        return treatmentDao.findAll().stream().filter(Treatment::isActive).toList();
    }

    private void requireUniqueName(String name, int ownId) throws BusinessRuleException, DataAccessException {
        boolean taken = treatmentDao.findByName(name)
            .filter(existing -> existing.getId() != ownId)
            .isPresent();
        if (taken) {
            throw new BusinessRuleException("Ya existe un tratamiento llamado " + name + ".");
        }
    }
}
