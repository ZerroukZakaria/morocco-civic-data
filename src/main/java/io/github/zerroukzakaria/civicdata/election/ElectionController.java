package io.github.zerroukzakaria.civicdata.election;

import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionDetail;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionResults;
import io.github.zerroukzakaria.civicdata.election.ElectionViews.ElectionSummary;
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

    public ElectionController(ResultsService service) {
        this.service = service;
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
}