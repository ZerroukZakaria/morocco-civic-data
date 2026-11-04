package io.github.zerroukzakaria.civicdata.election;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "election")
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;
    private String name;
    private String kind;

    @Column(name = "election_date")
    private LocalDate electionDate;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_url")
    private String sourceUrl;

    private String licence;

    @Column(name = "licence_url")
    private String licenceUrl;

    private String citation;
    private String notes;

    /** Required by JPA. Rows are created by the importer, not through this class. */
    protected Election() {
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getKind() { return kind; }
    public LocalDate getElectionDate() { return electionDate; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getLicence() { return licence; }
    public String getLicenceUrl() { return licenceUrl; }
    public String getCitation() { return citation; }
    public String getNotes() { return notes; }
}