package io.github.zerroukzakaria.civicdata.importer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads one election (election.properties + results.tsv) into the database, all or nothing. */
@Service
public class ElectionImporter {

    public record ImportSummary(long electionId, int constituencies, int lists, int results, long totalVotes) {
    }

    private record ExistingParty(long id, String name, String kind) {
    }

    private final JdbcClient jdbc;
    private final ResultsReader reader = new ResultsReader();
    private final ElectionValidator validator = new ElectionValidator();

    public ElectionImporter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public ImportSummary importDirectory(Path directory) {
        ElectionMetadata meta = ElectionMetadata.load(directory.resolve("election.properties"));
        List<ResultRow> rows = reader.read(directory.resolve("results.tsv"));
        validator.validate(meta, rows);

        long existing = jdbc.sql("SELECT count(*) FROM election WHERE slug = :slug")
                .param("slug", meta.slug())
                .query(Long.class)
                .single();
        if (existing > 0) {
            throw new ImportException("Election '" + meta.slug() + "' is already imported. "
                    + "Delete it first (DELETE FROM election WHERE slug = '" + meta.slug() + "') to import it again.");
        }

        long electionId = jdbc.sql("""
                        INSERT INTO election (slug, name, kind, election_date, source_name, source_url,
                                              licence, licence_url, citation, notes)
                        VALUES (:slug, :name, :kind, :date, :sourceName, :sourceUrl,
                                :licence, :licenceUrl, :citation, :notes)
                        RETURNING id""")
                .param("slug", meta.slug())
                .param("name", meta.name())
                .param("kind", meta.kind())
                .param("date", meta.date())
                .param("sourceName", meta.sourceName())
                .param("sourceUrl", meta.sourceUrl())
                .param("licence", meta.licence())
                .param("licenceUrl", meta.licenceUrl())
                .param("citation", meta.citation())
                .param("notes", meta.notes())
                .query(Long.class)
                .single();

        Map<String, Long> regionIds = insertRegions(rows);
        Map<String, Long> partyIds = insertParties(rows);
        Map<Integer, Long> constituencyIds = insertConstituencies(electionId, rows, regionIds);

        long totalVotes = 0;
        for (ResultRow row : rows) {
            jdbc.sql("INSERT INTO result (constituency_id, party_id, votes) VALUES (:constituencyId, :partyId, :votes)")
                    .param("constituencyId", constituencyIds.get(row.constituencySourceId()))
                    .param("partyId", partyIds.get(row.partyCode()))
                    .param("votes", row.votes())
                    .update();
            totalVotes += row.votes();
        }
        return new ImportSummary(electionId, constituencyIds.size(), partyIds.size(), rows.size(), totalVotes);
    }

    private Map<String, Long> insertRegions(List<ResultRow> rows) {
        Map<String, Long> ids = new HashMap<>();
        for (String region : rows.stream().map(ResultRow::region).distinct().toList()) {
            jdbc.sql("INSERT INTO region (name) VALUES (:name) ON CONFLICT (name) DO NOTHING")
                    .param("name", region)
                    .update();
            long id = jdbc.sql("SELECT id FROM region WHERE name = :name")
                    .param("name", region)
                    .query(Long.class)
                    .single();
            ids.put(region, id);
        }
        return ids;
    }

    private Map<String, Long> insertParties(List<ResultRow> rows) {
        Map<String, Long> ids = new HashMap<>();
        for (ResultRow row : rows) {
            if (ids.containsKey(row.partyCode())) {
                continue;
            }
            Optional<ExistingParty> existing = jdbc.sql("SELECT id, name, kind FROM party WHERE code = :code")
                    .param("code", row.partyCode())
                    .query((rs, n) -> new ExistingParty(rs.getLong("id"), rs.getString("name"), rs.getString("kind")))
                    .optional();
            if (existing.isPresent()) {
                ExistingParty party = existing.get();
                if (!party.name().equals(row.partyName()) || !party.kind().equals(row.partyKind())) {
                    throw new ImportException("List code " + row.partyCode() + " already exists as '" + party.name()
                            + "' (" + party.kind() + ") but this file calls it '" + row.partyName()
                            + "' (" + row.partyKind() + ")");
                }
                ids.put(row.partyCode(), party.id());
            } else {
                long id = jdbc.sql("INSERT INTO party (code, name, kind) VALUES (:code, :name, :kind) RETURNING id")
                        .param("code", row.partyCode())
                        .param("name", row.partyName())
                        .param("kind", row.partyKind())
                        .query(Long.class)
                        .single();
                ids.put(row.partyCode(), id);
            }
        }
        return ids;
    }

    private Map<Integer, Long> insertConstituencies(long electionId, List<ResultRow> rows,
                                                    Map<String, Long> regionIds) {
        Map<Integer, Long> ids = new LinkedHashMap<>();
        for (ResultRow row : rows) {
            if (ids.containsKey(row.constituencySourceId())) {
                continue;
            }
            // CAST gives PostgreSQL an explicit type when a nullable value is NULL
            long id = jdbc.sql("""
                            INSERT INTO constituency (election_id, source_id, name, kind, region_id, wilaya,
                                                      prefecture_or_province, sub_prefecture, seats, turnout_rate)
                            VALUES (:electionId, :sourceId, :name, :kind, :regionId, CAST(:wilaya AS TEXT),
                                    CAST(:prefecture AS TEXT), CAST(:subPrefecture AS TEXT), :seats,
                                    CAST(:turnout AS NUMERIC))
                            RETURNING id""")
                    .param("electionId", electionId)
                    .param("sourceId", row.constituencySourceId())
                    .param("name", row.constituencyName())
                    .param("kind", row.kind())
                    .param("regionId", regionIds.get(row.region()))
                    .param("wilaya", row.wilaya())
                    .param("prefecture", row.prefectureOrProvince())
                    .param("subPrefecture", row.subPrefecture())
                    .param("seats", row.seats())
                    .param("turnout", row.turnoutRate())
                    .query(Long.class)
                    .single();
            ids.put(row.constituencySourceId(), id);
        }
        return ids;
    }
}