# GitHub Pages edition

GitHub Pages serves static files and cannot run the Java service. The Pages edition provides the country map, historical filters, country profiles and source documentation from bundled snapshots. Simulations, saved runs and automatic source refresh require the Java application. The site clearly identifies this limitation and labels the news snapshot.

To rebuild a Pages artifact from the project root:

```sh
mkdir -p frontend/public/data/snapshot
cp backend/src/main/resources/data/*.json frontend/public/data/snapshot/
cd frontend
npm ci
VITE_PAGES=true npm run build
```

On PowerShell, replace the final line with `$env:VITE_PAGES='true'; npm run build`. Publish the contents of `frontend/dist` to the `gh-pages` branch. Reset the environment variable before building the Java app's normal frontend. `frontend/public/data/snapshot` is generated and ignored by Git; the canonical data remains under the backend resources.
