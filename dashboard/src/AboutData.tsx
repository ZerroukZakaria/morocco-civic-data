import { formatDate } from "./format";
import type { Snapshot } from "./types";

const REPOSITORY = "https://github.com/ZerroukZakaria/morocco-civic-data";

export function AboutData({ snapshot, basis }: { snapshot: Snapshot; basis: string }) {
  const { election, generatedAt } = snapshot;
  const a = election.attribution;
  return (
    <footer className="about">
      <h2>About this data</h2>
      <p>{basis}</p>
      <p>{election.notes}</p>
      <p>Turnout is the rate reported by the source, shown as a percentage.</p>
      <p>
        Source: <a href={a.sourceUrl} rel="noopener">{a.sourceName}</a>. {a.citation} Licence:{" "}
        <a href={a.licenceUrl} rel="noopener">{a.licence}</a>.
      </p>
      <p>
        This page shows a static export of the API's responses, generated {formatDate(generatedAt)}. The code,
        the live API and the Docker setup are in the{" "}
        <a href={REPOSITORY} rel="noopener">repository</a>.
      </p>
    </footer>
  );
}