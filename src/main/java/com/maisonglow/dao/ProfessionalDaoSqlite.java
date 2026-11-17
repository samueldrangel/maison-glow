package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A professional and all treatment links are committed together. */
public final class ProfessionalDaoSqlite extends SqliteCrudDao<Professional> implements ProfessionalDao {
    private final TreatmentDao treatments;
    public ProfessionalDaoSqlite(SqliteDatabase database, TreatmentDao treatments) {
        super(database,"professional","id","document","name","phone","email","specialty","work_start","work_end","active");
        this.treatments = treatments;
    }
    @Override protected String[] toFields(Professional p) {
        return new String[]{""+p.getId(),p.getDocument(),p.getName(),p.getPhone(),p.getEmail(),p.getSpecialty(),
            p.getWorkStart().toString(),p.getWorkEnd().toString(),p.isActive()?"1":"0"};
    }
    @Override protected Professional fromFields(String[] f) throws DataAccessException {
        Professional p = new Professional(Integer.parseInt(f[0]),f[1],f[2],f[3],f[4],f[5],LocalTime.parse(f[6]),LocalTime.parse(f[7]));
        p.setActive("1".equals(f[8]));
        p.setTreatments(database.transaction(c -> {
            List<Treatment> offered = new ArrayList<>();
            try (var statement = c.prepareStatement("SELECT treatment_id FROM professional_treatment WHERE professional_id=? ORDER BY treatment_id")) {
                statement.setInt(1,p.getId());
                try (var rows = statement.executeQuery()) {
                    while(rows.next()) offered.add(findRelated(treatments,rows.getString(1),"tratamiento"));
                }
            }
            return offered;
        }));
        return p;
    }
    @Override protected void saveRelations(Connection c, Professional p) throws SQLException {
        try(var delete = c.prepareStatement("DELETE FROM professional_treatment WHERE professional_id=?")) {
            delete.setInt(1,p.getId()); delete.executeUpdate();
        }
        try(var insert = c.prepareStatement("INSERT INTO professional_treatment VALUES (?,?)")) {
            for(Treatment t : p.getTreatments()) { insert.setInt(1,p.getId()); insert.setInt(2,t.getId()); insert.addBatch(); }
            insert.executeBatch();
        }
    }
    @Override protected int getId(Professional p) { return p.getId(); }
    @Override protected void setId(Professional p,int id) { p.setId(id); }
    @Override public Optional<Professional> findByDocument(String document) throws DataAccessException {
        return query(" WHERE document=?",document).stream().findFirst();
    }
    @Override public List<Professional> findByTreatment(int id) throws DataAccessException {
        return query(" WHERE id IN (SELECT professional_id FROM professional_treatment WHERE treatment_id=?) ORDER BY id",id);
    }
}
