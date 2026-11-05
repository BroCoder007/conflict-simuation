<script lang="ts">
  import type { TimelinePoint } from '../types';
  let { points }: { points: TimelinePoint[] } = $props();
  let max = $derived(
    Math.max(100, ...points.flatMap((p) => [p.sideA.p90, p.sideB.p90]))
  );
  const x = (d: number) => 45 + (d / (points.at(-1)?.day || 1)) * 710;
  const y = (v: number) => 210 - (v / max) * 175;
  function line(key: 'sideA' | 'sideB') {
    return points
      .map((p, i) => `${i ? 'L' : 'M'}${x(p.day)},${y(p[key].median)}`)
      .join(' ');
  }
  function band(key: 'sideA' | 'sideB') {
    return (
      points
        .map((p, i) => `${i ? 'L' : 'M'}${x(p.day)},${y(p[key].p90)}`)
        .join(' ') +
      ' ' +
      [...points]
        .reverse()
        .map((p) => `L${x(p.day)},${y(p[key].p10)}`)
        .join(' ') +
      ' Z'
    );
  }
</script>

<svg
  viewBox="0 0 800 255"
  class="chart simulation-chart"
  role="img"
  aria-label="Illustrative remaining resource-index trajectories. Lines are trial medians; bands are P10–P90 sampled ranges. This is not a casualty forecast."
  >{#each [0, 0.5, 1] as tick}<line
      x1="45"
      x2="755"
      y1={y(max * tick)}
      y2={y(max * tick)}
      stroke="#dce2dc"
    /><text x="35" y={y(max * tick) + 4} text-anchor="end"
      >{Math.round(max * tick)}</text
    >{/each}<path d={band('sideA')} fill="#59aab4" opacity=".16" /><path
    d={band('sideB')}
    fill="#bf895b"
    opacity=".16"
  /><path
    d={line('sideA')}
    fill="none"
    stroke="#438f9a"
    stroke-width="2.5"
  /><path
    d={line('sideB')}
    fill="none"
    stroke="#b77f4d"
    stroke-width="2.5"
  />{#each [0, 0.25, 0.5, 0.75, 1] as tick}<text
      x={x((points.at(-1)?.day || 1) * tick)}
      y="238"
      text-anchor="middle"
      >Day {Math.round((points.at(-1)?.day || 1) * tick)}</text
    >{/each}</svg
>
