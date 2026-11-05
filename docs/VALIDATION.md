# Verification — 5 November 2026

Local environment: Windows, Temurin JDK 25.0.1 compiling with Java 21 compatibility, Node 24.18.0 and Python 3.14. CI targets Temurin 21 and Node 24 on Ubuntu.

## Automated checks

- **18 Java tests passed:** eight engine tests, seven API/application tests and three reporting-parser tests. Covers seeded reproducibility, sample-band ordering, terrain/diplomacy sensitivity, missing resources, input bounds, persistence/workspace isolation, origin protection, cache headers, offline startup and RSS link/date/entity handling.
- **Six data-contract tests passed:** country-key reconciliation, category totals, flagged inconsistent intervals, period/event-count agreement, historical predecessors, bundled geography and preserved observation years. Tests use bundled snapshots without network calls.
- **Frontend check:** zero Svelte/TypeScript errors or warnings. Production build passed. MapLibre is loaded as a separate map/worker bundle; its size warning is expected and is not hidden.
- **Package integrity passed:** 31 built assets and data files matched the JAR byte for byte.
- **Production frontend dependency audit:** zero known advisories. Two low-severity build-tool entries remain from `deepmerge`; see SECURITY.md. This does not constitute a full Java/container security audit.

## Browser checks against the packaged JAR

- The root page, MapLibre world map, year selection and physical-landform toggle worked. The map uses bundled geometry and a locally packaged worker, with no map-service key.
- Two scenarios were run/saved through the interface, with different terrain/diplomacy assumptions. The resulting shares, trajectory charts, saved library and side-by-side comparison were visible.
- Saved runs survived a process restart. Country profiles showed actual resource years, historical observations, source-quality notices and comparisons.
- Official UN reporting refreshed successfully with correctly parsed publication dates and visible freshness state. The old cached HTML/worker issue found during development was fixed with packaged worker URLs and root/cache-header handling.
- Desktop at 1440 × 1000 and phone at 390 × 844 were inspected. Phone overview, navigation, country and saved-result views had no horizontal document overflow. The collapsed mobile navigation is inert; the refresh icon retains an accessible name. JSON result export was downloaded successfully. Charts have table alternatives; the full accessibility of every control has not been independently audited.

## Limits

No forecasting-accuracy claim is made. The model is uncalibrated and has no historical outcome backtest. Terrain presets are generic assumptions, not a raster-derived ground model. Resource observations can be old; personnel is frequently from 2020.

SVG fallback code is supplied, but a forced WebGL-unavailable browser run was not exercised. Input-import and CSV-export controls are implemented but were not exercised through the browser; no external user files were uploaded. PostgreSQL deployment and the Docker recipe were not run locally. Real reverse-proxy configuration and public/shared hosting require deployment verification. No production service was deployed.
