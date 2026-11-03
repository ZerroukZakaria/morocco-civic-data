package io.github.zerroukzakaria.civicdata.importer;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Imports an election at startup, but only when civic.import.dir is set. */
@Component
@ConditionalOnProperty(name = "civic.import.dir")
public class ImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);

    private final ElectionImporter importer;
    private final String directory;

    public ImportRunner(ElectionImporter importer, @Value("${civic.import.dir}") String directory) {
        this.importer = importer;
        this.directory = directory;
    }

    @Override
    public void run(String... args) {
        ElectionImporter.ImportSummary summary = importer.importDirectory(Path.of(directory));
        log.info("Imported election {}: {} constituencies, {} lists, {} results, {} votes",
                summary.electionId(), summary.constituencies(), summary.lists(),
                summary.results(), summary.totalVotes());
    }
}