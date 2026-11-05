<script lang="ts">
  import { onMount } from 'svelte';
  import { geoNaturalEarth1, geoPath } from 'd3-geo';
  import type { FeatureCollection, Feature } from 'geojson';
  import type {
    Map as GLMap,
    GeoJSONSource,
    StyleSpecification
  } from 'maplibre-gl';
  import 'maplibre-gl/dist/maplibre-gl.css';
  import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url';
  import type { Aggregate, CountryInfo } from '../types';
  import { count } from '../lib';
  let {
    rows,
    countries,
    selected = '',
    physical = false,
    onselect,
    focus = false
  }: {
    rows: Aggregate[];
    countries: CountryInfo[];
    selected?: string;
    physical?: boolean;
    onselect: (id: string) => void;
    focus?: boolean;
  } = $props();
  let container: HTMLDivElement;
  let map: GLMap | null = null;
  let ready = $state(false);
  let world = $state.raw<FeatureCollection | null>(null);
  let landforms = $state.raw<FeatureCollection | null>(null);
  let fallback = $state(false);
  let failure = $state('');
  let hovered = $state('');
  let data = $derived(new Map(rows.map((r) => [r.country, r])));
  const projection = geoNaturalEarth1().scale(160).translate([500, 270]);
  const path = geoPath(projection);
  function polygonData(): FeatureCollection {
    return {
      type: 'FeatureCollection',
      features: (world?.features || []).map((f) => ({
        ...f,
        properties: {
          ...f.properties,
          best: data.get(String(f.properties?.id))?.best || 0
        }
      }))
    };
  }
  function points(): FeatureCollection {
    return {
      type: 'FeatureCollection',
      features: rows
        .filter((r) => r.best > 0)
        .flatMap((r) => {
          const c = countries.find((c) => c.id === r.country);
          return c && !c.historical && (c.latitude || c.longitude)
            ? [
                {
                  type: 'Feature' as const,
                  geometry: {
                    type: 'Point' as const,
                    coordinates: [c.longitude, c.latitude]
                  },
                  properties: { id: c.id, name: c.name, best: r.best }
                }
              ]
            : [];
        })
    };
  }
  const style: StyleSpecification = {
    version: 8,
    sources: {},
    layers: [
      {
        id: 'ocean',
        type: 'background',
        paint: { 'background-color': '#142226' }
      }
    ]
  };
  $effect(() => {
    rows;
    selected;
    physical;
    world;
    if (ready && map) {
      (map.getSource('countries') as GeoJSONSource)?.setData(polygonData());
      (map.getSource('points') as GeoJSONSource)?.setData(points());
      map.setFilter('selected', ['==', ['get', 'id'], selected]);
      map.setLayoutProperty(
        'landforms',
        'visibility',
        physical ? 'visible' : 'none'
      );
    }
  });
  $effect(() => {
    const id = selected;
    if (focus && ready && map) {
      const c = countries.find((c) => c.id === id);
      if (c && (c.latitude || c.longitude))
        map.flyTo({
          center: [c.longitude, c.latitude],
          zoom: 2.5,
          duration: 700,
          essential: false
        });
    }
  });
  onMount(() => {
    let disposed = false;
    (async () => {
      try {
        const [w, l] = await Promise.all([
          fetch('/data/world.geojson').then((r) => r.json()),
          fetch('/data/landforms.geojson').then((r) => r.json())
        ]);
        if (disposed) return;
        world = w;
        landforms = l;
        const gl = await import('maplibre-gl');
        if (disposed) return;
        gl.setWorkerUrl(workerUrl);
        map = new gl.Map({
          container,
          style,
          center: [15, 15],
          zoom: 0.2,
          minZoom: -1,
          maxZoom: 6,
          attributionControl: false,
          renderWorldCopies: false
        });
        map.addControl(
          new gl.NavigationControl({ showCompass: false }),
          'bottom-right'
        );
        map.on('error', () => {
          fallback = true;
          map?.remove();
          map = null;
        });
        map.on('load', () => {
          if (!map || disposed) return;
          map.addSource('countries', { type: 'geojson', data: polygonData() });
          map.addLayer({
            id: 'countries',
            type: 'fill',
            source: 'countries',
            paint: {
              'fill-color': [
                'interpolate',
                ['linear'],
                ['ln', ['+', ['get', 'best'], 1]],
                0,
                '#28383b',
                5,
                '#3c4943',
                9,
                '#765747',
                12,
                '#b2684d'
              ],
              'fill-opacity': 0.95
            }
          });
          map.addLayer({
            id: 'borders',
            type: 'line',
            source: 'countries',
            paint: {
              'line-color': '#60716e',
              'line-width': 0.65,
              'line-opacity': 0.55
            }
          });
          map.addSource('landforms', { type: 'geojson', data: landforms! });
          map.addLayer({
            id: 'landforms',
            type: 'fill',
            source: 'landforms',
            layout: { visibility: physical ? 'visible' : 'none' },
            paint: {
              'fill-color': '#c8d6ac',
              'fill-opacity': 0.17,
              'fill-outline-color': '#b0c18c'
            }
          });
          map.addLayer({
            id: 'selected',
            type: 'line',
            source: 'countries',
            filter: ['==', ['get', 'id'], selected],
            paint: { 'line-color': '#d4ed9c', 'line-width': 2 }
          });
          map.addSource('points', { type: 'geojson', data: points() });
          map.addLayer({
            id: 'halos',
            type: 'circle',
            source: 'points',
            paint: {
              'circle-radius': [
                'interpolate',
                ['linear'],
                ['ln', ['+', ['get', 'best'], 1]],
                0,
                5,
                12,
                20
              ],
              'circle-color': '#d89a70',
              'circle-opacity': 0.12
            }
          });
          map.addLayer({
            id: 'points',
            type: 'circle',
            source: 'points',
            paint: {
              'circle-radius': [
                'interpolate',
                ['linear'],
                ['ln', ['+', ['get', 'best'], 1]],
                0,
                2,
                12,
                6
              ],
              'circle-color': '#e6ad80',
              'circle-stroke-color': '#f4ce9c',
              'circle-stroke-width': 1,
              'circle-opacity': 0.85
            }
          });
          map.on('click', 'countries', (e) => {
            const id = String(e.features?.[0].properties.id || '');
            if (countries.some((c) => c.id === id)) onselect(id);
          });
          map.on('mousemove', 'countries', (e) => {
            hovered = String(e.features?.[0].properties.name || '');
            if (map) map.getCanvas().style.cursor = 'pointer';
          });
          map.on('mouseleave', 'countries', () => {
            hovered = '';
            if (map) map.getCanvas().style.cursor = '';
          });
          map.fitBounds(
            [
              [-175, -55],
              [180, 80]
            ],
            {
              padding: { top: 40, bottom: 65, left: 30, right: 30 },
              duration: 0
            }
          );
          ready = true;
        });
      } catch (error) {
        fallback = true;
        if (!world)
          failure =
            'Map boundaries could not be loaded. Use the country selector to explore the same data.';
      }
    })();
    return () => {
      disposed = true;
      map?.remove();
      map = null;
    };
  });
