<script lang="ts">
  import { onMount } from 'svelte';
  import {
    Globe2,
    LayoutDashboard,
    FlaskConical,
    MapPinned,
    Layers,
    BookOpen,
    RefreshCw,
    ArrowUpRight,
    Menu,
    ChevronRight
  } from '@lucide/svelte';
  import Overview from './pages/Overview.svelte';
  import CountryExplorer from './pages/CountryExplorer.svelte';
  import ScenarioLab from './pages/ScenarioLab.svelte';
  import Library from './pages/Library.svelte';
  import Methodology from './pages/Methodology.svelte';
  import type {
    Catalog,
    HistoryRow,
    CandidateRow,
    NewsSnapshot,
    SourceStatus,
    RunSummary,
    SavedRun
  } from './types';
  import { api, date } from './lib';
  type View = 'overview' | 'lab' | 'countries' | 'library' | 'method';
  const navigation = [
    { id: 'overview', label: 'Global overview', icon: LayoutDashboard },
    { id: 'lab', label: 'Scenario lab', icon: FlaskConical },
    { id: 'countries', label: 'Country explorer', icon: MapPinned },
    { id: 'library', label: 'Saved scenarios', icon: Layers },
    { id: 'method', label: 'Data & methodology', icon: BookOpen }
  ] as const;
  let view = $state<View>('overview');
  let catalog = $state<Catalog | null>(null);
  let history = $state<HistoryRow[]>([]);
  let candidate = $state<CandidateRow[]>([]);
  let news = $state<NewsSnapshot | null>(null);
  let sources = $state<SourceStatus | null>(null);
  let runs = $state<RunSummary[]>([]);
  let activeRun = $state<SavedRun | null>(null);
  let countryId = $state('UKR');
  let startCountry = $state('');
  let loading = $state(true);
  let error = $state('');
  let notice = $state('');
  let menu = $state(false);
  let compactNavigation = $state(false);
  let watchlist = $state<string[]>([]);
  let refreshing = $state(false);
  function navigate(next: View) {
    view = next;
    location.hash = next;
    menu = false;
    window.scrollTo({ top: 0, behavior: 'instant' });
  }
  function chooseCountry(id: string) {
    if (!catalog?.countries.some((c) => c.id === id)) return;
    countryId = id;
    navigate('countries');
  }
  function chooseLab(id = '') {
    startCountry = id;
    activeRun = null;
    navigate('lab');
  }
  async function refreshRuns() {
    runs = await api<RunSummary[]>('/runs');
  }
  async function created(run: SavedRun) {
    activeRun = run;
    try {
      await refreshRuns();
    } catch {
      notice = 'Run saved. Reopen the library to refresh its list.';
    }
  }
  function toggleWatch(id: string) {
    watchlist = watchlist.includes(id)
      ? watchlist.filter((c) => c !== id)
      : [...watchlist, id];
    localStorage.setItem('atlas-watchlist', JSON.stringify(watchlist));
  }
  async function load() {
    loading = true;
    error = '';
    try {
      const [c, h, a, n, s, r] = await Promise.all([
        api<Catalog>('/catalog'),
        api<HistoryRow[]>('/history'),
        api<CandidateRow[]>('/candidate'),
        api<NewsSnapshot>('/news'),
        api<SourceStatus>('/sources'),
        api<RunSummary[]>('/runs')
      ]);
      catalog = c;
      history = h;
      candidate = a;
      news = n;
      sources = s;
      runs = r;
    } catch (e) {
      error = (e as Error).message;
    } finally {
      loading = false;
    }
  }
  async function refreshSources() {
    refreshing = true;
    notice = '';
    try {
      const previous = sources?.lastAttempt;
      sources = await api<SourceStatus>('/sources/refresh', { method: 'POST' });
      if (
        sources.enabled &&
        !sources.refreshing &&
        sources.lastAttempt === previous &&
        previous
      ) {
        notice =
          'Sources were checked recently. Refresh requests are limited to once every ten minutes.';
        return;
      }
      notice = sources.enabled
        ? 'Refresh requested. Published conflict releases stay pinned; news and resource updates will appear when ready.'
        : 'Automatic and manual source refresh are disabled by server configuration.';
    } catch (e) {
      notice = (e as Error).message;
    } finally {
      refreshing = false;
    }
  }
  onMount(() => {
    const media = window.matchMedia('(max-width: 800px)');
    const updateNavigation = () => {
      compactNavigation = media.matches;
    };
    updateNavigation();
    media.addEventListener('change', updateNavigation);
    const readHash = () => {
      const next = location.hash.slice(1);
      if (navigation.some((n) => n.id === next)) view = next as View;
    };
    readHash();
    window.addEventListener('hashchange', readHash);
    try {
      const stored = JSON.parse(
        localStorage.getItem('atlas-watchlist') || '[]'
      );
      if (Array.isArray(stored))
        watchlist = stored
          .filter((id) => typeof id === 'string' && /^[A-Z]{3}$/.test(id))
          .slice(0, 100);
    } catch {}
    load();
    const timer = setInterval(async () => {
      if (document.hidden || !catalog) return;
      try {
        const [s, n] = await Promise.all([
          api<SourceStatus>('/sources'),
          api<NewsSnapshot>('/news')
        ]);
        if (s.resourcesFetchedAt !== catalog.resources.fetchedAt)
          catalog = await api<Catalog>('/catalog');
        sources = s;
        news = n;
      } catch {
        /* Existing dated observations stay visible while offline. */
      }
    }, 30000);
    return () => {
      clearInterval(timer);
      window.removeEventListener('hashchange', readHash);
      media.removeEventListener('change', updateNavigation);
    };
  });
