<script lang="ts">
  import { ExternalLink, Database, Layers, FlaskConical } from '@lucide/svelte';
  import type { Catalog, SourceStatus } from '../types';
  import { count, stamp } from '../lib';
  let { catalog, sources }: { catalog: Catalog; sources: SourceStatus | null } =
    $props();
</script>

<div class="page-intro">
  <div>
    <p class="eyebrow">DATA & METHODOLOGY</p>
    <h1>Show the working<span>.</span></h1>
    <p>
      Where the observations come from, and where the model’s assumptions begin.
    </p>
  </div>
</div>
<div class="method-grid">
  <article class="panel method-card">
    <Database size={23} />
    <h2>Observed data</h2>
    <p>
      UCDP records organized violence: state-based, non-state and one-sided
      violence. These categories are broader than interstate wars. The map
      aggregates observations by country.
    </p>
    <dl>
      <div>
        <dt>Annual release</dt>
        <dd>{catalog.manifest.historyVersion} / 1989–2025</dd>
      </div>
      <div>
        <dt>Country-year rows</dt>
        <dd>{count(catalog.manifest.historyRows)}</dd>
      </div>
      <div>
        <dt>Candidate release</dt>
        <dd>{catalog.manifest.candidateVersion}</dd>
      </div>
      <div>
        <dt>Candidate period</dt>
        <dd>1 January–30 September 2026</dd>
      </div>
      <div>
        <dt>Retrieved</dt>
        <dd>{stamp(catalog.manifest.fetchedAt)}</dd>
      </div>
    </dl>
    <a
      class="source-link"
      href="https://ucdp.uu.se/downloads/"
      target="_blank"
      rel="noreferrer">UCDP downloads & codebooks <ExternalLink size={13} /></a
    >
    <p class="field-note">
      Candidate observations can be revised. Fatality ranges are published
      estimates, not complete counts. Monthly candidate totals allocate an event
      to its reported start month. No precise event locations are distributed in
      this app.
    </p>
  </article>
  <article class="panel method-card">
    <Layers size={23} />
    <h2>Resources & geography</h2>
    <p>
      World Bank indicators provide military expenditure (SIPRI), personnel, GDP
      and population. Every value retains its observation year. They are
      incomplete proxies and do not measure current combat effectiveness.
    </p>
    <p>
      Natural Earth country polygons and major landform regions are bundled
      locally. The physical overlay is geographic context; scenario terrain and
      climate are manually assigned.
    </p>
    <a
      class="source-link"
      href="https://data.worldbank.org/indicator/MS.MIL.XPND.CD"
      target="_blank"
      rel="noreferrer"
      >World Bank military indicators <ExternalLink size={13} /></a
    ><a
      class="source-link"
      href="https://www.naturalearthdata.com/about/terms-of-use/"
      target="_blank"
      rel="noreferrer">Natural Earth map data <ExternalLink size={13} /></a
    >
    <p class="field-note">
      Information from the Stockholm International Peace Research Institute
      (SIPRI) Military Expenditure Database, https://doi.org/10.55163/CQGC9685.
      Data terms differ from the code licence. This project is an educational,
      non-commercial use; check source terms before reuse.
    </p>
  </article>
</div>
<section class="panel model-method">
  <div class="panel-heading">
    <div>
      <h2>The Java simulation</h2>
      <span>aggregate-resource-v1 · explicit, illustrative coefficients</span>
    </div>
    <FlaskConical size={22} />
  </div>
  <div class="method-columns">
    <article>
      <h3>01 / Resource balance</h3>
      <p>
        Take square-root shares of each country’s observed spending and
        personnel relative to the pair. Blend them 55:45. Initial index shares
        combine that proxy (65%), assumed readiness share (20%) and assumed
        supply share (15%). The pair starts at 100 arbitrary units before
        sampling.
      </p>
    </article>
    <article>
      <h3>02 / Sample uncertainty</h3>
      <p>
        Each trial samples independent lognormal initial variation and daily
        shocks. Generic terrain increases shared friction and uncertainty.
        Variability in up to ten historical country-years widens dispersion; it
        does not imply military experience or effectiveness.
      </p>
    </article>
    <article>
      <h3>03 / Classify the trials</h3>
      <p>
        Supply and climate assumptions affect aggregate index decay. Diplomatic
        pressure sets an illustrative daily stopping rate. A final index ratio
        above 1.35 is classified as an advantage; other continuing trials are a
        prolonged contest. Stopped trials are de-escalation.
      </p>
    </article>
  </div>
  <p class="notice">
    The coefficients are not fitted or calibrated against historical war
    outcomes. Trial shares are not reliable real-world probabilities. The model
    does not estimate casualties, territorial gains, targeting, manoeuvres,
    alliance interventions or nuclear escalation.
  </p>
  <details class="data-details">
    <summary>Historical variability and output ranges</summary>
    <p>
      For each country, calculate the population standard deviation of log(1 +
      annual reported fatalities) over up to ten observed country-years. Divide
      by 5 and cap at 1. With fewer than three observations use a declared
      neutral value of 0.5; missing years are never inserted as zero. This
      affects dispersion only.
    </p>
    <p>
      P10, median and P90 are order statistics across sampled trials on each
      day. They are descriptive model ranges, not empirically validated
      confidence intervals. For full equations, data receipts and
      reproducibility, see the repository documentation or export a run.
    </p>
  </details>
