package io.github.zerroukzakaria.civicdata.election;

import io.github.zerroukzakaria.civicdata.election.ElectionViews.Attribution;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionResults;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionSummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ListResult;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.RegionSummary;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ResultsService {

    static final String BASIS = "Share of the votes cast for lists in this ballot. "
            + "Blank and spoilt votes are not available in the source data.";

    private record Row(String code, String name, String kind, long votes) {
    }

    private final ElectionRepository elections;
    private final JdbcClient jdbc;

    public ResultsService(ElectionRepository elections, JdbcClient jdbc) {
        this.elections = elections;
        this.jdbc = jdbc;
    }

    public List<ElectionSummary> listElections() {
        return elections.findAllByOrderByElectionDateDesc().stream()
                .map(e -> new ElectionSummary(e.getSlug(), e.getName(), e.getKind(), e.getElectionDate()))
                .toList();
    }

    public ElectionDetail electionDetail(String slug) {
        Election e = find(slug);
        return new ElectionDetail(e.getSlug(), e.getName(), e.getKind(), e.getElectionDate(), e.getNotes(),
                Attribution.of(e));
    }

    public ElectionResults nationalResults(String slug, Ballot ballot) {
        Election e = find(slug);
        return build(e, "national", ballot, rows(e.getId(), null, ballot));
    }

    public List<RegionSummary> regions(String slug) {
        Election e = find(slug);
        return jdbc.sql("""
                        SELECT g.id AS id, g.name AS name,
                               CAST(COUNT(*) AS INTEGER) AS constituencies,
                               CAST(SUM(c.seats) AS INTEGER) AS seats
                        FROM constituency c
                        JOIN region g ON g.id = c.region_id
                        WHERE c.election_id = :electionId
                        GROUP BY g.id, g.name
                        ORDER BY g.name""")
                .param("electionId", e.getId())
                .query((rs, n) -> new RegionSummary(rs.getLong("id"), rs.getString("name"),
                        rs.getInt("constituencies"), rs.getInt("seats")))
                .list();
    }

    public ElectionResults regionResults(String slug, long regionId, Ballot ballot) {
        Election e = find(slug);
        String regionName = jdbc.sql("""
                        SELECT g.name FROM region g
                        WHERE g.id = :regionId
                          AND EXISTS (SELECT 1 FROM constituency c
                                      WHERE c.region_id = g.id AND c.election_id = :electionId)""")
                .param("regionId", regionId)
                .param("electionId", e.getId())
                .query(String.class)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Region " + regionId + " not found in election " + slug));
        return build(e, regionName, ballot, rows(e.getId(), regionId, ballot));
    }

    private Election find(String slug) {
        return elections.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Election not found: " + slug));
    }

    private List<Row> rows(long electionId, Long regionId, Ballot ballot) {
        String sql = """
                SELECT p.code AS code, p.name AS name, p.kind AS kind,
                       CAST(SUM(r.votes) AS BIGINT) AS votes
                FROM result r
                JOIN constituency c ON c.id = r.constituency_id
                JOIN party p ON p.id = r.party_id
                WHERE c.election_id = :electionId AND c.kind = :ballot
                """
                + (regionId == null ? "" : "  AND c.region_id = :regionId\n")
                + """
                GROUP BY p.code, p.name, p.kind
                ORDER BY votes DESC, p.code
                """;
        var statement = jdbc.sql(sql)
                .param("electionId", electionId)
                .param("ballot", ballot.name());
        if (regionId != null) {
            statement = statement.param("regionId", regionId);
        }
        return statement
                .query((rs, n) -> new Row(rs.getString("code"), rs.getString("name"),
                        rs.getString("kind"), rs.getLong("votes")))
                .list();
    }

    private ElectionResults build(Election e, String scope, Ballot ballot, List<Row> rows) {
        long total = rows.stream().mapToLong(Row::votes).sum();
        List<ListResult> lists = rows.stream()
                .map(r -> new ListResult(r.code(), r.name(), r.kind(), r.votes(), Shares.percent(r.votes(), total)))
                .toList();
        return new ElectionResults(e.getSlug(), scope, ballot, total, BASIS, lists, Attribution.of(e));
    }
}