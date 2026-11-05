import type {
  CountryInfo,
  Metric,
  Outcomes,
  Scenario,
  Aggregate,
  HistoryRow,
  CandidateRow,
  SavedRun
} from './types';
export const count = (n: number) =>
  new Intl.NumberFormat('en', { maximumFractionDigits: 0 }).format(n);
export const compact = (n: number) =>
  new Intl.NumberFormat('en', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(n);
export const money = (m?: Metric) =>
  m ? '$' + compact(m.value) : 'Unavailable';
export const date = (s: string) => {
  const d = new Date(s);
  return Number.isNaN(d.valueOf())
    ? 'Date unavailable'
    : d.toLocaleDateString('en', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        timeZone: 'UTC'
      });
};
export const stamp = (s: string) =>
  s
    ? new Date(s).toLocaleString('en', {
        dateStyle: 'medium',
        timeStyle: 'short',
        timeZone: 'UTC'
      }) + ' UTC'
    : 'Not yet attempted';
export const label = (countries: CountryInfo[], id: string) =>
  countries.find((c) => c.id === id)?.name || id;
export const defaults = (): Scenario => ({
  name: 'Regional resource scenario',
  countryA: 'IND',
  countryB: 'PAK',
  terrain: 'MOUNTAIN',
  days: 60,
  trials: 1000,
  seed: 42,
  readinessA: 70,
  readinessB: 70,
  supplyA: 70,
  supplyB: 70,
  diplomacy: 30,
  climateStress: 20
});
export const terrainLabels = {
  OPEN: 'Open plains',
  MOUNTAIN: 'Mountainous',
  URBAN: 'Urban',
  DESERT: 'Arid / desert',
  COASTAL: 'Coastal'
};
export const outcomeLabels: Record<keyof Outcomes, string> = {
  advantageA: 'Side A advantage',
  advantageB: 'Side B advantage',
  prolonged: 'Prolonged contest',
  deescalation: 'De-escalation'
};
export const outcomeColors: Record<keyof Outcomes, string> = {
  advantageA: '#a9dbe1',
  advantageB: '#d8bd9b',
  prolonged: '#c2ccd0',
  deescalation: '#b7d679'
};
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  if (import.meta.env.VITE_PAGES === 'true') {
    const base = import.meta.env.BASE_URL + 'data/snapshot/';
    const read = async (name: string) => { const r = await fetch(base + name + '.json'); if (!r.ok) throw new Error('Snapshot unavailable'); return r.json(); };
    if (path === '/catalog') { const [countries, resources, manifest] = await Promise.all([read('countries'), read('metrics'), read('manifest')]); return { countries, resources, manifest } as T; }
    if (path === '/history') return await read('history') as T;
    if (path === '/candidate') return await read('candidate') as T;
    if (path === '/news') { const n = await read('news'); return { ...n, stale: true, lastError: 'This Pages edition shows a dated reporting snapshot.' } as T; }
    if (path === '/sources') { const [manifest, resources, news] = await Promise.all([read('manifest'), read('metrics'), read('news')]); return { enabled: false, refreshing: false, lastAttempt: '', resourcesFetchedAt: resources.fetchedAt, resourceError: '', newsError: '', newsFetchedAt: news.fetchedAt, conflict: manifest } as T; }
    if (path === '/runs') return [] as T;
    throw new Error('The Java application is required for simulations and saved scenarios.');
  }
  const res = await fetch('/api' + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers }
  });
  if (!res.ok) {
    let message = `Request failed (${res.status}).`;
    try {
      const body = await res.json();
      message = body.message || message;
    } catch {}
    throw new Error(message);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}
export function aggregate(
  history: HistoryRow[],
  candidate: CandidateRow[],
  year: string
): Aggregate[] {
  if (year !== 'latest')
    return history
      .filter((r) => r.year === Number(year))
      .map((r) => ({ ...r, events: null }));
  const by = new Map<string, Aggregate>();
  for (const r of candidate) {
    let row = by.get(r.country);
    if (!row) {
      row = {
        country: r.country,
        best: 0,
        low: 0,
        high: 0,
        events: 0,
        stateBased: 0,
        nonState: 0,
        oneSided: 0
      };
      by.set(r.country, row);
    }
    for (const key of [
      'best',
      'low',
      'high',
      'stateBased',
      'nonState',
      'oneSided'
    ] as const)
      row[key] += r[key];
    row.events! += r.events;
  }
  return [...by.values()];
}
export function download(
  name: string,
  body: string,
  type = 'application/json'
) {
  const url = URL.createObjectURL(new Blob([body], { type }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export function exportRun(run: SavedRun) {
  download(
    `conflict-atlas-${run.id}.json`,
    JSON.stringify({ schemaVersion: 1, ...run }, null, 2)
  );
}
export function exportTimeline(run: SavedRun) {
  const rows = [
    [
      'Day',
      'A p10',
      'A median',
      'A p90',
      'B p10',
      'B median',
      'B p90',
      'Strain p10',
      'Strain median',
      'Strain p90'
    ],
    ...run.result.timeline.map((p) => [
      p.day,
      p.sideA.p10,
      p.sideA.median,
      p.sideA.p90,
      p.sideB.p10,
      p.sideB.median,
      p.sideB.p90,
      p.strain.p10,
      p.strain.median,
      p.strain.p90
    ])
  ];
  download(
    `scenario-${run.id}-timeline.csv`,
    rows.map((r) => r.join(',')).join('\r\n'),
    'text/csv'
  );
}
