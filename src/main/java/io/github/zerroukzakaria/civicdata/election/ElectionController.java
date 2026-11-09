package io.github.zerroukzakaria.civicdata.election;

import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencyDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencySummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionResults;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionSummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.PagedResult;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.RegionSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elections")
@Tag(name = "Elections", description = "Election results at national, regional and constituency level")
public class ElectionController {

    private static final String SLUG = "Election identifier, for example legislative-2021";

    private final ResultsService service;
    private final ConstituencyService constituencyService;

    public ElectionController(ResultsService service, ConstituencyService constituencyService) {
        this.service = service;
        this.constituencyService = constituencyService;
    }

    @GetMapping
    @Operation(summary = "List the imported elections")
    public List<ElectionSummary> elections() {
        return service.listElections();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Describe one election",
            description = "Includes the source, the licence and the citation required to reuse the data.")
    public ElectionDetail election(@Parameter(description = SLUG) @PathVariable String slug) {
        return service.electionDetail(slug);
    }

    @GetMapping("/{slug}/results")
    @Operation(summary = "National results for one ballot",
            description = "Votes and vote share per list, highest first.")
    public ElectionResults results(
            @Parameter(description = SLUG) @PathVariable String slug,
            @Parameter(description = "Which ballot to report. Shares are never mixed across ballots.")
            @RequestParam(defaultValue = "LOCAL") Ballot ballot) {
        return service.nationalResults(slug, ballot);
    }

    @GetMapping("/{slug}/regions")
    @Operation(summary = "List the regions",
            description = "Constituencies counts both local and regional constituencies.")
    public List<RegionSummary> regions(@Parameter(description = SLUG) @PathVariable String slug) {
        return service.regions(slug);
    }

    @GetMapping("/{slug}/regions/{regionId}/results")
    @Operation(summary = "Results inside one region for one ballot")
    public ElectionResults regionResults(
            @Parameter(description = SLUG) @PathVariable String slug,
            @Parameter(description = "Region id, from the regions endpoint") @PathVariable long regionId,
            @Parameter(description = "Which ballot to report") @RequestParam(defaultValue = "LOCAL") Ballot ballot) {
        return service.regionResults(slug, regionId, ballot);
    }

    @GetMapping("/{slug}/constituencies")
    @Operation(summary = "List constituencies, paged",
            description = "Returns one page of constituencies with their seats, turnout and total votes. "
                    + "Pages start at 0 and size is limited to 100.")
    public PagedResult<ConstituencySummary> constituencies(
            @Parameter(description = SLUG) @PathVariable String slug,
            @Parameter(description = "Only constituencies of this ballot. All of them when omitted.")
            @RequestParam(required = false) Ballot ballot,
            @Parameter(description = "Only constituencies in this region") @RequestParam(required = false) Long regionId,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 100") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort key: name, seats, votes or turnout")
            @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String direction) {
        return constituencyService.list(slug, ballot, regionId, page, size, sort, direction);
    }

    @GetMapping("/{slug}/constituencies/{id}")
    @Operation(summary = "One constituency with its results",
            description = "Votes and vote share per list inside this constituency, highest first.")
    public ConstituencyDetail constituency(
            @Parameter(description = SLUG) @PathVariable String slug,
            @Parameter(description = "Constituency id, from the constituencies endpoint") @PathVariable long id) {
        return constituencyService.detail(slug, id);
    }
}