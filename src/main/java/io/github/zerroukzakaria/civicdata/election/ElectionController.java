package io.github.zerroukzakaria.civicdata.election;

import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencyDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ConstituencySummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionResults;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionSummary;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.PagedResult;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.RegionSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    private final ResultsService service;
    private final ConstituencyService constituencyService;

    public ElectionController(ResultsService service, ConstituencyService constituencyService) {
        this.service = service;
        this.constituencyService = constituencyService;
    }

    @GetMapping
    public List<ElectionSummary> elections() {
        return service.listElections();
    }

    @GetMapping("/{slug}")
    public ElectionDetail election(@PathVariable String slug) {
        return service.electionDetail(slug);
    }

    @GetMapping("/{slug}/results")
    public ElectionResults results(@PathVariable String slug,
                                   @RequestParam(defaultValue = "LOCAL") Ballot ballot) {
        return service.nationalResults(slug, ballot);
    }

    @GetMapping("/{slug}/regions")
    public List<RegionSummary> regions(@PathVariable String slug) {
        return service.regions(slug);
    }

    @GetMapping("/{slug}/regions/{regionId}/results")
    public ElectionResults regionResults(@PathVariable String slug, @PathVariable long regionId,
                                         @RequestParam(defaultValue = "LOCAL") Ballot ballot) {
        return service.regionResults(slug, regionId, ballot);
    }

    @GetMapping("/{slug}/constituencies")
    public PagedResult<ConstituencySummary> constituencies(
            @PathVariable String slug,
            @RequestParam(required = false) Ballot ballot,
            @RequestParam(required = false) Long regionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return constituencyService.list(slug, ballot, regionId, page, size, sort, direction);
    }

    @GetMapping("/{slug}/constituencies/{id}")
    public ConstituencyDetail constituency(@PathVariable String slug, @PathVariable long id) {
        return constituencyService.detail(slug, id);
    }
}