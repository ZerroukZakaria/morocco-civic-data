package io.github.zerroukzakaria.civicdata.importer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/** The contents of an election.properties file. */
public record ElectionMetadata(
        String slug,
        String name,
        String kind,
        LocalDate date,
        int expectedSeats,
        String sourceName,
        String sourceUrl,
        String licence,
        String licenceUrl,
        String citation,
        String notes) {

    public static ElectionMetadata load(Path file) {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            throw new ImportException("Cannot read " + file + ": " + e.getMessage(), e);
        }

        String slug = required(p, "slug");
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new ImportException("election.properties: slug must be lowercase letters, digits and hyphens: " + slug);
        }
        return new ElectionMetadata(
                slug,
                required(p, "name"),
                required(p, "kind"),
                parseDate(required(p, "date")),
                parseSeats(required(p, "expected.seats")),
                required(p, "source.name"),
                required(p, "source.url"),
                required(p, "licence"),
                required(p, "licence.url"),
                required(p, "citation"),
                p.getProperty("notes", "").trim());
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ImportException("election.properties: missing required key '" + key + "'");
        }
        return value.trim();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ImportException("election.properties: invalid date '" + value + "' (expected yyyy-mm-dd)", e);
        }
    }

    private static int parseSeats(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ImportException("election.properties: expected.seats is not a number: " + value, e);
        }
    }
}