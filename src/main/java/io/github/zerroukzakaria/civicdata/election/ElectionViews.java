package io.github.zerroukzakaria.civicdata.election;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The JSON shapes returned by the API. */
public final class ElectionViews {

    private ElectionViews() {
    }

    /** Where the data comes from and how it must be credited (CC BY 4.0 requires this). */
    public record Attribution(String sourceName, String sourceUrl, String licence, String licenceUrl,
                              String citation) {
        static Attribution of(Election e) {
            return new Attribution(e.getSourceName(), e.getSourceUrl(), e.getLicence(), e.getLicenceUrl(),
                    e.getCitation());
        }
    }

    public record ElectionSummary(String slug, String name, String kind, LocalDate date) {
    }

    public record ElectionDetail(String slug, String name, String kind, LocalDate date, String notes,
                                 Attribution attribution) {
    }

    /** Constituencies counts both local and regional ones; seats is the sum of their seats. */
    public record RegionSummary(long id, String name, int constituencies, int seats) {
    }

    public record ListResult(String code, String name, String kind, long votes, BigDecimal sharePercent) {
    }

    public record ElectionResults(String election, String scope, Ballot ballot, long totalVotes, String basis,
                                  List<ListResult> lists, Attribution attribution) {
    }
}