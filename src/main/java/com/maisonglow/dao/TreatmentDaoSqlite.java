package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Treatment;

import java.util.Optional;


/** SQLite persistence for the treatment table. */
public class TreatmentDaoSqlite extends SqliteCrudDao<Treatment> implements TreatmentDao {

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int DESCRIPTION = 2;
    private static final int PRICE = 3;
    private static final int DURATION_MINUTES = 4;
    private static final int ACTIVE = 5;
    private static final int FIELD_COUNT = 6;
    private static final String ACTIVE_FLAG = "1";
    private static final String INACTIVE_FLAG = "0";


    public TreatmentDaoSqlite(SqliteDatabase database) {
        super(database, "treatment", "id", "name", "description", "price", "duration_minutes", "active");
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
        fields[PRICE] = treatment.getPrice().toPlainString();
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
            new java.math.BigDecimal(fields[PRICE]), Integer.parseInt(fields[DURATION_MINUTES]));
        if (INACTIVE_FLAG.equals(fields[ACTIVE])) {
            treatment.setActive(false);
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
