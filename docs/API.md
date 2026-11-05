# API

The UI and API share one origin. There is no permissive cross-origin configuration. All writes require an `Origin` header matching the application’s scheme and `Host`. Run endpoints use a random HttpOnly workspace cookie; records are scoped to its hash. Keep the same cookie jar when using an API client.

| Method / path | Purpose |
| --- | --- |
| `GET /api/health` | Process/model health |
| `GET /api/catalog` | Country catalog, latest resource snapshot and conflict manifest |
| `GET /api/history` | Pinned annual country observations |
| `GET /api/candidate` | Country/month candidate aggregates |
| `GET /api/countries/{ISO3}` | Country resources, history and historical variability |
| `GET /api/news` | Selected official reporting, retrieval date, stale/error flags |
| `GET /api/sources` | Source health, update state and pinned conflict metadata |
| `POST /api/sources/refresh` | Request a bounded background news/resource refresh |
| `GET /api/runs` | Current browser workspace’s run summaries |
| `POST /api/runs` | Validate, simulate and save a run |
| `GET /api/runs/{UUID}` | Read a run belonging to this workspace |
| `DELETE /api/runs/{UUID}` | Remove a run belonging to this workspace |

Example request body for `POST /api/runs`:

```json
{
  "name": "Regional resource scenario",
  "countryA": "IND",
  "countryB": "PAK",
  "terrain": "MOUNTAIN",
  "days": 60,
  "trials": 1000,
  "seed": 42,
  "readinessA": 70,
  "readinessB": 70,
  "supplyA": 70,
  "supplyB": 70,
  "diplomacy": 30,
  "climateStress": 20
}
```

A successful run returns 201 and `{id, createdAt, name, result}`. `result` includes request, resolved country/resource inputs, model version, outcome shares, daily P10/median/P90 trajectories, factor explanations and limitations.

Bounds: title 1–80 characters; different current country IDs; terrain `OPEN`, `MOUNTAIN`, `URBAN`, `DESERT`, `COASTAL`; days 7–180; trials 100–2,000; seed 0–2,147,483,647; readiness/supply 20–100; diplomacy/climate 0–100. Missing resource data is rejected rather than estimated. There are at most 50 saved runs per workspace and two concurrent simulations per process. Mutating requests have an IP rate limit of 20 per minute; limit responses use 429 and `Retry-After`.

Invalid inputs return 400. Origin failures return 403. Runs outside the workspace return 404. Source refresh returns 202 even when a recent refresh is already covered by the cooldown; inspect `refreshing`, `lastAttempt` and `enabled`. No client-supplied URL is used for outbound requests.
