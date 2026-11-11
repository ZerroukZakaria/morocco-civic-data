import { useEffect, useState } from "react";
import { AboutData } from "./AboutData";
import { ConstituencyDialog } from "./ConstituencyDialog";
import { ConstituencyTable } from "./ConstituencyTable";
import { loadElections, loadSnapshot } from "./data";
import { formatDate } from "./format";
import { ResultsPanel } from "./ResultsPanel";
import type { Ballot, ElectionSummary, Snapshot } from "./types";

export default function App() {
  const [elections, setElections] = useState<ElectionSummary[]>([]);
  const [slug, setSlug] = useState<string | null>(null);
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [ballot, setBallot] = useState<Ballot>("LOCAL");
  const [regionId, setRegionId] = useState(""); // an empty string means national
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    loadElections()
      .then((list) => {
        setElections(list);
        setSlug(list[0]?.slug ?? null);
      })
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!slug) return;
    setSnapshot(null);
    setRegionId("");
    setSelectedId(null);
    loadSnapshot(slug)
      .then(setSnapshot)
      .catch((e: Error) => setError(e.message));
  }, [slug]);

  const results = snapshot
    ? regionId
      ? snapshot.regionResults[regionId][ballot]
      : snapshot.results[ballot]
    : null;

  const rows = snapshot
    ? snapshot.constituencies.filter(
        (c) => c.ballot === ballot && (regionId === "" || String(c.regionId) === regionId),
      )
    : [];

  const selected =
    snapshot && selectedId !== null ? (snapshot.constituencyDetails[String(selectedId)] ?? null) : null;

  return (
    <div className="page">
      <header>
        <h1>Morocco Civic Data</h1>
        <p className="muted">Legislative election results by region and constituency</p>
      </header>

      {error && <p role="alert" className="error">{error}</p>}
      {!error && !snapshot && <p className="muted">Loading…</p>}

      {snapshot && results && (
        <main>
          <div className="controls">
            <div className="field">
              <label htmlFor="election">Election</label>
              <select id="election" value={slug ?? ""} onChange={(e) => setSlug(e.target.value)}>
                {elections.map((e) => (
                  <option key={e.slug} value={e.slug}>
                    {e.name} ({formatDate(e.date)})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span className="label" id="ballot-label">Ballot</span>
              <div className="toggle" role="group" aria-labelledby="ballot-label">
                <button type="button" aria-pressed={ballot === "LOCAL"} onClick={() => setBallot("LOCAL")}>
                  Local
                </button>
                <button type="button" aria-pressed={ballot === "REGIONAL"} onClick={() => setBallot("REGIONAL")}>
                  Regional
                </button>
              </div>
            </div>

            <div className="field">
              <label htmlFor="region">Region</label>
              <select id="region" value={regionId} onChange={(e) => setRegionId(e.target.value)}>
                <option value="">All regions (national)</option>
                {snapshot.regions.map((r) => (
                  <option key={r.id} value={String(r.id)}>
                    {r.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <ResultsPanel results={results} />
          <ConstituencyTable key={`${slug}-${ballot}-${regionId}`} rows={rows} onSelect={setSelectedId} />
          <AboutData snapshot={snapshot} basis={results.basis} />
        </main>
      )}

      <ConstituencyDialog detail={selected} onClose={() => setSelectedId(null)} />
    </div>
  );
}