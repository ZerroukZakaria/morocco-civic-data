package io.github.zerroukzakaria.civicdata.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Dataset-level checks. Collects every problem it finds, then reports them together. */
public final class ElectionValidator {

    /** Local and regional ballots are cast by the same voters, so their totals should be close. */
    private static final double TOTALS_TOLERANCE = 0.01;

    public void validate(ElectionMetadata meta, List<ResultRow> rows) {
        List<String> problems = new ArrayList<>();
        if (rows.isEmpty()) {
            problems.add("the results file has no rows");
        } else {
            Map<Integer, ResultRow.Constituency> constituencies = checkConstituencies(rows, problems);
            checkDuplicates(rows, problems);
            checkSeats(meta, constituencies, problems);
            checkTotals(rows, problems);
            checkParties(rows, problems);
        }
        if (!problems.isEmpty()) {
            throw new ImportException("Validation failed:\n - " + String.join("\n - ", problems));
        }
    }

    private Map<Integer, ResultRow.Constituency> checkConstituencies(List<ResultRow> rows, List<String> problems) {
        Map<Integer, ResultRow.Constituency> byId = new LinkedHashMap<>();
        Set<Integer> reported = new HashSet<>();
        for (ResultRow row : rows) {
            ResultRow.Constituency previous = byId.putIfAbsent(row.constituencySourceId(), row.constituency());
            if (previous != null
                    && !previous.equals(row.constituency())
                    && reported.add(row.constituencySourceId())) {
                problems.add("constituency %d (%s) has inconsistent details across rows"
                        .formatted(row.constituencySourceId(), row.constituencyName()));
            }
        }
        return byId;
    }

    private void checkDuplicates(List<ResultRow> rows, List<String> problems) {
        Set<String> seen = new HashSet<>();
        for (ResultRow row : rows) {
            if (!seen.add(row.constituencySourceId() + "|" + row.partyCode())) {
                problems.add("duplicate result for constituency %d and list %s"
                        .formatted(row.constituencySourceId(), row.partyCode()));
            }
        }
    }

    private void checkSeats(ElectionMetadata meta, Map<Integer, ResultRow.Constituency> constituencies,
                            List<String> problems) {
        int total = constituencies.values().stream().mapToInt(ResultRow.Constituency::seats).sum();
        if (total != meta.expectedSeats()) {
            problems.add("the seats add up to %d but %d were expected".formatted(total, meta.expectedSeats()));
        }
    }

    private void checkTotals(List<ResultRow> rows, List<String> problems) {
        Map<String, Long> totals = rows.stream()
                .collect(Collectors.groupingBy(ResultRow::kind, Collectors.summingLong(ResultRow::votes)));
        Long local = totals.get("LOCAL");
        Long regional = totals.get("REGIONAL");
        if (local == null || regional == null) {
            return; // the check only applies when both kinds of ballot are present
        }
        long larger = Math.max(local, regional);
        if (larger == 0) {
            problems.add("no votes at all in the results");
            return;
        }
        double gap = Math.abs(local - regional) / (double) larger;
        if (gap > TOTALS_TOLERANCE) {
            problems.add("local votes (%d) and regional votes (%d) differ by %.1f%%"
                    .formatted(local, regional, gap * 100));
        }
    }

    private void checkParties(List<ResultRow> rows, List<String> problems) {
        Map<String, String> signatureByCode = new HashMap<>();
        Set<String> reported = new HashSet<>();
        for (ResultRow row : rows) {
            String signature = row.partyName() + "|" + row.partyKind();
            String previous = signatureByCode.putIfAbsent(row.partyCode(), signature);
            if (previous != null && !previous.equals(signature) && reported.add(row.partyCode())) {
                problems.add("list code %s appears with different names or kinds".formatted(row.partyCode()));
            }
        }
    }
}