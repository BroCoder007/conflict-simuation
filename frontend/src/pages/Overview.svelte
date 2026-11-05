<script lang="ts">
  import {
    ArrowUpRight,
    SlidersHorizontal,
    Search,
    Mountain,
    ArrowRight,
    ExternalLink
  } from '@lucide/svelte';
  import WorldMap from '../components/WorldMap.svelte';
  import type {
    Catalog,
    HistoryRow,
    CandidateRow,
    NewsSnapshot,
    Aggregate
  } from '../types';
  import { aggregate, count, compact, date, label } from '../lib';
  let {
    catalog,
    history,
    candidate,
    news,
    watchlist,
    oncountry,
    onlab
  }: {
    catalog: Catalog;
    history: HistoryRow[];
    candidate: CandidateRow[];
    news: NewsSnapshot | null;
    watchlist: string[];
    oncountry: (id: string) => void;
    onlab: () => void;
  } = $props();
  let year = $state('latest');
  let search = $state('');
  let physical = $state(false);
  let region = $state('All regions');
  let raw = $derived(aggregate(history, candidate, year));
  let rows = $derived(
    raw.filter(
      (r) =>
        r.best > 0 &&
        (!region ||
          region === 'All regions' ||
          catalog.countries.find((c) => c.id === r.country)?.region === region)
    )
  );
  let ranked = $derived([...rows].sort((a, b) => b.best - a.best));
  let filtered = $derived(
    ranked.filter((r) =>
      label(catalog.countries, r.country)
        .toLowerCase()
        .includes(search.toLowerCase())
    )
  );
  let total = $derived(rows.reduce((n, r) => n + r.best, 0));
  let low = $derived(rows.reduce((n, r) => n + r.low, 0));
  let high = $derived(rows.reduce((n, r) => n + r.high, 0));
  let period = $derived(
    year === 'latest'
      ? 'JAN–SEP 2026 · CANDIDATE DATA'
      : `${year} · ANNUAL DATA`
  );
  let regions = $derived(
    [
      ...new Set(
        catalog.countries.filter((c) => !c.historical).map((c) => c.region)
      )
    ].sort()
  );
</script>

<div class="page-intro">
  <div>
    <p class="eyebrow">THE GLOBAL OBSERVATORY</p>
    <h1>A world in focus<span>.</span></h1>
    <p>Understand the context. Explore the data. Question the assumptions.</p>
  </div>
  <button class="dark-button" onclick={onlab}
    >Open scenario lab <ArrowUpRight size={16} /></button
  >
</div>
<div class="stat-grid">
  <article>
    <div class="stat-top">
      Countries with reported violence<span class="tiny-dot"></span>
    </div>
    <strong>{rows.length}</strong>
    <p>{period}</p>
  </article>
  <article>
    <div class="stat-top">Reported fatalities · best estimate</div>
    <strong>{compact(total)}</strong>
    <p>Published range {count(low)}–{count(high)}</p>
  </article>
  <article>
    <div class="stat-top">
      {year === 'latest' ? 'Candidate events recorded' : 'Historical coverage'}
    </div>
    <strong
      >{year === 'latest'
        ? compact(rows.reduce((n, r) => n + (r.events || 0), 0))
        : '1989–2025'}</strong
    >
    <p>
      {year === 'latest'
        ? 'All organized-violence categories'
        : 'Country-year observations'}
    </p>
  </article>
  <article class="stat-note">
    <span class="tiny-label">DATA, WITH CONTEXT</span>
    <h3>Every number has<br />a reporting period.</h3>
    <p>Published observations. Never a live battlefield feed.</p>
  </article>
