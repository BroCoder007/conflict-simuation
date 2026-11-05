<script lang="ts">
  import {
    Play,
    RotateCcw,
    Upload,
    Download,
    ChevronDown,
    Check,
    Info,
    Mountain
  } from '@lucide/svelte';
  import SimulationChart from '../components/SimulationChart.svelte';
  import OutcomeBars from '../components/OutcomeBars.svelte';
  import type { Catalog, SavedRun, Scenario, Terrain } from '../types';
  import {
    api,
    defaults,
    label,
    money,
    compact,
    terrainLabels,
    exportRun,
    exportTimeline,
    stamp
  } from '../lib';
  let {
    catalog,
    run = null,
    startCountry = '',
    oncreated
  }: {
    catalog: Catalog;
    run: SavedRun | null;
    startCountry?: string;
    oncreated: (run: SavedRun) => void;
  } = $props();
  let draft = $state<Scenario>(defaults());
  let busy = $state(false);
  let error = $state('');
  let imported = $state(false);
  let input: HTMLInputElement;
  let previous = '';
  let previousStart = '';
  $effect(() => {
    if (run && run.id !== previous) {
      previous = run.id;
      draft = { ...run.result.request };
    }
  });
  $effect(() => {
    if (startCountry && startCountry !== previousStart) {
      previousStart = startCountry;
      draft = {
        ...draft,
        countryA: startCountry,
        countryB:
          draft.countryB === startCountry
            ? startCountry === 'USA'
              ? 'CHN'
              : 'USA'
            : draft.countryB
      };
    }
  });
  let eligible = $derived(
    catalog.countries.filter(
      (c) =>
        !c.historical &&
        catalog.resources.countries[c.id]?.spending?.value > 0 &&
        catalog.resources.countries[c.id]?.personnel?.value > 0
    )
  );
  let metricsA = $derived(catalog.resources.countries[draft.countryA] || {});
  let metricsB = $derived(catalog.resources.countries[draft.countryB] || {});
  let changed = $derived(
    run && JSON.stringify(draft) !== JSON.stringify(run.result.request)
  );
  async function simulate() {
    busy = true;
    error = '';
    try {
      const saved = await api<SavedRun>('/runs', {
        method: 'POST',
        body: JSON.stringify(draft)
      });
      oncreated(saved);
      imported = false;
    } catch (e) {
      error = e instanceof Error ? e.message : 'The scenario could not be run.';
    } finally {
      busy = false;
    }
  }
  async function loadInputs(event: Event) {
    const target = event.currentTarget as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) return;
    try {
      if (file.size > 524288)
        throw new Error('Choose an input document smaller than 512 KB.');
      const data = JSON.parse(await file.text());
      const values = data.result?.request || data.request || data;
      const keys = Object.keys(defaults()) as (keyof Scenario)[];
      if (keys.some((key) => !(key in values)))
        throw new Error(
          'This file does not contain a complete Conflict Atlas scenario.'
        );
      const next = Object.fromEntries(
        keys.map((key) => [key, values[key]])
      ) as unknown as Scenario;
      if (
        !eligible.some((c) => c.id === next.countryA) ||
        !eligible.some((c) => c.id === next.countryB) ||
        !Object.keys(terrainLabels).includes(next.terrain)
      )
        throw new Error(
          'Choose countries with published resource data and a supported terrain preset.'
        );
      draft = next;
      imported = true;
      error = '';
    } catch (e) {
      error = e instanceof Error ? e.message : 'Invalid scenario file.';
    }
    target.value = '';
  }
</script>

<div class="page-intro">
  <div>
    <p class="eyebrow">THE SCENARIO LAB</p>
    <h1>Change the assumptions<span>.</span></h1>
    <p>
      An aggregate Java simulation. Explore how the model behaves under
      uncertainty.
    </p>
  </div>
  <button class="light-button" onclick={() => input.click()}
    ><Upload size={16} />Import inputs</button
  ><input
    class="sr-only"
    bind:this={input}
    type="file"
    accept="application/json,.json"
    onchange={loadInputs}
  />
