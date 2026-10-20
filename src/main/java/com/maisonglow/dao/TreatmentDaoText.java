package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Treatment;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Stores treatments in a text file with the columns of the {@code treatment}
 * table: {@code id|name|description|price|duration_minutes|active}.
 */
public class TreatmentDaoText extends TextFileDao<Treatment> implements TreatmentDao {

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int DESCRIPTION = 2;
    private static final int PRICE = 3;
    private static final int DURATION_MINUTES = 4;
    private static final int ACTIVE = 5;
    private static final int FIELD_COUNT = 6;
    private static final String ACTIVE_FLAG = "1";
    private static final String INACTIVE_FLAG = "0";

    /**
     * Creates the DAO over the given file.
     *
     * @param file the treatments file, usually {@code data/treatments.txt}
     */
    public TreatmentDaoText(Path file) {
        super(file);
    }

    @Override
    public Optional<Treatment> findByName(String name) throws DataAccessException {
        return findAll().stream()
            .filter(treatment -> treatment.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    protected String[] toFields(Treatment treatment) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(treatment.getId());
        fields[NAME] = treatment.getName();
        fields[DESCRIPTION] = treatment.getDescription();
        fields[PRICE] = String.valueOf(treatment.getPrice());
        fields[DURATION_MINUTES] = String.valueOf(treatment.getDurationMinutes());
        fields[ACTIVE] = treatment.isActive() ? ACTIVE_FLAG : INACTIVE_FLAG;
        return fields;
    }

    @Override
    protected Treatment fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        Treatment treatment = new Treatment(Integer.parseInt(fields[ID]), fields[NAME], fields[DESCRIPTION],
            Double.parseDouble(fields[PRICE]), Integer.parseInt(fields[DURATION_MINUTES]));
        if (INACTIVE_FLAG.equals(fields[ACTIVE])) {
            treatment.deactivate();
        }
        return treatment;
    }

    @Override
    protected int getId(Treatment treatment) {
        return treatment.getId();
    }

    @Override
    protected void setId(Treatment treatment, int id) {
        treatment.setId(id);
    }
}
