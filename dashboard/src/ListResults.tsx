import { formatNumber, formatPercent } from "./format";
import type { ListResult } from "./types";

export function ListResults({ lists, label }: { lists: ListResult[]; label: string }) {
  const top = lists.slice(0, 10);
  const max = top[0]?.sharePercent || 1;

  return (
    <>
      <ol className="bars" aria-label={`${label}: the ten lists with the most votes`}>
        {top.map((list) => (
          <li key={list.code}>
            <span className="bar-label">
              {list.name} <span className="chip">{list.code}</span>
            </span>
            <span className="bar-track" aria-hidden="true">
              <span className="bar-fill" style={{ width: `${(list.sharePercent / max) * 100}%` }} />
            </span>
            <span className="bar-value">{formatPercent(list.sharePercent)}</span>
          </li>
        ))}
      </ol>

      <details>
        <summary>All {lists.length} lists</summary>
        <table>
          <caption className="sr-only">Votes and vote share for every list, highest first</caption>
          <thead>
            <tr>
              <th scope="col">List</th>
              <th scope="col">Votes</th>
              <th scope="col">Share</th>
            </tr>
          </thead>
          <tbody>
            {lists.map((list) => (
              <tr key={list.code}>
                <th scope="row">
                  {list.name} <span className="chip">{list.code}</span>
                  {list.kind === "COALITION" && <span className="chip">coalition</span>}
                </th>
                <td>{formatNumber(list.votes)}</td>
                <td>{formatPercent(list.sharePercent)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </>
  );
}