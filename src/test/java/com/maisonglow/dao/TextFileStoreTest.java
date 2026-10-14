package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextFileStore tests")
class TextFileStoreTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("a file that does not exist reads as empty")
    void missingFileReadsAsEmpty() throws DataAccessException {
        TextFileStore store = new TextFileStore(folder.resolve("missing.txt"));

        assertThat(store.readLines()).isEmpty();
    }

    @Test
    @DisplayName("written lines are read back in the same order, in UTF-8")
    void writeThenRead() throws DataAccessException {
        TextFileStore store = new TextFileStore(folder.resolve("items.txt"));

        store.writeLines(List.of("1|Uñas", "2|Peluquería"));

        assertThat(store.readLines()).containsExactly("1|Uñas", "2|Peluquería");
    }

    @Test
    @DisplayName("writing creates missing folders and leaves no temporary file")
    void writeCreatesFoldersWithoutLeftovers() throws DataAccessException {
        Path file = folder.resolve("data").resolve("items.txt");

        new TextFileStore(file).writeLines(List.of("1|Corte"));

        assertThat(file).exists();
        assertThat(Files.exists(file.resolveSibling("items.txt.tmp"))).isFalse();
    }

    @Test
    @DisplayName("writing replaces the previous content")
    void writeReplacesContent() throws DataAccessException {
        TextFileStore store = new TextFileStore(folder.resolve("items.txt"));
        store.writeLines(List.of("1|Corte", "2|Tinte"));

        store.writeLines(List.of("2|Tinte"));

        assertThat(store.readLines()).containsExactly("2|Tinte");
    }
}