</script>

<div
  class="world-map"
  aria-label="World map of published organized violence by country"
>
  <div class="gl-map" bind:this={container} class:hidden={fallback}></div>
  {#if fallback && world}<svg
      class="fallback-map"
      viewBox="0 0 1000 540"
      aria-label="World map fallback"
      role="group"
      >{#each world.features as feature}{@const id = String(
          feature.properties?.id
        )}{@const value = data.get(id)?.best || 0}<path
          d={path(feature) || ''}
          fill={id === selected
            ? '#789b66'
            : value > 1000
              ? '#886046'
              : value > 0
                ? '#5e5848'
                : '#2b3e41'}
          stroke="#61716e"
          stroke-width=".6"
          tabindex="0"
          role="button"
          aria-label={`${feature.properties?.name}, ${count(value)} reported fatalities`}
          onclick={() => onselect(id)}
          onkeydown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onselect(id);
            }
          }}><title>{feature.properties?.name}</title></path
        >{/each}{#if physical && landforms}{#each landforms.features as f}<path
            d={path(f) || ''}
            fill="#a6bc85"
            fill-opacity=".25"
            stroke="#a6bc85"
            stroke-width=".5"
            pointer-events="none"
          />{/each}{/if}{#each countries.filter((c) => !c.historical && (c.latitude || c.longitude) && data.has(c.id)) as c}{@const point =
          projection([c.longitude, c.latitude])}{#if point}<circle
            cx={point[0]}
            cy={point[1]}
            r="3"
            fill="#edb77d"
            pointer-events="none"
          />{/if}{/each}</svg
    >{/if}
  {#if !ready && !fallback}<div class="map-loading">
      Drawing the world…
    </div>{/if}
  {#if failure}<p class="map-loading">{failure}</p>{/if}
  <div class="map-watermark">WORLD / ORGANIZED VIOLENCE</div>
  {#if hovered}<div class="map-hover">{hovered} · Select to explore ↗</div>{/if}
  <div class="map-legend">
    <span class="legend-dot"></span>Country reference points<span
      class="heat-scale"
    ></span>Lower → higher reported fatalities
  </div>
  <div class="map-credit">
    Natural Earth · country aggregates · source-defined boundaries
  </div>
</div>
