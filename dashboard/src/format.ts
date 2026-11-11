import type { Ballot } from "./types";

const numbers = new Intl.NumberFormat("en-US");

export const formatNumber = (value: number): string => numbers.format(value);

export const formatPercent = (value: number): string => `${value.toFixed(2)}%`;

/** Dates are shown in UTC so a date such as 2021-09-08 never shifts by a day. */
export const formatDate = (iso: string): string =>
  new Intl.DateTimeFormat("en-GB", { dateStyle: "long", timeZone: "UTC" }).format(new Date(iso));

export const ballotLabel = (ballot: Ballot): string =>
  ballot === "LOCAL" ? "Local constituency ballot" : "Regional list ballot";

export const formatTurnout = (rate: number | null): string =>
  rate === null ? "n/a" : `${(rate * 100).toFixed(2)}%`;