package io.github.zerroukzakaria.civicdata.importer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectionValidatorTest {

    private final ElectionValidator validator = new ElectionValidator();

    private static ElectionMetadata meta(int expectedSeats) {
        return new ElectionMetadata("test-election", "Test", "LEGISLATIVE", LocalDate.of(2021, 9, 8),
                expectedSeats, "source", "https://example.org", "CC BY 4.0",
                "https://example.org/licence", "citation", "");
    }

    private static ResultRow row(int id, String kind, int seats, String code, String partyName, long votes) {
        return new ResultRow(id, "Constituency " + id, kind, "Region", null, null, null,
                seats, 0.5, code, partyName, "PARTY", votes);
    }

    private static ResultRow row(int id, String kind, int seats, String code, long votes) {
        return row(id, kind, seats, code, "Party " + code, votes);
    }

    private static List<ResultRow> consistentData() {
        return List.of(
                row(1, "LOCAL", 3, "AAA", 600),
                row(1, "LOCAL", 3, "BBB", 400),
                row(2, "REGIONAL", 2, "AAA", 500),
                row(2, "REGIONAL", 2, "BBB", 495));
    }

    @Test
    void acceptsConsistentData() {
        assertThatCode(() -> validator.validate(meta(5), consistentData())).doesNotThrowAnyException();
    }

    @Test
    void rejectsAWrongSeatTotal() {
        assertThatThrownBy(() -> validator.validate(meta(6), consistentData()))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("seats");
    }

    @Test
    void rejectsADuplicateResult() {
        List<ResultRow> rows = List.of(
                row(1, "LOCAL", 5, "AAA", 600),
                row(1, "LOCAL", 5, "AAA", 700));
        assertThatThrownBy(() -> validator.validate(meta(5), rows))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void rejectsLocalAndRegionalTotalsThatDiffer() {
        List<ResultRow> rows = List.of(
                row(1, "LOCAL", 3, "AAA", 1000),
                row(2, "REGIONAL", 2, "AAA", 600));
        assertThatThrownBy(() -> validator.validate(meta(5), rows))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("differ");
    }

    @Test
    void rejectsInconsistentConstituencyDetails() {
        List<ResultRow> rows = List.of(
                row(1, "LOCAL", 3, "AAA", 600),
                row(1, "LOCAL", 4, "BBB", 400));
        assertThatThrownBy(() -> validator.validate(meta(3), rows))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("inconsistent");
    }

    @Test
    void rejectsAListCodeWithTwoNames() {
        List<ResultRow> rows = List.of(
                row(1, "LOCAL", 3, "AAA", "Party One", 600),
                row(2, "LOCAL", 2, "AAA", "Party Two", 400));
        assertThatThrownBy(() -> validator.validate(meta(5), rows))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("AAA");
    }
}