</section>
<section class="panel source-status">
  <div class="panel-heading">
    <div>
      <h2>Source health</h2>
      <span
        >Last successful data is retained when a provider is unavailable</span
      >
    </div>
  </div>
  <dl>
    <div>
      <dt>Resources retrieved</dt>
      <dd>
        {stamp(sources?.resourcesFetchedAt || catalog.resources.fetchedAt)}
      </dd>
    </div>
    <div>
      <dt>UN reporting retrieved</dt>
      <dd>{stamp(sources?.newsFetchedAt || '')}</dd>
    </div>
    <div>
      <dt>Last refresh attempt</dt>
      <dd>{stamp(sources?.lastAttempt || '')}</dd>
    </div>
    <div>
      <dt>Automatic updates</dt>
      <dd>
        {sources?.enabled
          ? 'Enabled · news every 15 minutes; resources after 24 hours'
          : 'Disabled by server configuration'}
      </dd>
    </div>
  </dl>
  {#if sources?.resourceError}<p class="inline-note">
      {sources.resourceError}
    </p>{/if}{#if sources?.newsError}<p class="inline-note">
      {sources.newsError}
    </p>{/if}
  <p class="field-note">
    UCDP releases are pinned and rebuilt with the repository’s data tool; the
    refresh button updates news and resource observations, not the pinned
    conflict release. UN News’s feed is official reporting, not a complete or
    independently verified global war status feed.
  </p>
</section>
<section class="panel source-table">
  <div class="panel-heading"><h2>Snapshot source receipts</h2></div>
  <div class="table-wrap">
    <table>
      <thead
        ><tr><th>Source</th><th>Original download</th><th>SHA-256</th></tr
        ></thead
      ><tbody
        >{#each catalog.manifest.sources as source}<tr
            ><td>{source.id}</td><td
              ><a href={source.url} target="_blank" rel="noreferrer"
                >Open source ↗</a
              ></td
            ><td><code>{source.sha256}</code></td></tr
          >{/each}</tbody
      >
    </table>
  </div>
  <p class="panel-footnote">
    Country and boundary names follow source conventions and do not imply
    recognition or endorsement. Historical predecessors are separate where
    mapped; comparability across changing borders is limited.
  </p>
</section>

<section class="panel quality-register">
  <div class="panel-heading">
    <div>
      <h2>Source-quality register</h2>
      <span>Published intervals retained without silent correction</span>
    </div>
  </div>
  <p class="panel-footnote">
    {catalog.manifest.qualityIssues.length} country-period aggregates have a low/high
    interval inconsistent with the best estimate. Invalid intervals are excluded from
    chart shading. Best estimates remain visible as published; the dataset is subject
    to revision.
  </p>
  <div class="table-wrap">
    <table>
      <thead
        ><tr
          ><th>Country</th><th>Period</th><th>Best</th><th>Low</th><th>High</th
          ></tr
        ></thead
      ><tbody
        >{#each catalog.manifest.qualityIssues as q}<tr
            ><td>{q.country}</td><td>{q.period}</td><td>{count(q.best)}</td><td
              >{count(q.low)}</td
            ><td>{count(q.high)}</td></tr
          >{/each}</tbody
      >
    </table>
  </div>
</section>
