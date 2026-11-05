# Data, attribution and quality

This application bundles country-scale aggregates and reference geography. It does not distribute precise conflict-event coordinates. The original download URLs and SHA-256 receipts are recorded in `backend/src/main/resources/data/manifest.json` and displayed in the interface. Receipts hash the decoded download payload, not compressed HTTP transport bytes.

## UCDP

- [Country-Year Dataset on Organized Violence within Country Borders, version 26.1](https://ucdp.uu.se/downloads/): 1989–2025. The snapshot preserves the reported best/low/high totals and state-based, non-state and one-sided breakdowns.
- [Candidate Events Dataset, version 26.01.26.09](https://ucdp.uu.se/downloads/): January–September 2026. Events are deduplicated by ID and aggregated by source country and reported start month. An event spanning months is assigned to its start month; monthly aggregates are not exact daily fatality histories.
- Licence: **CC BY 4.0**. Candidate data is provisional and may be revised. The concept of organized violence includes more than interstate wars; these are not a real-time inventory of active wars.

Required source citations:

- Davies, Shawn; Therése Pettersson; Magnus Öberg (2026). *Organized violence 1989–2025, and violent political protests*. Journal of Peace Research. [DOI](https://doi.org/10.1093/jopres/xjag046).
- Sundberg, Ralph and Erik Melander (2013). *Introducing the UCDP Georeferenced Event Dataset*. Journal of Peace Research 50(4).
- Hegre, Håvard; Mihai Croicu; Kristine Eck; Stina Högbladh (2020). *Introducing the UCDP Candidate Events Dataset*. Research & Politics.

UCDP is part of and funded by DEMSCORE, national research infrastructure grant 2021-00162 from the Swedish Research Council.

### Mapping and inconsistencies

Explicit aliases reconcile source names to country identifiers. Pre-1992 Soviet Union observations, pre-2006 Yugoslavia/predecessor observations, Czechoslovakia, East Germany and pre-unification Yemen series are kept as separate historical entities rather than silently mapped to present countries. This does not resolve every historical boundary change. Original country-year names remain in the exported table.

Ten country-period aggregates in this pinned snapshot contain a published interval inconsistent with the best estimate. The quality register preserves the original numbers and identifies each case. Such annual intervals are omitted from chart shading. Best estimates remain visible as published; global or country aggregates can still include these observations. No “corrected” totals are invented.

No candidate observations is not evidence that no violence occurred. Missing annual rows are not automatically zero-filled. Source methodology, reporting coverage and revisions limit comparability.

## World Bank and original indicator providers

The [World Bank Indicators API](https://datahelpdesk.worldbank.org/knowledgebase/articles/889392) supplies the latest non-null observation per indicator and economy (`mrnev=1`). The app preserves the year and indicator code:

| Code | Indicator | Role in this app |
| --- | --- | --- |
| `MS.MIL.XPND.CD` | Military expenditure, current USD — SIPRI | Resource proxy |
| `MS.MIL.TOTL.P1` | Armed-forces personnel, total | Resource proxy; can include paramilitary forces |
| `NY.GDP.MKTP.CD` | GDP, current USD | Context only |
| `SP.POP.TOTL` | Population, total | Context only |

Years can differ across countries and indicators. Many personnel observations in this snapshot are from **2020** and are visibly marked as older observations. A recent retrieval timestamp does not make a historical value current.

Information from the Stockholm International Peace Research Institute (SIPRI) Military Expenditure Database: [https://doi.org/10.55163/CQGC9685](https://doi.org/10.55163/CQGC9685). Spending is an input measure, not measured military effectiveness; see [SIPRI methods](https://www.sipri.org/databases/milex/sources-and-methods).

World Bank data retains original-provider terms. The small latest-observation SIPRI excerpt is provided for educational, non-commercial analysis, with attribution. It is not relicensed under MIT or a blanket CC BY grant. Review [SIPRI terms](https://www.sipri.org/about/terms-and-conditions) before redistribution or commercial use; the fair-use policy has purpose and dataset-size limits.

## Geography

[Natural Earth](https://www.naturalearthdata.com/about/terms-of-use/) 1:110m country boundaries and selected major geographic regions are public domain. Files originate from the project’s [official vector repository](https://github.com/nvkelso/natural-earth-vector). Attributes are reduced to display identifiers/names; source names and boundary conventions are retained. Landforms are broad regions, not complete terrain classification, elevation, vegetation, urban density or ground truth. Country markers use reference/label coordinates, not reported event positions.

## Current reporting

The [official UN News RSS feed](https://news.un.org/feed/subscribe/en/news/all/rss.xml), linked by [UN Geneva’s RSS directory](https://www.ungeneva.org/en/news-media/rss), supplies headline, link and publication date. Topic words select conflict/humanitarian reporting. Selection is heuristic and can include or omit relevant stories. Reporting is not a complete conflict inventory or an independent real-time verification service.

Only short headline/link metadata is retained; full articles, imagery and descriptions are not copied. Ownership remains with the UN/source publisher. News dates are normalized to UTC. The parser disables DTD/external entities, accepts HTTPS article links on `news.un.org`, and skips malformed/duplicate entries.

## Refresh behaviour

Bundled snapshots support offline use. News refreshes after startup and every 15 minutes; resources refresh after 24 hours. A manual request can update both, subject to a ten-minute cooldown. Network timeouts, response-size limits and last-good persistence prevent provider failures from silently replacing data with empty results. UCDP release upgrades require a deliberate snapshot rebuild and application rebuild.
