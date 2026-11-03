CREATE TABLE election (
    id            BIGSERIAL PRIMARY KEY,
    slug          TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    kind          TEXT NOT NULL CHECK (kind IN ('LEGISLATIVE')),
    election_date DATE NOT NULL,
    source_name   TEXT NOT NULL,
    source_url    TEXT NOT NULL,
    licence       TEXT NOT NULL,
    licence_url   TEXT NOT NULL,
    citation      TEXT NOT NULL,
    notes         TEXT NOT NULL DEFAULT ''
);

CREATE TABLE region (
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- A "party" is any list that can receive votes: a party or a coalition.
CREATE TABLE party (
    id   BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('PARTY', 'COALITION'))
);

CREATE TABLE constituency (
    id                     BIGSERIAL PRIMARY KEY,
    election_id            BIGINT  NOT NULL REFERENCES election (id) ON DELETE CASCADE,
    source_id              INTEGER NOT NULL,
    name                   TEXT    NOT NULL,
    kind                   TEXT    NOT NULL CHECK (kind IN ('LOCAL', 'REGIONAL')),
    region_id              BIGINT  NOT NULL REFERENCES region (id),
    wilaya                 TEXT,
    prefecture_or_province TEXT,
    sub_prefecture         TEXT,
    seats                  INTEGER NOT NULL CHECK (seats > 0),
    turnout_rate           NUMERIC(5, 4) CHECK (turnout_rate BETWEEN 0 AND 1),
    UNIQUE (election_id, source_id)
);
CREATE INDEX constituency_region ON constituency (election_id, region_id);

-- No row means the party had no list in that constituency (which is not the same as zero votes).
CREATE TABLE result (
    constituency_id BIGINT NOT NULL REFERENCES constituency (id) ON DELETE CASCADE,
    party_id        BIGINT NOT NULL REFERENCES party (id),
    votes           BIGINT NOT NULL CHECK (votes >= 0),
    PRIMARY KEY (constituency_id, party_id)
);
CREATE INDEX result_party ON result (party_id);