package io.github.zerroukzakaria.civicdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zerroukzakaria.civicdata.election.Ballot;
import io.github.zerroukzakaria.civicdata.election.ElectionRepository;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionResults;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ListResult;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.RegionSummary;
import io.github.zerroukzakaria.civicdata.election.ResultsService;
import io.github.zerroukzakaria.civicdata.importer.ElectionImporter;
import io.github.zerroukzakaria.civicdata.importer.ImportException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs the migration, the importer and the queries against a real PostgreSQL.
 * Expected numbers were verified independently from the source workbook.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ElectionDatabaseIntegrationTest {

    private static final String SLUG = "legislative-2021";
    private static final Path DIR = Path.of("data", "normalized", "legislative-2021");

    @Autowired
    ElectionImporter importer;

    @Autowired
    ResultsService results;

    @Autowired
    ElectionRepository elections;

    @BeforeEach
    void importTheElectionOnce() {
        if (elections.findBySlug(SLUG).isEmpty()) {
            importer.importDirectory(DIR);
        }
    }

    @Test
    void theImportedElectionHasTheStructureOfTheSource() {
        List<RegionSummary> regions = results.regions(SLUG);

        assertThat(regions).hasSize(12);
        assertThat(regions.stream().mapToInt(RegionSummary::constituencies).sum()).isEqualTo(104);
        assertThat(regions.stream().mapToInt(RegionSummary::seats).sum()).isEqualTo(395);
    }

    @Test
    void nationalResultsMatchTheSourceTotals() {
        ElectionResults local = results.nationalResults(SLUG, Ballot.LOCAL);
        assertThat(local.totalVotes()).isEqualTo(7_588_505L);
        assertThat(local.lists()).hasSize(30);
        ListResult first = local.lists().get(0);
        assertThat(first.code()).isEqualTo("RNI");
        assertThat(first.votes()).isEqualTo(2_099_036L);
        assertThat(first.sharePercent()).isEqualByComparingTo("27.66");
        assertThat(local.attribution().licence()).isEqualTo("CC BY 4.0");

        ElectionResults regional = results.nationalResults(SLUG, Ballot.REGIONAL);
        assertThat(regional.totalVotes()).isEqualTo(7_571_623L);
        assertThat(regional.lists()).hasSize(28);
    }

    @Test
    void regionalBreakdownsAddUpToTheNationalTotal() {
        List<RegionSummary> regions = results.regions(SLUG);

        long sum = regions.stream()
                .mapToLong(r -> results.regionResults(SLUG, r.id(), Ballot.LOCAL).totalVotes())
                .sum();
        assertThat(sum).isEqualTo(7_588_505L);

        RegionSummary casablanca = regions.stream()
                .filter(r -> r.name().equals("Casablanca - Settat"))
                .findFirst()
                .orElseThrow();
        assertThat(casablanca.constituencies()).isEqualTo(17);
        assertThat(casablanca.seats()).isEqualTo(69);
        ElectionResults inRegion = results.regionResults(SLUG, casablanca.id(), Ballot.LOCAL);
        assertThat(inRegion.scope()).isEqualTo("Casablanca - Settat");
        assertThat(inRegion.totalVotes()).isEqualTo(1_168_195L);
        assertThat(inRegion.lists()).hasSize(29);
    }

    @Test
    void unknownElectionsAndRegionsAreNotFound() {
        assertThatThrownBy(() -> results.nationalResults("nope", Ballot.LOCAL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThatThrownBy(() -> results.regionResults(SLUG, 999_999L, Ballot.LOCAL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void importingTheSameElectionTwiceIsRefused() {
        assertThatThrownBy(() -> importer.importDirectory(DIR))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("already imported");
    }

    @Test
    void aFailedImportLeavesNothingBehind(@TempDir Path tmp) throws IOException {
        // Same data under another slug, but one list code is given a different name than it already has
        String properties = Files.readString(DIR.resolve("election.properties"))
                .replace("slug=legislative-2021", "slug=test-conflict");
        String resultsFile = Files.readString(DIR.resolve("results.tsv"))
                .replace("Parti Authenticité et Modernité", "A different name");
        Files.writeString(tmp.resolve("election.properties"), properties);
        Files.writeString(tmp.resolve("results.tsv"), resultsFile);

        assertThatThrownBy(() -> importer.importDirectory(tmp))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("PAM");

        // The election row was inserted before the failure, so this proves the rollback worked
        assertThat(elections.findBySlug("test-conflict")).isEmpty();
    }
}