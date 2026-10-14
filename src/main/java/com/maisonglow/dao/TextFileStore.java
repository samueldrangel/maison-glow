package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads and writes the lines of one UTF-8 data file. Writing goes through a
 * temporary file that then replaces the original, so an interrupted write
 * never leaves a half-written data file behind.
 */
public final class TextFileStore {

    private static final String TEMP_SUFFIX = ".tmp";
    private static final String BYTE_ORDER_MARK = "﻿";

    private final Path file;

    /**
     * Creates a store for the given file. The file does not need to exist yet.
     *
     * @param file the path of the data file
     */
    public TextFileStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /**
     * Reads every line of the file.
     *
     * @return the lines of the file; empty if the file does not exist yet
     * @throws DataAccessException if the file exists but cannot be read
     */
    public List<String> readLines() throws DataAccessException {
        if (Files.notExists(file)) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            removeByteOrderMark(lines);
            return lines;
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer el archivo " + file, e);
        }
    }

    /**
     * Replaces the content of the file with the given lines, creating the
     * file and its folders if needed.
     *
     * @param lines the lines to write, without line terminators
     * @throws DataAccessException if the file cannot be written
     */
    public void writeLines(List<String> lines) throws DataAccessException {
        Path tempFile = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            createParentFolders();
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            replaceFileWith(tempFile);
        } catch (IOException e) {
            deleteLeftover(tempFile, e);
            throw new DataAccessException("No se pudo guardar el archivo " + file, e);
        }
    }

    /**
     * Returns the path of the data file.
     *
     * @return the file path
     */
    public Path getFile() {
        return file;
    }

    private void createParentFolders() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void replaceFileWith(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteLeftover(Path tempFile, IOException writeError) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException cleanupError) {
            writeError.addSuppressed(cleanupError);
        }
    }

    private static void removeByteOrderMark(List<String> lines) {
        if (!lines.isEmpty() && lines.get(0).startsWith(BYTE_ORDER_MARK)) {
            lines.set(0, lines.get(0).substring(BYTE_ORDER_MARK.length()));
        }
    }
}
