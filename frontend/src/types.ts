export interface CountryInfo {
  id: string;
  name: string;
  region: string;
  latitude: number;
  longitude: number;
  historical: boolean;
}
export interface Metric {
  value: number;
  year: number;
  indicator: string;
}
export interface Country {
  info: CountryInfo;
  metrics: Record<string, Metric>;
  fetchedAt: string;
}
export interface HistoryRow {
  country: string;
  sourceName: string;
  year: number;
  best: number;
  low: number;
  high: number;
  stateBased: number;
  nonState: number;
  oneSided: number;
}
export interface CandidateRow {
  country: string;
  month: string;
  events: number;
  best: number;
  low: number;
  high: number;
  stateBased: number;
  nonState: number;
  oneSided: number;
}
export interface Aggregate {
  country: string;
  best: number;
  low: number;
  high: number;
  events: number | null;
  stateBased: number;
  nonState: number;
  oneSided: number;
}
export interface Manifest {
  fetchedAt: string;
  historyVersion: string;
  historyStart: number;
  historyEnd: number;
  candidateVersion: string;
  candidateStart: string;
  candidateEnd: string;
  historyRows: number;
  candidateEvents: number;
  countryCount: number;
  qualityIssues: {
    country: string;
    period: string;
    best: number;
    low: number;
    high: number;
    type: string;
  }[];
  sources: { id: string; url: string; sha256: string }[];
}
export interface Catalog {
  countries: CountryInfo[];
  resources: {
    fetchedAt: string;
    countries: Record<string, Record<string, Metric>>;
  };
  manifest: Manifest;
}
export interface NewsItem {
  title: string;
  url: string;
  publishedAt: string;
  source: string;
}
export interface NewsSnapshot {
  fetchedAt: string;
  items: NewsItem[];
  stale: boolean;
  lastError: string;
}
export type Terrain = 'OPEN' | 'MOUNTAIN' | 'URBAN' | 'DESERT' | 'COASTAL';
export interface Scenario {
  name: string;
  countryA: string;
  countryB: string;
  terrain: Terrain;
  days: number;
  trials: number;
  seed: number;
  readinessA: number;
  readinessB: number;
  supplyA: number;
  supplyB: number;
  diplomacy: number;
  climateStress: number;
}
export interface Outcomes {
  advantageA: number;
  advantageB: number;
  prolonged: number;
  deescalation: number;
}
export interface Band {
  p10: number;
  median: number;
  p90: number;
}
export interface TimelinePoint {
  day: number;
  sideA: Band;
  sideB: Band;
  strain: Band;
}
export interface SimulationResult {
  modelVersion: string;
  generatedAt: string;
  request: Scenario;
  inputs: {
    sideA: Country;
    sideB: Country;
    historicalVariabilityA: number;
    historicalVariabilityB: number;
    conflictVersion: string;
    resourceFetchedAt: string;
  };
  outcomes: Outcomes;
  timeline: TimelinePoint[];
  factors: { name: string; sideA: number; sideB: number; basis: string }[];
  notes: string[];
}
export interface SavedRun {
  id: string;
  createdAt: string;
  name: string;
  result: SimulationResult;
}
export interface RunSummary {
  id: string;
  createdAt: string;
  name: string;
  countryA: string;
  countryB: string;
  terrain: Terrain;
  days: number;
  outcomes: Outcomes;
}
export interface SourceStatus {
  enabled: boolean;
  refreshing: boolean;
  lastAttempt: string;
  resourcesFetchedAt: string;
  resourceError: string;
  newsError: string;
  newsFetchedAt: string;
  conflict: Manifest;
}