</div>
{#if error}<p class="notice error" role="alert">{error}</p>{/if}{#if imported}<p
    class="notice"
  >
    Inputs loaded. Run the scenario to recompute results using the current data
    snapshot.
  </p>{/if}
<div class="lab-grid">
  <form
    class="scenario-form panel"
    onsubmit={(e) => {
      e.preventDefault();
      simulate();
    }}
  >
    <div class="form-section">
      <div class="form-step">
        <span>01</span>
        <h2>The participants</h2>
      </div>
      <label
        >Scenario title<input
          bind:value={draft.name}
          required
          maxlength="80"
        /></label
      >
      <div class="two-cols">
        <label
          >Side A<select bind:value={draft.countryA}
            >{#each eligible as c}<option value={c.id}>{c.name}</option
              >{/each}</select
          ></label
        ><label
          >Side B<select bind:value={draft.countryB}
            >{#each eligible as c}<option value={c.id}>{c.name}</option
              >{/each}</select
          ></label
        >
      </div>
      <div class="resource-readout">
        <div>
          <span>SIDE A / RESOURCE OBSERVATIONS</span><strong
            >{money(metricsA.spending)}
            <small>{metricsA.spending?.year}</small></strong
          >
          <p>
            {metricsA.personnel
              ? compact(metricsA.personnel.value)
              : 'No personnel data'} personnel · {metricsA.personnel?.year ||
              'unavailable'}
          </p>
        </div>
        <div>
          <span>SIDE B / RESOURCE OBSERVATIONS</span><strong
            >{money(metricsB.spending)}
            <small>{metricsB.spending?.year}</small></strong
          >
          <p>
            {metricsB.personnel
              ? compact(metricsB.personnel.value)
              : 'No personnel data'} personnel · {metricsB.personnel?.year ||
              'unavailable'}
          </p>
        </div>
      </div>
      <p class="field-note">
        These are the latest published values, with their actual years.
        Readiness, supply and terrain below are user assumptions.
      </p>
    </div>
    <div class="form-section">
      <div class="form-step">
        <span>02</span>
        <h2>The environment</h2>
      </div>
      <label
        >Generic terrain<select bind:value={draft.terrain}
          >{#each Object.entries(terrainLabels) as [key, value]}<option
              value={key}>{value}</option
            >{/each}</select
        ></label
      >
      <div class="two-cols">
        <label
          >Horizon (days)<input
            type="number"
            bind:value={draft.days}
            min="7"
            max="180"
            required
          /></label
        ><label
          >Trials<select bind:value={draft.trials}
            ><option value={250}>250 · quick</option><option value={1000}
              >1,000 · standard</option
            ><option value={2000}>2,000 · extended</option></select
          ></label
        >
      </div>
      <label class="slider-label"
        ><span>Climate stress <strong>{draft.climateStress}%</strong></span
        ><input
          type="range"
          min="0"
          max="100"
          bind:value={draft.climateStress}
        /></label
      >
      <p class="field-note">
        Landform overlays provide geographic context. This preset applies
        generic friction to both sides; it is not a ground survey.
      </p>
    </div>
    <div class="form-section">
      <div class="form-step">
        <span>03</span>
        <h2>The assumptions</h2>
      </div>
      <div class="two-cols">
        <label class="slider-label"
          ><span>A readiness <strong>{draft.readinessA}</strong></span><input
            type="range"
            min="20"
            max="100"
            bind:value={draft.readinessA}
          /></label
        ><label class="slider-label"
          ><span>B readiness <strong>{draft.readinessB}</strong></span><input
            type="range"
            min="20"
            max="100"
            bind:value={draft.readinessB}
          /></label
        ><label class="slider-label"
          ><span>A supply <strong>{draft.supplyA}</strong></span><input
            type="range"
            min="20"
            max="100"
            bind:value={draft.supplyA}
          /></label
        ><label class="slider-label"
          ><span>B supply <strong>{draft.supplyB}</strong></span><input
            type="range"
            min="20"
            max="100"
            bind:value={draft.supplyB}
          /></label
        >
      </div>
      <label class="slider-label"
        ><span>Diplomatic pressure <strong>{draft.diplomacy}%</strong></span
        ><input
          type="range"
          min="0"
          max="100"
          bind:value={draft.diplomacy}
        /></label
      >
      <details class="seed-details">
        <summary>Reproducibility settings</summary><label
          >Random seed<input
            type="number"
            bind:value={draft.seed}
            min="0"
            max="2147483647"
            required
          /></label
        >
        <p class="field-note">
          The same inputs, data and model version reproduce the same numerical
          result.
        </p>
      </details>
    </div>
    <div class="run-controls">
      <button
        class="dark-button run-button"
        disabled={busy || draft.countryA === draft.countryB}
        ><Play size={15} />{busy
          ? 'Running the trials…'
          : 'Run & save scenario'}</button
      ><button
        type="button"
        class="icon-button"
        aria-label="Reset scenario inputs"
        onclick={() => {
          draft = defaults();
          error = '';
        }}><RotateCcw size={16} /></button
      >
    </div>
    <p class="field-note">
      Runs are saved to this browser’s private workspace. Up to 50 saved runs.
    </p>
  </form>
  <div class="lab-results">
    {#if run}<section class="result-banner">
        <span class="eyebrow"
          >SCENARIO COMPLETE / {run.result.request.trials.toLocaleString()} TRIALS</span
        >
        <h2>{run.name}</h2>
        <p>
          {label(catalog.countries, run.result.request.countryA)} <span>×</span>
          {label(catalog.countries, run.result.request.countryB)} · {run.result
            .request.days} days · {terrainLabels[run.result.request.terrain]}
        </p>
        <div class="result-actions">
          <button onclick={() => exportRun(run!)}
            ><Download size={14} />Export result JSON</button
          ><button onclick={() => exportTimeline(run!)}
            ><Download size={14} />Export timeline CSV</button
          >
        </div>
      </section>
      {#if changed}<p class="notice">
          Inputs have changed. Run again to update these saved results.
        </p>{/if}
      <section class="panel results-outcomes">
        <div class="panel-heading">
          <div>
            <h2>Illustrative scenario shares</h2>
            <span>Model classifications across sampled trials</span>
          </div>
        </div>
        <OutcomeBars outcomes={run.result.outcomes} />
        <p class="panel-footnote">
          These are uncalibrated model outputs, not probabilities of real-world
          victory. “Advantage” describes a model resource-index ratio.
        </p>
      </section>
      <section class="panel trajectory-panel">
        <div class="panel-heading">
          <div>
            <h2>How the model unfolds</h2>
            <span>Remaining aggregate resource index · arbitrary units</span>
          </div>
        </div>
        <SimulationChart points={run.result.timeline} />
        <div class="chart-caption">
          <span><i class="line-swatch side-a"></i>Side A median</span><span
            ><i class="line-swatch side-b"></i>Side B median</span
          ><span>Shading: sampled P10–P90</span>
        </div>
        <details class="data-details">
          <summary>Read the sampled trajectory</summary>
          <div class="table-wrap">
            <table>
              <thead
                ><tr
                  ><th>Day</th><th>A median (P10–P90)</th><th
                    >B median (P10–P90)</th
                  ><th>Strain index median</th></tr
                ></thead
              ><tbody
                >{#each run.result.timeline.filter((p, i) => i % 5 === 0 || i === run!.result.timeline.length - 1) as p}<tr
                    ><td>{p.day}</td><td
                      >{p.sideA.median} ({p.sideA.p10}–{p.sideA.p90})</td
                    ><td>{p.sideB.median} ({p.sideB.p10}–{p.sideB.p90})</td><td
                      >{p.strain.median}</td
                    ></tr
                  >{/each}</tbody
              >
            </table>
          </div>
        </details>
      </section>
      <section class="panel factor-panel">
        <div class="panel-heading">
          <div>
            <h2>What went into the result</h2>
            <span>Transparent inputs, no hidden country ranking</span>
          </div>
        </div>
        {#each run.result.factors as f}<div class="factor-row">
            <div>
              <h3>{f.name}</h3>
              <p>{f.basis}</p>
            </div>
            <span>A {f.sideA.toFixed(1)}</span><span
              >B {f.sideB.toFixed(1)}</span
            >
          </div>{/each}
        <details class="data-details">
          <summary>Assumptions, limitations & data receipt</summary>
          <ul class="model-notes">
            {#each run.result.notes as note}<li>{note}</li>{/each}
          </ul>
          <dl class="receipt">
            <div>
              <dt>Model</dt>
              <dd>{run.result.modelVersion}</dd>
            </div>
            <div>
              <dt>Generated</dt>
              <dd>{stamp(run.result.generatedAt)}</dd>
            </div>
            <div>
              <dt>Resources retrieved</dt>
              <dd>{stamp(run.result.inputs.resourceFetchedAt)}</dd>
            </div>
            <div>
              <dt>Random seed</dt>
              <dd>{run.result.request.seed}</dd>
            </div>
          </dl>
        </details>
      </section>{:else}<section class="lab-empty">
        <div class="lab-orbit" aria-hidden="true"><Mountain size={42} /></div>
        <span class="eyebrow">START WITH A QUESTION</span>
        <h2>What changes<br />when the inputs do?</h2>
        <p>
          Select two countries, choose the conditions and run the trials. See
          the range of outcomes, then compare your assumptions.
        </p>
        <div class="model-principles">
          <span><Check size={14} />Seeded & reproducible</span><span
            ><Check size={14} />Published data years</span
          ><span><Check size={14} />Visible model assumptions</span>
        </div>
        <div class="lab-caveat">
          <Info size={17} />
          <p>
            This is an educational resource model. It does not predict
            casualties, territory, military manoeuvres or real-world war
            outcomes.
          </p>
        </div>
      </section>{/if}
  </div>
</div>
