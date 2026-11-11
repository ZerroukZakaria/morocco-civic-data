// Calls the running API and saves everything the dashboard needs as static JSON files.
// Usage: start the API (.\mvnw.cmd spring-boot:run), then run "npm run export" in this folder.
import { mkdir, writeFile } from "node:fs/promises";

const BASE = process.env.API_BASE ?? "http://localhost:8080";
const OUT = new URL("../public/data/", import.meta.url);
const BALLOTS = ["LOCAL", "REGIONAL"];

async function get(path) {
  const response = await fetch(BASE + path);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText} for ${path}`);
  }
  return response.json();
}

async function exportElection(slug) {
  const base = `/api/elections/${slug}`;
  const election = await get(base);
  const regions = await get(`${base}/regions`);

  const results = {};
  for (const ballot of BALLOTS) {
    results[ballot] = await get(`${base}/results?ballot=${ballot}`);
  }

  const regionResults = {};
  for (const region of regions) {
    regionResults[region.id] = {};
    for (const ballot of BALLOTS) {
      regionResults[region.id][ballot] = await get(`${base}/regions/${region.id}/results?ballot=${ballot}`);
    }
  }

  // The API caps a page at 100 items, so walk through all pages
  const constituencies = [];
  for (let page = 0; ; page++) {
    const result = await get(`${base}/constituencies?size=100&page=${page}`);
    constituencies.push(...result.items);
    if (page + 1 >= result.totalPages) break;
  }

  const constituencyDetails = {};
  for (const c of constituencies) {
    constituencyDetails[c.id] = await get(`${base}/constituencies/${c.id}`);
  }

  return {
    generatedAt: new Date().toISOString(),
    election,
    results,
    regions,
    regionResults,
    constituencies,
    constituencyDetails,
  };
}

const elections = await get("/api/elections");
await mkdir(OUT, { recursive: true });
await writeFile(new URL("elections.json", OUT), JSON.stringify(elections));

for (const election of elections) {
  const snapshot = await exportElection(election.slug);
  const file = `${election.slug}.json`;
  await writeFile(new URL(file, OUT), JSON.stringify(snapshot));
  console.log(
    `${file}: ${snapshot.regions.length} regions, ${snapshot.constituencies.length} constituencies, ` +
      `${Object.keys(snapshot.constituencyDetails).length} details`,
  );
}