package io.github.zerroukzakaria.civicdata.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks the committed 2021 files against numbers verified independently from the source workbook. */
class NormalizedFilesTest {

    private static final Path DIR = Path.of("data", "normalized", "legislative-2021");

    private static long votes(List<ResultRow> rows, String kind) {
        return rows.stream().filter(r -> r.kind().equals(kind)).mapToLong(ResultRow::votes).sum();
    }

    @Test
    void the2021FilesPassValidationAndMatchTheSourceTotals() {
        ElectionMetadata meta = ElectionMetadata.load(DIR.resolve("election.properties"));
        List<ResultRow> rows = new ResultsReader().read(DIR.resolve("results.tsv"));

        assertThatCode(() -> new ElectionValidator().validate(meta, rows)).doesNotThrowAnyException();

        Map<Integer, ResultRow.Constituency> constituencies = new LinkedHashMap<>();
        rows.forEach(r -> constituencies.putIfAbsent(r.constituencySourceId(), r.constituency()));

        assertThat(meta.slug()).isEqualTo("legislative-2021");
        assertThat(rows).hasSize(1705);
        assertThat(constituencies).hasSize(104);
        assertThat(rows.stream().map(ResultRow::partyCode).distinct().count()).isEqualTo(30);
        assertThat(constituencies.values().stream().mapToInt(ResultRow.Constituency::seats).sum()).isEqualTo(395);
        assertThat(votes(rows, "LOCAL")).isEqualTo(7_588_505L);
        assertThat(votes(rows, "REGIONAL")).isEqualTo(7_571_623L);
    }
}