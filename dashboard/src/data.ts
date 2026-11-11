import type { ElectionSummary, Snapshot } from "./types";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${import.meta.env.BASE_URL}data/${path}`);
  const hint = `Could not load ${path}. Run "npm run export" while the API is running.`;
  if (!response.ok) {
    throw new Error(hint);
  }
  try {
    return (await response.json()) as T;
  } catch {
    // The dev server answers unknown paths with the page itself, which is not JSON
    throw new Error(hint);
  }
}

export const loadElections = () => getJson<ElectionSummary[]>("elections.json");
export const loadSnapshot = (slug: string) => getJson<Snapshot>(`${slug}.json`);