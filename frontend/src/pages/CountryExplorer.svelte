<script lang="ts">
  import { Star, ArrowUpRight, Download, ArrowLeftRight } from '@lucide/svelte';
  import WorldMap from '../components/WorldMap.svelte';
  import HistoryChart from '../components/HistoryChart.svelte';
  import type {
    Catalog,
    HistoryRow,
    CandidateRow,
    CountryInfo
  } from '../types';
  import {
    aggregate,
    money,
    compact,
    count,
    download,
    date,
    label
  } from '../lib';
  let {
    catalog,
    history,
    candidate,
    id,
    watchlist,
    onselect,
    onwatch,
    onlab
  }: {
    catalog: Catalog;
    history: HistoryRow[];
    candidate: CandidateRow[];
    id: string;
    watchlist: string[];
    onselect: (id: string) => void;
    onwatch: (id: string) => void;
    onlab: (id: string) => void;
  } = $props();
  let compare = $state('PAK');
  let country = $derived(catalog.countries.find((c) => c.id === id)!);
  let metrics = $derived(catalog.resources.countries[id] || {});
  let other = $derived(catalog.resources.countries[compare] || {});
  let rows = $derived(history.filter((r) => r.country === id));
  let latest = $derived(aggregate(history, candidate, 'latest'));
  let current = $derived(latest.find((r) => r.country === id));
  const metricTitle = {
    spending: 'Military expenditure',
    personnel: 'Armed forces personnel',
    gdp: 'Gross domestic product',
    population: 'Population'
  };
  function exportProfile() {
    download(
      `country-${id}.json`,
      JSON.stringify(
        {
          country,
          metrics,
          resourcesFetchedAt: catalog.resources.fetchedAt,
          history: rows,
          candidate: candidate.filter((r) => r.country === id),
          conflictSources: catalog.manifest
        },
        null,
        2
      )
    );
  }
</script>

<div class="page-intro">
  <div>
    <p class="eyebrow">COUNTRY EXPLORER</p>
    <h1>Look beneath the headline<span>.</span></h1>
    <p>Published resource indicators and historical context, side by side.</p>
  </div>
  <button class="light-button" onclick={exportProfile}
    ><Download size={16} />Export profile</button
  >
