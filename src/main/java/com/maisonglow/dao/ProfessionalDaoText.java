package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores professionals in a text file with the columns of the
 * {@code professional} table:
 * {@code id|document|name|phone|email|specialty|work_start|work_end|active}.
 * The treatments each one offers go to a second file with the columns of
 * {@code professional_treatment}: {@code professional_id|treatment_id}.
 *
 * <p>The links are written again after every change of a professional. There
 * are no transactions between the two files: if the links of a new
 * professional cannot be saved, the professional is removed again.</p>
 */
public class ProfessionalDaoText extends TextFileDao<Professional> implements ProfessionalDao {

    private static final int ID = 0;
    private static final int DOCUMENT = 1;
    private static final int NAME = 2;
    private static final int PHONE = 3;
    private static final int EMAIL = 4;
    private static final int SPECIALTY = 5;
    private static final int WORK_START = 6;
    private static final int WORK_END = 7;
    private static final int ACTIVE = 8;
    private static final int FIELD_COUNT = 9;
    private static final int LINK_PROFESSIONAL_ID = 0;
    private static final int LINK_TREATMENT_ID = 1;
    private static final int LINK_FIELD_COUNT = 2;
    private static final int UNSAVED_ID = 0;
    private static final String ACTIVE_FLAG = "1";
    private static final String INACTIVE_FLAG = "0";

    private final TextFileStore linksStore;
    private final CrudDao<Treatment> treatments;
    private Map<Integer, List<String>> treatmentIdsByProfessional;

    /**
     * Creates the DAO over the given files.
     *
     * @param file       the professionals file, usually {@code data/professionals.txt}
     * @param linksFile  the links file, usually {@code data/professional_treatments.txt}
     * @param treatments where to find the treatments each professional offers
     */
    public ProfessionalDaoText(Path file, Path linksFile, CrudDao<Treatment> treatments) {
        super(file);
        this.linksStore = new TextFileStore(linksFile);
        this.treatments = treatments;
    }

    @Override
    public Professional create(Professional professional) throws DataAccessException {
        Professional created = super.create(professional);
        try {
            writeLinks();
        } catch (DataAccessException e) {
            undoCreate(created, e);
            throw e;
        }
        return created;
    }

    private void undoCreate(Professional professional, DataAccessException cause) {
        try {
            super.delete(professional.getId());
            professional.setId(UNSAVED_ID);
        } catch (DataAccessException undoError) {
            cause.addSuppressed(undoError);
        }
    }

    @Override
    public void update(Professional professional) throws DataAccessException {
        super.update(professional);
        writeLinks();
    }

    @Override
    public void delete(int id) throws DataAccessException {
        super.delete(id);
        writeLinks();
    }

    @Override
    public Optional<Professional> findByDocument(String document) throws DataAccessException {
        return findAll().stream()
            .filter(professional -> professional.getDocument().equals(document))
            .findFirst();
    }

    @Override
    public List<Professional> findByTreatment(int treatmentId) throws DataAccessException {
        return findAll().stream()
            .filter(professional -> professional.getTreatments().stream()
                .anyMatch(treatment -> treatment.getId() == treatmentId))
            .toList();
    }

    @Override
    protected String[] toFields(Professional professional) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(professional.getId());
        fields[DOCUMENT] = professional.getDocument();
        fields[NAME] = professional.getName();
        fields[PHONE] = professional.getPhone();
        fields[EMAIL] = professional.getEmail();
        fields[SPECIALTY] = professional.getSpecialty();
        fields[WORK_START] = professional.getWorkStart().toString();
        fields[WORK_END] = professional.getWorkEnd().toString();
        fields[ACTIVE] = professional.isActive() ? ACTIVE_FLAG : INACTIVE_FLAG;
        return fields;
    }

    @Override
    protected Professional fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        Professional professional = new Professional(Integer.parseInt(fields[ID]), fields[DOCUMENT], fields[NAME],
            fields[PHONE], fields[EMAIL], fields[SPECIALTY],
            LocalTime.parse(fields[WORK_START]), LocalTime.parse(fields[WORK_END]));
        if (INACTIVE_FLAG.equals(fields[ACTIVE])) {
            professional.deactivate();
        }
        for (String treatmentId : treatmentIdsOf(professional.getId())) {
            professional.addTreatment(findRelated(treatments, treatmentId, "tratamiento"));
        }
        return professional;
    }

    @Override
    protected int getId(Professional professional) {
        return professional.getId();
    }

    @Override
    protected void setId(Professional professional, int id) {
        professional.setId(id);
    }

    private List<String> treatmentIdsOf(int professionalId) throws DataAccessException {
        if (treatmentIdsByProfessional == null) {
            treatmentIdsByProfessional = readLinks();
        }
        return treatmentIdsByProfessional.getOrDefault(professionalId, List.of());
    }

    private Map<Integer, List<String>> readLinks() throws DataAccessException {
        Map<Integer, List<String>> links = new HashMap<>();
        for (String line : linksStore.readLines()) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = TextLineCodec.decode(line);
            if (fields.length != LINK_FIELD_COUNT) {
                throw new DataAccessException("Línea inválida en " + linksStore.getFile() + ": " + line);
            }
            links.computeIfAbsent(Integer.parseInt(fields[LINK_PROFESSIONAL_ID]), id -> new ArrayList<>())
                .add(fields[LINK_TREATMENT_ID]);
        }
        return links;
    }

    private void writeLinks() throws DataAccessException {
        List<String> lines = new ArrayList<>();
        for (Professional professional : findAll()) {
            for (Treatment treatment : professional.getTreatments()) {
                lines.add(TextLineCodec.encode(String.valueOf(professional.getId()),
                    String.valueOf(treatment.getId())));
            }
        }
        linksStore.writeLines(lines);
    }
}
