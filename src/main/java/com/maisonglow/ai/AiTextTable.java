package com.maisonglow.ai;

import com.maisonglow.dao.TextFileStore;
import com.maisonglow.dao.TextLineCodec;
import com.maisonglow.exception.DataAccessException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Small text table with persistent, monotonic ids and atomic file replacement. */
final class AiTextTable<T> {
    private static final String HEADER = "#nextId=";
    private final TextFileStore store;
    private final Function<String[], T> parse;
    private final Function<T, String[]> fields;

    AiTextTable(Path path, Function<String[], T> parse, Function<T, String[]> fields) {
        store = new TextFileStore(path);
        this.parse = Objects.requireNonNull(parse);
        this.fields = Objects.requireNonNull(fields);
    }

    synchronized T append(Function<Integer, T> create) throws DataAccessException {
        State<T> state = load();
        T item = create.apply(state.nextId);
        state.items.add(item);
        write(state.nextId + 1, state.items);
        return item;
    }

    synchronized List<T> all() throws DataAccessException {
        return List.copyOf(load().items);
    }

    synchronized T update(int id, T replacement) throws DataAccessException {
        State<T> state = load();
        for (int i = 0; i < state.items.size(); i++) {
            if (Integer.parseInt(fields.apply(state.items.get(i))[0]) == id) {
                if (Integer.parseInt(fields.apply(replacement)[0]) != id) {
                    throw new DataAccessException("El id del registro no coincide.");
                }
                state.items.set(i, replacement);
                write(state.nextId, state.items);
                return replacement;
            }
        }
        throw new DataAccessException("No existe el registro " + id + ".");
    }

    private State<T> load() throws DataAccessException {
        int nextId = 1;
        List<T> items = new ArrayList<>();
        List<String> lines = store.readLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            try {
                if (line.startsWith(HEADER)) {
                    nextId = Math.max(nextId, Integer.parseInt(line.substring(HEADER.length())));
                } else {
                    String[] raw = TextLineCodec.decode(line);
                    T item = parse.apply(raw);
                    int id = Integer.parseInt(fields.apply(item)[0]);
                    if (id < 1 || items.stream().anyMatch(existing ->
                        Integer.parseInt(fields.apply(existing)[0]) == id)) {
                        throw new IllegalArgumentException("Id duplicado o inválido");
                    }
                    items.add(item);
                    nextId = Math.max(nextId, id + 1);
                }
            } catch (RuntimeException e) {
                throw new DataAccessException("La línea " + (i + 1) + " de " + store.getFile()
                    + " no es válida.", e);
            }
        }
        return new State<>(nextId, items);
    }

    private void write(int nextId, List<T> items) throws DataAccessException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER + nextId);
        for (T item : items) lines.add(TextLineCodec.encode(fields.apply(item)));
        store.writeLines(lines);
    }

    private record State<T>(int nextId, List<T> items) { }
}
