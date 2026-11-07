package io.github.zerroukzakaria.civicdata.election;

import io.github.zerroukzakaria.civicdata.election.ElectionViews.Attribution;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencyDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencySummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ListResult;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.PagedResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ConstituencyService {

    static final int MAX_PAGE_SIZE = 100;

    /** The only sort keys accepted. The SQL text comes from here, never from the request. */
    public enum Sort {
        NAME("c.name"),
        SEATS("c.seats"),
        VOTES("total_votes"),
        TURNOUT("c.turnout_rate");

        private final String sql;

        Sort(String sql) {
            this.sql = sql;
        }

        String sql() {
            return sql;
        }

        static Sort parse(String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw badRequest("sort must be one of: name, seats, votes, turnout");
            }
        }
    }

    private record Head(long id, int sourceId, String name, Ballot ballot, long regionId, String region,
                        String wilaya, String prefectureOrProvince, String subPrefecture, int seats,
                        BigDecimal turnoutRate) {
    }

    private record Row(String code, String name, String kind, long votes) {
    }

    private final ElectionRepository elections;
    private final JdbcClient jdbc;

    public ConstituencyService(ElectionRepository elections, JdbcClient jdbc) {
        this.elections = elections;
        this.jdbc = jdbc;
    }

    public PagedResult<ConstituencySummary> list(String slug, Ballot ballot, Long regionId, int page, int size,
                                                 String sort, String direction) {
        Election election = find(slug);
        if (page < 0) {
            throw badRequest("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Sort sortBy = Sort.parse(sort);
        String order = parseDirection(direction);

        StringBuilder where = new StringBuilder("WHERE c.election_id = :electionId");
        if (ballot != null) {
            where.append(" AND c.kind = :ballot");
        }
        if (regionId != null) {
            where.append(" AND c.region_id = :regionId");
        }

        long totalItems = bind(jdbc.sql("SELECT COUNT(*) FROM constituency c " + where),
                election.getId(), ballot, regionId)
                .query(Long.class)
                .single();

        String sql = "SELECT c.id AS id, c.source_id AS source_id, c.name AS name, c.kind AS kind, "
                + "g.id AS region_id, g.name AS region, c.seats AS seats, c.turnout_rate AS turnout_rate, "
                + "CAST(COALESCE(SUM(r.votes), 0) AS BIGINT) AS total_votes "
                + "FROM constituency c "
                + "JOIN region g ON g.id = c.region_id "
                + "LEFT JOIN result r ON r.constituency_id = c.id "
                + where
                + " GROUP BY c.id, g.id, g.name"
                + " ORDER BY " + sortBy.sql() + " " + order + " NULLS LAST, c.id"
                + " LIMIT :size OFFSET :offset";

        List<ConstituencySummary> items = bind(jdbc.sql(sql), election.getId(), ballot, regionId)
                .param("size", size)
                .param("offset", (long) page * size)
                .query((rs, n) -> new ConstituencySummary(
                        rs.getLong("id"),
                        rs.getInt("source_id"),
                        rs.getString("name"),
                        Ballot.valueOf(rs.getString("kind")),
                        rs.getLong("region_id"),
                        rs.getString("region"),
                        rs.getInt("seats"),
                        rs.getBigDecimal("turnout_rate"),
                        rs.getLong("total_votes")))
                .list();

        int totalPages = (int) ((totalItems + size - 1) / size);
        return new PagedResult<>(items, page, size, totalItems, totalPages);
    }

    public ConstituencyDetail detail(String slug, long id) {
        Election election = find(slug);

        Head head = jdbc.sql("""
                        SELECT c.id AS id, c.source_id AS source_id, c.name AS name, c.kind AS kind,
                               g.id AS region_id, g.name AS region, c.wilaya AS wilaya,
                               c.prefecture_or_province AS prefecture, c.sub_prefecture AS sub_prefecture,
                               c.seats AS seats, c.turnout_rate AS turnout_rate
                        FROM constituency c
                        JOIN region g ON g.id = c.region_id
                        WHERE c.id = :id AND c.election_id = :electionId""")
                .param("id", id)
                .param("electionId", election.getId())
                .query((rs, n) -> new Head(
                        rs.getLong("id"),
                        rs.getInt("source_id"),
                        rs.getString("name"),
                        Ballot.valueOf(rs.getString("kind")),
                        rs.getLong("region_id"),
                        rs.getString("region"),
                        rs.getString("wilaya"),
                        rs.getString("prefecture"),
                        rs.getString("sub_prefecture"),
                        rs.getInt("seats"),
                        rs.getBigDecimal("turnout_rate")))
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Constituency " + id + " not found in election " + slug));

        List<Row> rows = jdbc.sql("""
                        SELECT p.code AS code, p.name AS name, p.kind AS kind, r.votes AS votes
                        FROM result r
                        JOIN party p ON p.id = r.party_id
                        WHERE r.constituency_id = :id
                        ORDER BY r.votes DESC, p.code""")
                .param("id", id)
                .query((rs, n) -> new Row(rs.getString("code"), rs.getString("name"),
                        rs.getString("kind"), rs.getLong("votes")))
                .list();

        long total = rows.stream().mapToLong(Row::votes).sum();
        List<ListResult> lists = rows.stream()
                .map(r -> new ListResult(r.code(), r.name(), r.kind(), r.votes(), Shares.percent(r.votes(), total)))
                .toList();

        return new ConstituencyDetail(head.id(), head.sourceId(), head.name(), head.ballot(), head.regionId(),
                head.region(), head.wilaya(), head.prefectureOrProvince(), head.subPrefecture(), head.seats(),
                head.turnoutRate(), total, ResultsService.BASIS, lists, Attribution.of(election));
    }

    private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, long electionId, Ballot ballot,
                                          Long regionId) {
        statement = statement.param("electionId", electionId);
        if (ballot != null) {
            statement = statement.param("ballot", ballot.name());
        }
        if (regionId != null) {
            statement = statement.param("regionId", regionId);
        }
        return statement;
    }

    private static String parseDirection(String direction) {
        String value = direction.trim().toLowerCase(Locale.ROOT);
        if (value.equals("asc")) {
            return "ASC";
        }
        if (value.equals("desc")) {
            return "DESC";
        }
        throw badRequest("direction must be asc or desc");
    }

    private Election find(String slug) {
        return elections.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Election not found: " + slug));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}