</script>

<svelte:head
  ><title>{navigation.find((n) => n.id === view)?.label} — Conflict Atlas</title
  ></svelte:head
>
<a class="skip-link" href="#main">Skip to content</a>
<div class="app-shell">
  <aside
    class:open={menu}
    class="sidebar"
    id="atlas-navigation"
    inert={compactNavigation && !menu}
  >
    <a class="brand" href="#overview" onclick={() => navigate('overview')}
      ><span class="brand-icon"><Globe2 size={25} strokeWidth={1.3} /></span
      ><span>CONFLICT<br /><strong>ATLAS</strong></span></a
    >
    <p class="sidebar-label">OBSERVE. COMPARE. UNDERSTAND.</p>
    <nav aria-label="Main navigation">
      {#each navigation as nav}<button
          class:current={view === nav.id}
          onclick={() => {
            if (nav.id === 'lab') {
              startCountry = '';
            }
            navigate(nav.id);
          }}
          ><nav.icon size={17} strokeWidth={1.5} /><span>{nav.label}</span
          >{#if view === nav.id}<ChevronRight size={13} />{/if}</button
        >{/each}
    </nav>
    <div class="sidebar-bottom">
      <div class="workspace-badge">
        <span class="tiny-dot"></span>YOUR RESEARCH WORKSPACE
      </div>
      <p>A clearer view<br />of a complex world.</p>
      <button onclick={() => navigate('method')}
        >Know the sources <ArrowUpRight size={13} /></button
      ><span class="sidebar-version">AN OPEN-SOURCE EXPLORATION</span>
    </div>
  </aside>
  <div class="app-main">
    <header class="topbar">
      <div class="breadcrumb">
        <button
          class="mobile-menu icon-button"
          aria-label="Toggle navigation"
          aria-controls="atlas-navigation"
          aria-expanded={menu}
          onclick={() => (menu = !menu)}><Menu size={20} /></button
        ><Globe2 size={16} /><span>Workspace</span><ChevronRight
          size={12}
        /><strong>{navigation.find((n) => n.id === view)?.label}</strong>
      </div>
      <div class="topbar-right">
        <span class="release-label"
          ><span class="live-dot"></span>Conflict data through Sep 2026</span
        ><button
          class="refresh-button"
          aria-label="Refresh sources"
          onclick={refreshSources}
          disabled={refreshing || sources?.refreshing}
          ><RefreshCw
            size={14}
            class={refreshing || sources?.refreshing ? 'spinning' : ''}
          /><span
            >{sources?.refreshing
              ? 'Refreshing sources'
              : 'Refresh sources'}</span
          ></button
        >
      </div>
    </header>
    <main id="main">
      {#if notice}<div class="notice top-notice" role="status">
          <span>{notice}</span><button
            aria-label="Dismiss message"
            onclick={() => (notice = '')}>×</button
          >
        </div>{/if}{#if loading}<div class="loading-page">
          <Globe2 size={44} />
          <h1>Putting the world in context.</h1>
          <p>Loading the published observations and your workspace…</p>
        </div>{:else if error || !catalog}<section class="large-empty">
          <h1>The workspace couldn’t load.</h1>
          <p>{error || 'The Java service is unavailable.'}</p>
          <p>
            Start the Java backend and try again. The application uses bundled
            data when external providers are offline.
          </p>
          <button class="dark-button" onclick={load}
            >Try again <RefreshCw size={15} /></button
          >
        </section>{:else}{#if view === 'overview'}<Overview
            {catalog}
            {history}
            {candidate}
            {news}
            {watchlist}
            oncountry={chooseCountry}
            onlab={() => chooseLab()}
          />{:else if view === 'countries'}<CountryExplorer
            {catalog}
            {history}
            {candidate}
            id={countryId}
            {watchlist}
            onselect={chooseCountry}
            onwatch={toggleWatch}
            onlab={chooseLab}
          />{:else if view === 'lab'}<ScenarioLab
            {catalog}
            run={activeRun}
            {startCountry}
            oncreated={created}
          />{:else if view === 'library'}<Library
            {catalog}
            {runs}
            onopen={(run) => {
              activeRun = run;
              startCountry = '';
              navigate('lab');
            }}
            onrefresh={refreshRuns}
            onlab={() => chooseLab()}
          />{:else}<Methodology {catalog} {sources} />{/if}
        <footer class="app-footer">
          <span
            >CONFLICT ATLAS <span class="footer-divider">/</span> A research & scenario
            workspace</span
          ><button onclick={() => navigate('method')}
            >Sources, assumptions & limitations <ArrowUpRight
              size={12}
            /></button
          >
        </footer>{/if}
    </main>
  </div>
</div>
