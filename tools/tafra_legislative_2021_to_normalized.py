#!/usr/bin/env python3
"""
Convert Tafra's wide workbook of the 2021 Moroccan legislative results into this
project's normalized import format (election.properties + results.tsv).

Usage:
    python tools/tafra_legislative_2021_to_normalized.py <workbook.xlsx> <output-dir>

Source: "Résultats des élections législatives marocaines de 2021, listes locales et
régionales", Association Tafra, Rabat, 2022 (CC BY 4.0).

The script refuses to write anything if a sanity check fails.
Requires: pandas, openpyxl.
"""
import sys
from pathlib import Path

import pandas as pd

PREFIX = "Nombre de voix obtenu par "
EXPECTED_SEATS = 395          # 305 local + 90 regional
TOTALS_TOLERANCE = 0.01       # local vs regional vote totals must agree within 1%

CONSTITUENCY_COLUMNS = {
    "idCirconscription": "constituency_source_id",
    "circonscription": "constituency_name",
    "typeListe": "kind",
    "region": "region",
    "wilaya": "wilaya",
    "prefProv": "prefecture_or_province",
    "sousPref": "sub_prefecture",
    "nSieges": "seats",
    "txParticipation": "turnout_rate",
}
KINDS = {"locale": "LOCAL", "regionale": "REGIONAL"}

TSV_HEADER = [
    "constituency_source_id", "constituency_name", "kind", "region", "wilaya",
    "prefecture_or_province", "sub_prefecture", "seats", "turnout_rate",
    "party_code", "party_name", "party_kind", "votes",
]


def fail(message: str) -> None:
    sys.exit(f"ERROR: {message}")


def clean(value) -> str:
    """Empty string for missing values; trimmed text otherwise."""
    if pd.isna(value):
        return ""
    return str(value).strip()


def party_catalogue(dictionary: pd.DataFrame) -> dict:
    """party code -> (name, kind), read from the workbook's own dictionary sheet."""
    catalogue = {}
    for _, row in dictionary.iterrows():
        code, definition = clean(row.iloc[0]), clean(row.iloc[1])
        if not definition.startswith(PREFIX):
            continue
        name = definition[len(PREFIX):]
        kind = "PARTY"
        if name.startswith("la coalition "):
            kind = "COALITION"
            name = name[len("la coalition "):].split(" comprenant ")[0]
        catalogue[code] = (name.strip(), kind)
    return catalogue


def main(xlsx: Path, out_dir: Path) -> None:
    data = pd.read_excel(xlsx, sheet_name="donnees")
    dictionary = pd.read_excel(xlsx, sheet_name="dictionnaire")
    catalogue = party_catalogue(dictionary)

    missing = [c for c in CONSTITUENCY_COLUMNS if c not in data.columns]
    if missing:
        fail(f"missing columns in sheet 'donnees': {missing}")
    party_columns = [c for c in data.columns if c in catalogue]
    unknown = [c for c in data.columns[25:] if c not in catalogue]
    if unknown:
        fail(f"columns without a dictionary entry: {unknown}")

    if data["idCirconscription"].duplicated().any():
        fail("duplicate idCirconscription")
    if data["typeListe"].map(lambda v: v in KINDS).eq(False).any():
        fail("unexpected typeListe value")
    if int(data["nSieges"].sum()) != EXPECTED_SEATS:
        fail(f"seats add up to {int(data['nSieges'].sum())}, expected {EXPECTED_SEATS}")
    if not data["txParticipation"].between(0, 1).all():
        fail("turnout rate outside 0..1")

    rows = []
    totals = {"LOCAL": 0, "REGIONAL": 0}
    for _, r in data.iterrows():
        kind = KINDS[r["typeListe"]]
        constituency = [
            str(int(r["idCirconscription"])), clean(r["circonscription"]), kind,
            clean(r["region"]), clean(r["wilaya"]), clean(r["prefProv"]), clean(r["sousPref"]),
            str(int(r["nSieges"])), f"{float(r['txParticipation']):.4f}",
        ]
        for code in party_columns:
            value = r[code]
            if pd.isna(value):
                continue                     # no list in this constituency (not zero votes)
            if value < 0 or value != int(value):
                fail(f"{code} in {r['circonscription']}: invalid vote count {value}")
            name, party_kind = catalogue[code]
            totals[kind] += int(value)
            rows.append(constituency + [code, name, party_kind, str(int(value))])

    gap = abs(totals["LOCAL"] - totals["REGIONAL"]) / max(totals.values())
    if gap > TOTALS_TOLERANCE:
        fail(f"local ({totals['LOCAL']}) and regional ({totals['REGIONAL']}) totals differ by {gap:.2%}")

    for row in rows:
        for field in row:
            if "\t" in field or "\n" in field or "\r" in field:
                fail(f"tab or newline inside a field: {field!r}")

    out_dir.mkdir(parents=True, exist_ok=True)
    with open(out_dir / "results.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("\t".join(TSV_HEADER) + "\n")
        for row in rows:
            f.write("\t".join(row) + "\n")

    properties = {
        "slug": "legislative-2021",
        "name": "Moroccan legislative elections 2021",
        "kind": "LEGISLATIVE",
        "date": "2021-09-08",
        "expected.seats": str(EXPECTED_SEATS),
        "source.name": "Association Tafra, from elections.ma",
        "source.url": "https://open.africa/dataset/elections-legislatives-maroc-2021",
        "licence": "CC BY 4.0",
        "licence.url": "https://creativecommons.org/licenses/by/4.0/",
        "citation": "\"Résultats des élections législatives marocaines de 2021, listes locales et régionales\" Association Tafra. Rabat. 2022.",
        "notes": ("Registered voters and spoilt votes are not available in the source and are not imported. "
                  "Seats won per party are not in the source. "
                  "AFG is a coalition (including PSU, which also appears as its own list)."),
    }
    with open(out_dir / "election.properties", "w", encoding="utf-8", newline="\n") as f:
        for key, value in properties.items():
            f.write(f"{key}={value}\n")

    print(f"constituencies : {len(data)} ({(data['typeListe'] == 'locale').sum()} local, {(data['typeListe'] == 'regionale').sum()} regional)")
    print(f"seats          : {int(data['nSieges'].sum())}")
    print(f"lists          : {len(party_columns)}")
    print(f"result rows    : {len(rows)}")
    print(f"votes          : local {totals['LOCAL']}, regional {totals['REGIONAL']} (gap {gap:.2%})")
    print(f"written to     : {out_dir}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(Path(sys.argv[1]), Path(sys.argv[2]))
