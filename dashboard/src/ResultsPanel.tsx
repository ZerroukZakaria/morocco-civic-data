import { ballotLabel, formatNumber } from "./format";
import { ListResults } from "./ListResults";
import type { ElectionResults } from "./types";

export function ResultsPanel({ results }: { results: ElectionResults }) {
  return (
    <section className="card" aria-labelledby="results-title">
      <h2 id="results-title">{results.scope === "national" ? "National results" : results.scope}</h2>
      <p className="muted">
        {ballotLabel(results.ballot)} · {formatNumber(results.totalVotes)} votes cast for lists
      </p>
      <ListResults lists={results.lists} label={results.scope} />
    </section>
  );
}