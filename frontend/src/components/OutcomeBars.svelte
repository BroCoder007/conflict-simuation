<script lang="ts">
  import type { Outcomes } from '../types';
  import { outcomeLabels, outcomeColors } from '../lib';
  let { outcomes }: { outcomes: Outcomes } = $props();
</script>

<div class="outcomes">
  {#each Object.keys(outcomeLabels) as key}{@const k = key as keyof Outcomes}
    <div class="outcome-row">
      <div>
        <span>{outcomeLabels[k]}</span><strong
          >{outcomes[k].toFixed(1)}<small>%</small></strong
        >
      </div>
      <div class="outcome-track">
        <span
          style:width={`${outcomes[k]}%`}
          style:background={outcomeColors[k]}
        ></span>
      </div>
    </div>{/each}
</div>