</div>
<div class="observatory-grid">
  <section class="map-panel">
    <div class="panel-heading">
      <div>
        <h2>Conflict landscape</h2>
        <span>{period}</span>
      </div>
      <div class="map-filters">
        <label
          ><span class="sr-only">Observation period</span><select
            bind:value={year}
            ><option value="latest">Latest candidate release</option
            >{#each Array.from( { length: 37 }, (_, i) => String(2025 - i) ) as y}<option
                value={y}>{y} annual</option
              >{/each}</select
          ></label
        ><button
          class:active={physical}
          class="icon-button terrain-toggle"
          aria-label="Toggle physical landforms"
          aria-pressed={physical}
          onclick={() => (physical = !physical)}><Mountain size={17} /></button
        >
      </div>
    </div>
    <WorldMap
      {rows}
      countries={catalog.countries}
      {physical}
      onselect={oncountry}
    />
    <div class="map-bottom">
      <span
        ><span class="live-dot"></span>{year === 'latest'
          ? `UCDP candidate ${catalog.manifest.candidateVersion}`
          : `UCDP country-year ${catalog.manifest.historyVersion}`}</span
      ><button class="link-button" onclick={onlab}
        >Explore a scenario <ArrowRight size={14} /></button
      >
    </div>
  </section>
  <aside class="country-ranking">
    <div class="panel-heading">
      <div>
        <h2>Country watch</h2>
        <span>Sorted by reported fatalities</span>
      </div>
      <span class="count-pill">{rows.length}</span>
    </div>
    <div class="ranking-filter">
      <label class="search"
        ><Search size={14} /><input
          bind:value={search}
          type="search"
          placeholder="Find a country"
          aria-label="Find a country in the observatory"
        /></label
      ><select bind:value={region} aria-label="Filter region"
        ><option>All regions</option>{#each regions as r}<option>{r}</option
          >{/each}</select
      >
    </div>
    <div class="ranking-list">
      {#each filtered as row, i}<button
          class="country-row"
          onclick={() => oncountry(row.country)}
          ><span class="country-number">{String(i + 1).padStart(2, '0')}</span
          ><span class="country-row-name"
            >{label(catalog.countries, row.country)}<small
              >{row.events !== null
                ? `${count(row.events)} candidate events`
                : 'Annual observation'}</small
            ></span
          ><span class="fatal-count"
            >{compact(row.best)}<span
              class="rank-bar"
              style:width={`${Math.max(3, Math.sqrt(row.best / (ranked[0]?.best || 1)) * 65)}px`}
            ></span></span
          ><ArrowUpRight size={13} /></button
        >{:else}<p class="empty-small">
          No observations match these filters.
        </p>{/each}
    </div>
  </aside>
</div>
<p class="quality-link">
  <a href="#method"
    >{catalog.manifest.qualityIssues.length} source-interval quality flags · view
    the register ↗</a
  >
</p>
<div class="lower-grid">
  <section class="reporting">
    <div class="section-heading">
      <div>
        <p class="eyebrow">LATEST REPORTING</p>
        <h2>Beyond the numbers</h2>
      </div>
      <span class:stale={news?.stale} class="feed-badge"
        ><span class="live-dot"></span>{news && !news.stale
          ? 'Feed refreshed'
          : 'Saved feed snapshot'}</span
      >
    </div>
    <p class="section-description">
      Selected official UN News reporting on conflict and humanitarian
      developments. Headlines are filtered by topic words; this is not a
      complete conflict inventory.
    </p>
    {#if news?.lastError}<p class="inline-note">{news.lastError}</p>{/if}
    <div class="news-grid">
      {#each news?.items.slice(0, 6) || [] as item}<a
          class="news-card"
          href={item.url}
          target="_blank"
          rel="noreferrer"
          ><span class="news-meta"
            >{item.source} / {date(item.publishedAt)}</span
          >
          <h3>{item.title}</h3>
          <span class="read-story"
            >Read the report <ExternalLink size={13} /></span
          ></a
        >{:else}<p class="empty-small">
          Reporting is unavailable. The published conflict datasets remain
          available above.
        </p>{/each}
    </div>
  </section>
  <aside class="watch-panel">
    <p class="eyebrow">YOUR WATCHLIST</p>
    <h2>A closer look.</h2>
    <p>Keep the countries you follow within reach.</p>
    {#each watchlist as id}<button
        class="watch-row"
        onclick={() => oncountry(id)}
        >{label(catalog.countries, id)}<ArrowUpRight size={15} /></button
      >{:else}<div class="watch-empty">
        <Search size={23} />
        <p>Select a country on the map, then save it to your watchlist.</p>
      </div>{/each}<span class="local-note">Saved in this browser</span>
  </aside>
</div>
