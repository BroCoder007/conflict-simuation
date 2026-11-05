<script lang="ts">
  import {
    ArrowUpRight,
    Download,
    Trash2,
    GitCompareArrows,
    Play
  } from '@lucide/svelte';
  import OutcomeBars from '../components/OutcomeBars.svelte';
  import type { Catalog, RunSummary, SavedRun } from '../types';
  import { api, label, date, exportRun, terrainLabels } from '../lib';
  let {
    catalog,
    runs,
    onopen,
    onrefresh,
    onlab
  }: {
    catalog: Catalog;
    runs: RunSummary[];
    onopen: (run: SavedRun) => void;
    onrefresh: () => Promise<void>;
    onlab: () => void;
  } = $props();
  let selected = $state<string[]>([]);
  let compared = $state<SavedRun[]>([]);
  let error = $state('');
  let pendingDelete = $state('');
  let busy = $state(false);
  async function open(id: string) {
    try {
      onopen(await api<SavedRun>('/runs/' + id));
    } catch (e) {
      error = (e as Error).message;
    }
  }
  async function compare() {
    busy = true;
    try {
      compared = await Promise.all(
        selected.map((id) => api<SavedRun>('/runs/' + id))
      );
    } catch (e) {
      error = (e as Error).message;
    } finally {
      busy = false;
    }
  }
  async function remove(id: string) {
    try {
      await api('/runs/' + id, { method: 'DELETE' });
      selected = selected.filter((s) => s !== id);
      compared = compared.filter((s) => s.id !== id);
      pendingDelete = '';
      await onrefresh();
    } catch (e) {
      error = (e as Error).message;
    }
  }
  async function exportSaved(id: string) {
    try {
      exportRun(await api<SavedRun>('/runs/' + id));
    } catch (e) {
      error = (e as Error).message;
    }
  }
</script>

<div class="page-intro">
  <div>
    <p class="eyebrow">YOUR SCENARIO LIBRARY</p>
    <h1>Keep the questions coming<span>.</span></h1>
    <p>Revisit a run, compare the assumptions and export the evidence.</p>
  </div>
  <button class="dark-button" onclick={onlab}
    >New scenario <Play size={15} /></button
  >
</div>
{#if error}<p class="notice error" role="alert">{error}</p>{/if}
<div class="library-toolbar">
  <span>{runs.length} / 50 saved runs · this browser’s workspace</span><button
    class="light-button"
    disabled={selected.length !== 2 || busy}
    onclick={compare}
    ><GitCompareArrows size={16} />{busy
      ? 'Loading comparison…'
      : `Compare selected (${selected.length}/2)`}</button
  >
</div>
{#if !runs.length}<section class="large-empty">
    <GitCompareArrows size={40} />
    <h2>Your first scenario belongs here.</h2>
    <p>
      Run a scenario in the lab. The inputs, data receipt and results will be
      saved together.
    </p>
    <button class="dark-button" onclick={onlab}
      >Go to the lab <ArrowUpRight size={15} /></button
    >
  </section>{:else}<div class="run-grid">
    {#each runs as run}<article class="saved-card">
        <div class="saved-top">
          <label
            ><input
              type="checkbox"
              aria-label={`Compare ${run.name}`}
              checked={selected.includes(run.id)}
              disabled={!selected.includes(run.id) && selected.length >= 2}
              onchange={(e) => {
                selected = e.currentTarget.checked
                  ? [...selected, run.id]
                  : selected.filter((id) => id !== run.id);
              }}
            /><span>Select to compare</span></label
          ><span>{date(run.createdAt)}</span>
        </div>
        <h2>{run.name}</h2>
        <p>
          {label(catalog.countries, run.countryA)} <span>×</span>
          {label(catalog.countries, run.countryB)}
        </p>
        <div class="saved-tags">
          <span>{terrainLabels[run.terrain]}</span><span>{run.days} days</span>
        </div>
        <OutcomeBars outcomes={run.outcomes} />
        <div class="saved-actions">
          <button class="link-button" onclick={() => open(run.id)}
            >Open result <ArrowUpRight size={14} /></button
          ><button
            class="icon-button"
            aria-label={`Export ${run.name}`}
            onclick={() => exportSaved(run.id)}><Download size={15} /></button
          ><button
            class="icon-button"
            aria-label={`Remove ${run.name}`}
            onclick={() => (pendingDelete = run.id)}
            ><Trash2 size={15} /></button
          >
        </div>
        {#if pendingDelete === run.id}<div class="delete-confirm">
            <p>
              Remove this run from your workspace? Export it first if you want
              to keep a copy.
            </p>
            <button class="light-button" onclick={() => (pendingDelete = '')}
              >Keep it</button
            ><button class="danger-button" onclick={() => remove(run.id)}
              >Remove run</button
            >
          </div>{/if}
      </article>{/each}
  </div>{/if}
{#if compared.length === 2}<section class="panel compare-results">
    <div class="panel-heading">
      <div>
        <h2>Compare the scenarios</h2>
        <span
          >Different inputs can produce different model shares; they are not
          comparable forecasts.</span
        >
      </div>
    </div>
    <div class="two-cols">
      {#each compared as run}<article>
          <p class="eyebrow">{date(run.createdAt)}</p>
          <h3>{run.name}</h3>
          <p>
            {run.result.request.days} days · {run.result.request.trials} trials ·
            seed {run.result.request.seed}
          </p>
          <OutcomeBars outcomes={run.result.outcomes} />
          <dl>
            <div>
              <dt>Terrain</dt>
              <dd>{terrainLabels[run.result.request.terrain]}</dd>
            </div>
            <div>
              <dt>Readiness A / B</dt>
              <dd>
                {run.result.request.readinessA} / {run.result.request
                  .readinessB}
              </dd>
            </div>
            <div>
              <dt>Supply A / B</dt>
              <dd>
                {run.result.request.supplyA} / {run.result.request.supplyB}
              </dd>
            </div>
            <div>
              <dt>Diplomatic pressure</dt>
              <dd>{run.result.request.diplomacy}%</dd>
            </div>
            <div>
              <dt>Climate stress</dt>
              <dd>{run.result.request.climateStress}%</dd>
            </div>
          </dl>
        </article>{/each}
    </div>
  </section>{/if}
