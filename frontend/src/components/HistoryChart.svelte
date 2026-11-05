<script lang="ts">
  import type { HistoryRow } from '../types';
  import { count } from '../lib';
  let { rows, name }: { rows: HistoryRow[]; name: string } = $props();
  let sorted = $derived([...rows].sort((a, b) => a.year - b.year));
  let max = $derived(
    Math.max(1, ...sorted.flatMap((r) => [r.high, r.best, r.low]))
  );
  const x = (i: number) => 45 + (i / Math.max(1, sorted.length - 1)) * 710;
  const y = (v: number) => 200 - (v / max) * 165;
  let line = $derived(
    sorted.map((r, i) => `${i ? 'L' : 'M'}${x(i)},${y(r.best)}`).join(' ')
  );
  let band = $derived.by(() => {
    const groups: { row: HistoryRow; index: number }[][] = [];
    let current: { row: HistoryRow; index: number }[] = [];
    sorted.forEach((row, index) => {
      if (row.low <= row.best && row.best <= row.high)
        current.push({ row, index });
      else {
        if (current.length) groups.push(current);
        current = [];
      }
    });
    if (current.length) groups.push(current);
    return groups
      .map(
        (group) =>
          group
            .map(
              ({ row, index }, i) =>
                `${i ? 'L' : 'M'}${x(index)},${y(row.high)}`
            )
            .join(' ') +
          ' ' +
          [...group]
            .reverse()
            .map(({ row, index }) => `L${x(index)},${y(row.low)}`)
            .join(' ') +
          ' Z'
      )
      .join(' ');
  });
</script>

{#if sorted.length}<svg
    viewBox="0 0 800 245"
    class="chart history-chart"
    role="img"
    aria-label={`${name}: annual reported organized-violence fatalities, ${sorted[0].year}–${sorted.at(-1)?.year}. Shading shows published low to high estimates.`}
    ><line
      x1="45"
      x2="755"
      y1="200"
      y2="200"
      stroke="#d8ded6"
    />{#each [0, 0.5, 1] as tick}<text
        x="36"
        y={y(max * tick) + 4}
        text-anchor="end">{count(Math.round(max * tick))}</text
      ><line
        x1="45"
        x2="755"
        y1={y(max * tick)}
        y2={y(max * tick)}
        stroke="#e3e7df"
      />{/each}<path d={band} fill="#d3a37b" opacity=".24" /><path
      d={line}
      fill="none"
      stroke="#a86d46"
      stroke-width="2.5"
    />{#each sorted.filter((_, i) => i % Math.max(1, Math.floor(sorted.length / 6)) === 0 || i === sorted.length - 1) as row}<text
        x={x(sorted.indexOf(row))}
        y="225"
        text-anchor="middle">{row.year}</text
      >{/each}</svg
  >{:else}<div class="empty-small">
    No country-year observations are available for this selection.
  </div>{/if}
