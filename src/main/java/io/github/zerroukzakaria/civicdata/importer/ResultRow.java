package io.github.zerroukzakaria.civicdata.importer;

/** One line of results.tsv: the votes one list received in one constituency. */
public record ResultRow(
        int constituencySourceId,
        String constituencyName,
        String kind,
        String region,
        String wilaya,
        String prefectureOrProvince,
        String subPrefecture,
        int seats,
        Double turnoutRate,
        String partyCode,
        String partyName,
        String partyKind,
        long votes) {

    /** The constituency-level fields, which must be identical on every row of one constituency. */
    public record Constituency(
            String name,
            String kind,
            String region,
            String wilaya,
            String prefectureOrProvince,
            String subPrefecture,
            int seats,
            Double turnoutRate) {
    }

    public Constituency constituency() {
        return new Constituency(
                constituencyName, kind, region, wilaya, prefectureOrProvince, subPrefecture, seats, turnoutRate);
    }
}