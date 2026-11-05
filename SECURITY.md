# Security and operational scope

This is an educational research application with browser-scoped workspaces. It has not undergone an independent security assessment.

- Bind to loopback by default; use HTTPS and intentional access controls for shared deployments.
- Workspace cookies contain 256 random bits, are HttpOnly/SameSite=Strict and become Secure when the server sees HTTPS. Only the SHA-256 token hash scopes database records. Losing the cookie loses access to the workspace; this is not named-user authentication.
- Same-origin checks cover API writes. Database access uses prepared parameters. Requests are validated, simulation size/concurrency is bounded and write requests are rate limited. The 64 KB declared request-length guard is an initial application check; enforce a strict body/connection limit at the reverse proxy for public hosting, including chunked bodies.
- Backend source URLs are fixed. Fetches have time/size bounds; compressed feeds are bounded after decompression. RSS disables DTD/external entities and allows only HTTPS article links on the official news host. UI values are rendered as text, not untrusted HTML.
- HTML and API responses use `no-store`; UI assets are hashed. Security headers restrict framing, scripts, workers, network connections and device permissions. Maps, fonts and source snapshots are served locally.
- Local H2/PostgreSQL data and backups are operator-controlled, unencrypted application storage. Do not expose database files or the source checkout through a web server. The H2 console is disabled and its optional module is excluded.

Public deployments need appropriate authentication, database/backups, retention, monitoring and shared rate limiting if multiple instances are used. Browser-cookie separation is not a replacement for a user-account system.

Frontend production audit (5 November 2026): zero known advisories. Two low-severity development-tool entries remain from the transitive `deepmerge` advisory [GHSA-gcqx-w48m-w2h4](https://github.com/advisories/GHSA-gcqx-w48m-w2h4); a compatible patched release was not available. MapLibre uses the patched 6.16 release line. This is not a comprehensive audit of all Java or container dependencies.

Report vulnerabilities privately to the repository owner. Do not post private workspace cookies or exported private research in public issues.
