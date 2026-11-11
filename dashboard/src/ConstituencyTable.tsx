import { useMemo, useState } from "react";
import { formatNumber, formatTurnout } from "./format";
import type { ConstituencySummary } from "./types";

type SortKey = "name" | "seats" | "turnoutRate" | "totalVotes";

const PAGE_SIZE = 20;
const collator = new Intl.Collator("fr", { sensitivity: "base", numeric: true });
const plain = (s: string) => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

export function ConstituencyTable({
  rows,
  onSelect,
}: {
  rows: ConstituencySummary[];
  onSelect: (id: number) => void;
}) {
  const [query, setQuery] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("name");
  const [ascending, setAscending] = useState(true);
  const [page, setPage] = useState(0);

  const visible = useMemo(() => {
    const q = plain(query.trim());
    const filtered = q ? rows.filter((r) => plain(r.name).includes(q) || plain(r.region).includes(q)) : rows;
    const sign = ascending ? 1 : -1;
    return [...filtered].sort((a, b) => {
      if (sortKey === "name") return sign * collator.compare(a.name, b.name);
      const x = a[sortKey];
      const y = b[sortKey];
      if (x === null && y === null) return 0;
      if (x === null) return 1; // missing values always last
      if (y === null) return -1;
      return sign * (x - y) || collator.compare(a.name, b.name);
    });
  }, [rows, query, sortKey, ascending]);

  const pages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(page, pages - 1);
  const shown = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);

  const toggle = (key: SortKey) => {
    if (key === sortKey) {
      setAscending(!ascending);
    } else {
      setSortKey(key);
      setAscending(key === "name"); // numbers start with the largest
    }
    setPage(0);
  };

  const header = (key: SortKey, label: string) => (
    <th scope="col" aria-sort={sortKey === key ? (ascending ? "ascending" : "descending") : undefined}>
      <button type="button" className="sort" onClick={() => toggle(key)}>
        {label}
        <span aria-hidden="true">{sortKey === key ? (ascending ? " ▲" : " ▼") : ""}</span>
      </button>
    </th>
  );

  return (
    <section className="card" aria-labelledby="constituencies-title">
      <h2 id="constituencies-title">Constituencies</h2>
      <p className="muted">
        {visible.length} of {rows.length} shown
      </p>

      <div className="toolbar">
        <label className="field">
          <span className="label">Search by constituency or region</span>
          <input
            type="search"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
          />
        </label>
      </div>

      <table>
        <caption className="sr-only">Constituencies with their seats, turnout and total votes</caption>
        <thead>
          <tr>
            {header("name", "Constituency")}
            <th scope="col" className="left">Region</th>
            {header("seats", "Seats")}
            {header("turnoutRate", "Turnout")}
            {header("totalVotes", "Votes")}
          </tr>
        </thead>
        <tbody>
          {shown.map((r) => (
            <tr key={r.id}>
              <th scope="row">
                <button type="button" className="link" onClick={() => onSelect(r.id)}>
                  {r.name}
                </button>
              </th>
              <td className="left">{r.region}</td>
              <td>{r.seats}</td>
              <td>{formatTurnout(r.turnoutRate)}</td>
              <td>{formatNumber(r.totalVotes)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {visible.length === 0 && <p className="muted">No constituency matches your search.</p>}

      <nav className="pager" aria-label="Constituency pages">
        <button type="button" disabled={current === 0} onClick={() => setPage(current - 1)}>
          Previous
        </button>
        <span aria-live="polite">
          Page {current + 1} of {pages}
        </span>
        <button type="button" disabled={current >= pages - 1} onClick={() => setPage(current + 1)}>
          Next
        </button>
      </nav>
    </section>
  );
}