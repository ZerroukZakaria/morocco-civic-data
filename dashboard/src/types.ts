export type Ballot = "LOCAL" | "REGIONAL";

export interface Attribution {
  sourceName: string;
  sourceUrl: string;
  licence: string;
  licenceUrl: string;
  citation: string;
}

export interface ElectionSummary {
  slug: string;
  name: string;
  kind: string;
  date: string;
}

export interface ElectionDetail extends ElectionSummary {
  notes: string;
  attribution: Attribution;
}

export interface RegionSummary {
  id: number;
  name: string;
  constituencies: number;
  seats: number;
}

export interface ListResult {
  code: string;
  name: string;
  kind: "PARTY" | "COALITION";
  votes: number;
  sharePercent: number;
}

export interface ElectionResults {
  election: string;
  scope: string;
  ballot: Ballot;
  totalVotes: number;
  basis: string;
  lists: ListResult[];
  attribution: Attribution;
}

export interface ConstituencySummary {
  id: number;
  sourceId: number;
  name: string;
  ballot: Ballot;
  regionId: number;
  region: string;
  seats: number;
  turnoutRate: number | null;
  totalVotes: number;
}

export interface ConstituencyDetail extends ConstituencySummary {
  wilaya: string | null;
  prefectureOrProvince: string | null;
  subPrefecture: string | null;
  basis: string;
  lists: ListResult[];
  attribution: Attribution;
}

export interface Snapshot {
  generatedAt: string;
  election: ElectionDetail;
  results: Record<Ballot, ElectionResults>;
  regions: RegionSummary[];
  regionResults: Record<string, Record<Ballot, ElectionResults>>;
  constituencies: ConstituencySummary[];
  constituencyDetails: Record<string, ConstituencyDetail>;
}