</div>
<div class="country-toolbar">
  <label
    >Country / economy<select
      value={id}
      onchange={(e) => onselect(e.currentTarget.value)}
      >{#each catalog.countries as c}<option value={c.id}
          >{c.name}{c.historical ? ' · historical entity' : ''}</option
        >{/each}</select
    ></label
  ><button
    class:watched={watchlist.includes(id)}
    class="light-button"
    onclick={() => onwatch(id)}
    ><Star
      size={16}
      fill={watchlist.includes(id) ? 'currentColor' : 'none'}
    />{watchlist.includes(id)
      ? 'On your watchlist'
      : 'Add to watchlist'}</button
  >
</div>
<div class="country-heading">
  <div>
    <span class="country-code">{id}</span>
    <h2>{country.name}</h2>
    <p>
      {country.region}{country.historical
        ? ' · Historical borders and source names apply'
        : ''}
    </p>
  </div>
  {#if !country.historical}<button class="dark-button" onclick={() => onlab(id)}
      >Use in a scenario <ArrowUpRight size={16} /></button
    >{/if}
</div>
{#if catalog.manifest.qualityIssues.some((q) => q.country === id)}<p
    class="notice source-quality"
  >
    Source-quality note: this country has published intervals that do not
    contain the best estimate. Values are retained; inconsistent annual bands
    are omitted from the chart. <a href="#method">View the quality register ↗</a
    >
  </p>{/if}
<div class="country-metrics">
  {#each ['spending', 'personnel', 'gdp', 'population'] as key}<article>
      <p>{metricTitle[key as keyof typeof metricTitle]}</p>
      <strong
        >{metrics[key]
          ? ['spending', 'gdp'].includes(key)
            ? money(metrics[key])
            : compact(metrics[key].value)
          : 'Unavailable'}</strong
      ><span
        >{metrics[key]
          ? `Observation: ${metrics[key].year}`
          : 'No published observation in this snapshot'}</span
      >{#if metrics[key] && metrics[key].year < new Date().getUTCFullYear() - 2}<span
          class="age-note">Older observation</span
        >{/if}
    </article>{/each}
</div>
<p class="inline-note">
  Spending and headcount describe resources, not measured military
  effectiveness. Personnel can include paramilitary forces; definitions and
  reporting years differ. <a
    href="https://www.sipri.org/databases/milex/sources-and-methods"
    target="_blank"
    rel="noreferrer">Read source methods ↗</a
  >
</p>
<div class="country-detail-grid">
  <section class="panel history-panel">
    <div class="panel-heading">
      <div>
        <h2>The historical record</h2>
        <span>Reported organized-violence fatalities within borders</span>
      </div>
      <span class="tag">1989–2025</span>
    </div>
    <HistoryChart {rows} name={country.name} />
    <div class="chart-caption">
      <span><i class="line-swatch"></i>Best estimate</span><span
        ><i class="band-swatch"></i>Published low–high range</span
      >
    </div>
    <details class="data-details">
      <summary>Read the annual observations</summary>
      <div class="table-wrap">
        <table>
          <thead
            ><tr
              ><th>Year</th><th>Best</th><th>Low–high</th><th
                >Source country name</th
              ></tr
            ></thead
          ><tbody
            >{#each [...rows].reverse() as r}<tr
                ><td>{r.year}</td><td>{count(r.best)}</td><td
                  >{count(r.low)}–{count(r.high)}</td
                ><td>{r.sourceName}</td></tr
              >{/each}</tbody
          >
        </table>
      </div>
    </details>
    <p class="panel-footnote">
      UCDP {catalog.manifest.historyVersion}. Missing country-years are not
      filled with zero. Predecessor-state observations are kept separately where
      mapped; source boundaries may change over time.
    </p>
  </section>
  <section class="panel country-current">
    <div class="panel-heading">
      <div>
        <h2>Latest conflict release</h2>
        <span>January–September 2026 · candidate data</span>
      </div>
    </div>
    {#if current}<strong class="country-fatalities"
        >{count(current.best)}</strong
      >
      <p>Reported fatalities · best estimate</p>
      <span class="range-caption"
        >Published range {count(current.low)}–{count(current.high)}</span
      >
      <dl>
        <div>
          <dt>State-based</dt>
          <dd>{count(current.stateBased)}</dd>
        </div>
        <div>
          <dt>Non-state</dt>
          <dd>{count(current.nonState)}</dd>
        </div>
        <div>
          <dt>One-sided violence</dt>
          <dd>{count(current.oneSided)}</dd>
        </div>
        <div>
          <dt>Candidate events</dt>
          <dd>{count(current.events || 0)}</dd>
        </div>
      </dl>{:else}<div class="empty-small">
        No candidate events are recorded for this country in the bundled
        release. This does not establish that no violence occurred.
      </div>{/if}
  </section>
</div>
<section class="panel comparison-panel">
  <div class="panel-heading">
    <div>
      <h2>Compare the resources</h2>
      <span>Latest available observation for each indicator</span>
    </div>
    <label
      ><span class="sr-only">Comparison country</span><select
        bind:value={compare}
        >{#each catalog.countries.filter((c) => !c.historical && c.id !== id) as c}<option
            value={c.id}>{c.name}</option
          >{/each}</select
      ></label
    >
  </div>
  <div class="table-wrap">
    <table>
      <thead
        ><tr
          ><th>Indicator</th><th>{country.name}</th><th
            >{label(catalog.countries, compare)}</th
          ></tr
        ></thead
      ><tbody
        >{#each ['spending', 'personnel', 'gdp', 'population'] as key}<tr
            ><td>{metricTitle[key as keyof typeof metricTitle]}</td><td
              >{metrics[key]
                ? ['spending', 'gdp'].includes(key)
                  ? money(metrics[key])
                  : count(metrics[key].value)
                : 'Unavailable'}<small
                >{metrics[key]?.year || 'No observation'}</small
              ></td
            ><td
              >{other[key]
                ? ['spending', 'gdp'].includes(key)
                  ? money(other[key])
                  : count(other[key].value)
                : 'Unavailable'}<small
                >{other[key]?.year || 'No observation'}</small
              ></td
            ></tr
          >{/each}</tbody
      >
    </table>
  </div>
</section>
