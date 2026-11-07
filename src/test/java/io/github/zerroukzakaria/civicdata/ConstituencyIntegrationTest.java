package io.github.zerroukzakaria.civicdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zerroukzakaria.civicdata.election.Ballot;
import io.github.zerroukzakaria.civicdata.election.ConstituencyService;
import io.github.zerroukzakaria.civicdata.election.ElectionRepository;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencyDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencySummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.PagedResult;
import io.github.zerroukzakaria.civicdata.election.ResultsService;
import io.github.zerroukzakaria.civicdata.importer.ElectionImporter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

/** Expected numbers were computed independently from the source workbook. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConstituencyIntegrationTest {

    private static final String SLUG = "legislative-2021";
    private static final Path DIR = Path.of("data", "normalized", "legislative-2021");

    @Autowired
    ElectionImporter importer;

    @Autowired
    ConstituencyService constituencies;

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

    private long casablancaId() {
        return results.regions(SLUG).stream()
                .filter(r -> r.name().equals("Casablanca - Settat"))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private static void assertStatus(ThrowingCallable call, int status) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }

    @Test
    void listsConstituenciesInPages() {
        PagedResult<ConstituencySummary> first = constituencies.list(SLUG, null, null, 0, 20, "name", "asc");
        assertThat(first.totalItems()).isEqualTo(104);
        assertThat(first.totalPages()).isEqualTo(6);
        assertThat(first.items()).hasSize(20);

        assertThat(constituencies.list(SLUG, null, null, 5, 20, "name", "asc").items()).hasSize(4);

        PagedResult<ConstituencySummary> beyond = constituencies.list(SLUG, null, null, 6, 20, "name", "asc");
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.totalItems()).isEqualTo(104);
    }

    @Test
    void filtersByBallot() {
        PagedResult<ConstituencySummary> local = constituencies.list(SLUG, Ballot.LOCAL, null, 0, 20, "name", "asc");
        assertThat(local.totalItems()).isEqualTo(92);
        assertThat(local.totalPages()).isEqualTo(5);
        assertThat(constituencies.list(SLUG, Ballot.LOCAL, null, 4, 20, "name", "asc").items()).hasSize(12);

        assertThat(constituencies.list(SLUG, Ballot.REGIONAL, null, 0, 20, "name", "asc").totalItems())
                .isEqualTo(12);
    }

    @Test
    void filtersByRegion() {
        long regionId = casablancaId();
        assertThat(constituencies.list(SLUG, null, regionId, 0, 50, "name", "asc").totalItems()).isEqualTo(17);
        assertThat(constituencies.list(SLUG, Ballot.LOCAL, regionId, 0, 50, "name", "asc").totalItems())
                .isEqualTo(16);
        assertThat(constituencies.list(SLUG, Ballot.REGIONAL, regionId, 0, 50, "name", "asc").totalItems())
                .isEqualTo(1);
    }

    @Test
    void sortsBySeatsAndVotes() {
        ConstituencySummary mostSeats = constituencies.list(SLUG, null, null, 0, 5, "seats", "desc").items().get(0);
        assertThat(mostSeats.name()).isEqualTo("Circonscription régionale : Casablanca - Settat");
        assertThat(mostSeats.seats()).isEqualTo(12);

        ConstituencySummary mostVotes = constituencies.list(SLUG, null, null, 0, 5, "votes", "desc").items().get(0);
        assertThat(mostVotes.totalVotes()).isEqualTo(1_172_619L);
    }

    @Test
    void everyConstituencyAppearsExactlyOnceAcrossPages() {
        Set<Long> ids = new HashSet<>();
        long votes = 0;
        for (int page = 0; page < 6; page++) {
            for (ConstituencySummary c : constituencies.list(SLUG, null, null, page, 20, "name", "asc").items()) {
                assertThat(ids.add(c.id())).as("constituency %s was repeated", c.name()).isTrue();
                votes += c.totalVotes();
            }
        }
        assertThat(ids).hasSize(104);
        assertThat(votes).isEqualTo(15_160_128L);
    }

    @Test
    void detailShowsResultsAndSharesInsideTheConstituency() {
        ConstituencySummary regional =
                constituencies.list(SLUG, Ballot.REGIONAL, casablancaId(), 0, 10, "name", "asc").items().get(0);

        ConstituencyDetail detail = constituencies.detail(SLUG, regional.id());

        assertThat(detail.name()).isEqualTo("Circonscription régionale : Casablanca - Settat");
        assertThat(detail.ballot()).isEqualTo(Ballot.REGIONAL);
        assertThat(detail.seats()).isEqualTo(12);
        assertThat(detail.turnoutRate()).isEqualByComparingTo("0.4222");
        assertThat(detail.totalVotes()).isEqualTo(1_172_619L);
        assertThat(detail.lists()).hasSize(24);
        assertThat(detail.lists().get(0).code()).isEqualTo("RNI");
        assertThat(detail.lists().get(0).votes()).isEqualTo(318_033L);
        assertThat(detail.lists().get(0).sharePercent()).isEqualByComparingTo("27.12");
        assertThat(detail.attribution().licence()).isEqualTo("CC BY 4.0");
    }

    @Test
    void invalidRequestsAreRejected() {
        assertStatus(() -> constituencies.list(SLUG, null, null, 0, 0, "name", "asc"), 400);
        assertStatus(() -> constituencies.list(SLUG, null, null, 0, 101, "name", "asc"), 400);
        assertStatus(() -> constituencies.list(SLUG, null, null, -1, 20, "name", "asc"), 400);
        assertStatus(() -> constituencies.list(SLUG, null, null, 0, 20, "bogus", "asc"), 400);
        assertStatus(() -> constituencies.list(SLUG, null, null, 0, 20, "name", "sideways"), 400);
        assertStatus(() -> constituencies.list("nope", null, null, 0, 20, "name", "asc"), 404);
        assertStatus(() -> constituencies.detail(SLUG, 999_999L), 404);
    }
}