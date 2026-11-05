package io.conflictatlas.web;

import io.conflictatlas.data.*;
import io.conflictatlas.domain.Models.*;
import io.conflictatlas.engine.SimulationEngine;
import io.conflictatlas.persistence.RunRepository;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.security.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public final class AtlasController {
  private final DataCatalog catalog;
  private final SourceService sources;
  private final SimulationEngine engine;
  private final RunRepository runs;
  private final Semaphore capacity = new Semaphore(2);

  public AtlasController(
      DataCatalog catalog, SourceService sources, SimulationEngine engine, RunRepository runs) {
    this.catalog = catalog;
    this.sources = sources;
    this.engine = engine;
    this.runs = runs;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "model", SimulationEngine.VERSION);
  }

  @GetMapping("/catalog")
  public Map<String, Object> catalog() {
    return Map.of(
        "countries",
        catalog.countries(),
        "resources",
        catalog.resources(),
        "manifest",
        catalog.manifest());
  }

  @GetMapping("/history")
  public List<HistoryRow> history() {
    return catalog.history();
  }

  @GetMapping("/candidate")
  public List<CandidateRow> candidate() {
    return catalog.candidate();
  }

  @GetMapping("/countries/{id}")
  public Map<String, Object> country(@PathVariable String id) {
    return Map.of(
        "country",
        catalog.country(id),
        "history",
        catalog.history(id),
        "variability",
        catalog.variability(id));
  }

  @GetMapping("/news")
  public NewsSnapshot news() {
    return sources.news();
  }

  @GetMapping("/sources")
  public Map<String, Object> sources() {
    return sources.status();
  }

  @PostMapping("/sources/refresh")
  public ResponseEntity<Map<String, Object>> refresh() {
    sources.refresh(true);
    return ResponseEntity.accepted().body(sources.status());
  }

  @GetMapping("/runs")
  public List<RunSummary> runs(HttpServletRequest req, HttpServletResponse res) {
    return runs.list(owner(req, res));
  }

  @GetMapping("/runs/{id}")
  public SavedRun run(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
    return runs.find(owner(req, res), id)
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found in this workspace."));
  }

  @DeleteMapping("/runs/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
    if (!runs.delete(owner(req, res), id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/runs")
  public ResponseEntity<SavedRun> simulate(
      @Valid @RequestBody ScenarioRequest input, HttpServletRequest req, HttpServletResponse res) {
    if (!capacity.tryAcquire())
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "The simulation engine is busy. Please try again.");
    try {
      Country a = catalog.country(input.countryA()), b = catalog.country(input.countryB());
      if (a.info().historical() || b.info().historical())
        throw new IllegalArgumentException(
            "Historical entities are available in the archive, not current-resource scenarios.");
      var resolved =
          new ResolvedInputs(
              a,
              b,
              catalog.variability(a.info().id()),
              catalog.variability(b.info().id()),
              catalog.manifest().path("historyVersion").asText(),
              catalog.resources().fetchedAt());
      return ResponseEntity.status(201)
          .body(runs.save(owner(req, res), engine.simulate(input, resolved)));
    } finally {
      capacity.release();
    }
  }

  private String owner(HttpServletRequest req, HttpServletResponse res) {
    String token =
        Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
            .filter(c -> c.getName().equals("atlas_workspace"))
            .map(Cookie::getValue)
            .filter(t -> t.matches("[a-f0-9]{64}"))
            .findFirst()
            .orElse(null);
    if (token == null) {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      token = HexFormat.of().formatHex(bytes);
      res.addHeader(
          "Set-Cookie",
          ResponseCookie.from("atlas_workspace", token)
              .httpOnly(true)
              .secure(req.isSecure())
              .sameSite("Strict")
              .path("/")
              .maxAge(30 * 86400)
              .build()
              .toString());
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
