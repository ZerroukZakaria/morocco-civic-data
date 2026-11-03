package io.github.zerroukzakaria.civicdata.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads results.tsv: tab-separated, UTF-8, one header line, one row per constituency and list. */
public final class ResultsReader {

    static final List<String> HEADER = List.of(
            "constituency_source_id", "constituency_name", "kind", "region", "wilaya",
            "prefecture_or_province", "sub_prefecture", "seats", "turnout_rate",
            "party_code", "party_name", "party_kind", "votes");

    public List<ResultRow> read(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ImportException("Cannot read " + file + ": " + e.getMessage(), e);
        }
        if (lines.isEmpty()) {
            throw new ImportException(file + " is empty");
        }

        String headerLine = lines.get(0);
        if (headerLine.startsWith("\uFEFF")) {
            headerLine = headerLine.substring(1); // tolerate a byte order mark
        }
        List<String> header = List.of(headerLine.split("\t", -1));
        if (!header.equals(HEADER)) {
            throw new ImportException("Unexpected header in " + file + ": " + header);
        }

        List<ResultRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                rows.add(parseLine(lines.get(i), i + 1));
            }
        }
        return rows;
    }

    static ResultRow parseLine(String line, int lineNumber) {
        String[] f = line.split("\t", -1);
        if (f.length != HEADER.size()) {
            throw new ImportException("Line %d: expected %d fields but found %d"
                    .formatted(lineNumber, HEADER.size(), f.length));
        }
        try {
            int seats = Integer.parseInt(f[7].trim());
            if (seats <= 0) {
                throw new ImportException("Line %d: seats must be positive (%d)".formatted(lineNumber, seats));
            }
            Double turnout = f[8].isBlank() ? null : Double.parseDouble(f[8].trim());
            if (turnout != null && (turnout < 0 || turnout > 1)) {
                throw new ImportException("Line %d: turnout_rate must be between 0 and 1 (%s)"
                        .formatted(lineNumber, turnout));
            }
            long votes = Long.parseLong(f[12].trim());
            if (votes < 0) {
                throw new ImportException("Line %d: votes must not be negative (%d)".formatted(lineNumber, votes));
            }
            return new ResultRow(
                    Integer.parseInt(f[0].trim()),
                    required(f[1], "constituency_name", lineNumber),
                    oneOf(f[2], "kind", lineNumber, "LOCAL", "REGIONAL"),
                    required(f[3], "region", lineNumber),
                    optional(f[4]),
                    optional(f[5]),
                    optional(f[6]),
                    seats,
                    turnout,
                    required(f[9], "party_code", lineNumber),
                    required(f[10], "party_name", lineNumber),
                    oneOf(f[11], "party_kind", lineNumber, "PARTY", "COALITION"),
                    votes);
        } catch (NumberFormatException e) {
            throw new ImportException("Line %d: invalid number (%s)".formatted(lineNumber, e.getMessage()), e);
        }
    }

    private static String required(String value, String column, int lineNumber) {
        if (value.isBlank()) {
            throw new ImportException("Line %d: %s is required".formatted(lineNumber, column));
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value.isBlank() ? null : value.trim();
    }

    private static String oneOf(String value, String column, int lineNumber, String... allowed) {
        String trimmed = value.trim();
        for (String candidate : allowed) {
            if (candidate.equals(trimmed)) {
                return trimmed;
            }
        }
        throw new ImportException("Line %d: %s must be one of %s but was '%s'"
                .formatted(lineNumber, column, String.join(", ", allowed), trimmed));
    }
}