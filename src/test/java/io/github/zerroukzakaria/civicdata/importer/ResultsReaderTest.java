package io.github.zerroukzakaria.civicdata.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResultsReaderTest {

    private static final String HEADER = String.join("\t", ResultsReader.HEADER);
    private static final String VALID_ROW = String.join("\t",
            "101", "Test constituency", "LOCAL", "Test region", "", "Some province", "",
            "5", "0.5000", "AAA", "Party A", "PARTY", "1200");

    private final ResultsReader reader = new ResultsReader();

    private static Path write(Path dir, String... lines) throws IOException {
        return Files.writeString(dir.resolve("results.tsv"), String.join("\n", lines) + "\n");
    }

    @Test
    void parsesAValidRow(@TempDir Path dir) throws IOException {
        List<ResultRow> rows = reader.read(write(dir, HEADER, VALID_ROW));

        assertThat(rows).hasSize(1);
        ResultRow row = rows.get(0);
        assertThat(row.constituencySourceId()).isEqualTo(101);
        assertThat(row.kind()).isEqualTo("LOCAL");
        assertThat(row.wilaya()).isNull();
        assertThat(row.prefectureOrProvince()).isEqualTo("Some province");
        assertThat(row.turnoutRate()).isEqualTo(0.5);
        assertThat(row.votes()).isEqualTo(1200L);
    }

    @Test
    void toleratesAByteOrderMark(@TempDir Path dir) throws IOException {
        assertThat(reader.read(write(dir, "\uFEFF" + HEADER, VALID_ROW))).hasSize(1);
    }

    @Test
    void rejectsAnUnexpectedHeader(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a\tb", VALID_ROW);
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Unexpected header");
    }

    @Test
    void rejectsTheWrongNumberOfFieldsAndNamesTheLine(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER, "101\tonly two");
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Line 2");
    }

    @Test
    void rejectsNegativeVotes(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER, VALID_ROW.replaceAll("1200$", "-5"));
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("votes");
    }

    @Test
    void rejectsNonNumericVotes(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER, VALID_ROW.replaceAll("1200$", "abc"));
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Line 2");
    }
}