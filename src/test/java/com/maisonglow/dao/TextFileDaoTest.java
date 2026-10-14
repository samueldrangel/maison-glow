package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextFileDao tests")
class TextFileDaoTest {

    /** Minimal entity used to exercise the base class without real models. */
    static final class Note {
        private int id;
        private final String text;

        Note(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    /** Minimal DAO; the label prefix proves subclass fields are ready when loading. */
    static final class NoteDao extends TextFileDao<Note> {
        private final String labelPrefix;

        NoteDao(Path file, String labelPrefix) {
            super(file);
            this.labelPrefix = labelPrefix;
        }

        @Override
        protected String[] toFields(Note note) {
            return new String[] {String.valueOf(note.id), note.text};
        }

        @Override
        protected Note fromFields(String[] fields) {
            return new Note(Integer.parseInt(fields[0]), labelPrefix + fields[1]);
        }

        @Override
        protected int getId(Note note) {
            return note.id;
        }

        @Override
        protected void setId(Note note, int id) {
            note.id = id;
        }
    }

    @TempDir
    Path folder;

    private Path file;
    private NoteDao dao;

    @BeforeEach
    void setUp() {
        file = folder.resolve("notes.txt");
        dao = new NoteDao(file, "");
    }

    @Test
    @DisplayName("create assigns consecutive ids and saves the file")
    void createAssignsIdsAndSaves() throws Exception {
        Note first = dao.create(new Note(0, "Corte"));
        Note second = dao.create(new Note(0, "Tinte"));

        assertThat(first.id).isEqualTo(1);
        assertThat(second.id).isEqualTo(2);
        assertThat(Files.readAllLines(file)).containsExactly("1|Corte", "2|Tinte");
    }

    @Test
    @DisplayName("findById returns the entity or an empty Optional")
    void findById() throws DataAccessException {
        dao.create(new Note(0, "Corte"));

        assertThat(dao.findById(1)).hasValueSatisfying(note -> assertThat(note.text).isEqualTo("Corte"));
        assertThat(dao.findById(99)).isEmpty();
    }

    @Test
    @DisplayName("findAll lists the entities ordered by id")
    void findAllOrderedById() throws DataAccessException {
        dao.create(new Note(0, "Corte"));
        dao.create(new Note(0, "Tinte"));

        assertThat(dao.findAll()).extracting(note -> note.text).containsExactly("Corte", "Tinte");
    }

    @Test
    @DisplayName("update replaces the stored entity and saves it")
    void updateSaves() throws Exception {
        dao.create(new Note(0, "Corte"));

        dao.update(new Note(1, "Corte y secado"));

        assertThat(Files.readAllLines(file)).containsExactly("1|Corte y secado");
    }

    @Test
    @DisplayName("delete removes the entity from memory and from the file")
    void deleteRemoves() throws Exception {
        dao.create(new Note(0, "Corte"));
        dao.create(new Note(0, "Tinte"));

        dao.delete(1);

        assertThat(dao.findById(1)).isEmpty();
        assertThat(Files.readAllLines(file)).containsExactly("2|Tinte");
    }

    @Test
    @DisplayName("update and delete of a missing id fail")
    void missingIdFails() {
        assertThatThrownBy(() -> dao.update(new Note(7, "Nada"))).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> dao.delete(7)).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("a new DAO on the same file reloads the data and continues the ids")
    void reloadFromDisk() throws DataAccessException {
        dao.create(new Note(0, "Corte"));
        dao.create(new Note(0, "Tinte"));

        NoteDao reopened = new NoteDao(file, "");
        Note third = reopened.create(new Note(0, "Manicura"));

        assertThat(reopened.findAll()).hasSize(3);
        assertThat(third.id).isEqualTo(3);
    }

    @Test
    @DisplayName("text with | and \\ survives a save and a reload")
    void escapedTextSurvivesReload() throws DataAccessException {
        dao.create(new Note(0, "Tinte | mechas \\ brillo"));

        Note reloaded = new NoteDao(file, "").findById(1).orElseThrow();

        assertThat(reloaded.text).isEqualTo("Tinte | mechas \\ brillo");
    }

    @Test
    @DisplayName("loading waits until the subclass fields are initialized")
    void loadingUsesSubclassFields() throws Exception {
        Files.writeString(file, "1|Corte");

        Note loaded = new NoteDao(file, "Nota: ").findById(1).orElseThrow();

        assertThat(loaded.text).isEqualTo("Nota: Corte");
    }

    @Test
    @DisplayName("a malformed line is reported with its line number")
    void malformedLineFails() throws IOException {
        Files.write(file, List.of("1|Corte", "x|Tinte"));

        assertThatThrownBy(() -> dao.findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("línea 2");
    }

    @Test
    @DisplayName("if saving fails, the change is undone in memory")
    void failedSaveIsUndone() throws Exception {
        dao.create(new Note(0, "Corte"));
        Files.delete(file);
        Files.createDirectories(file.resolve("blocks-the-file"));

        Note rejected = new Note(0, "Tinte");

        assertThatThrownBy(() -> dao.create(rejected)).isInstanceOf(DataAccessException.class);
        assertThat(rejected.id).isZero();
        assertThat(dao.findAll()).hasSize(1);
    }
}
