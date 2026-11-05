package io.conflictatlas;

import static org.junit.jupiter.api.Assertions.*;

import io.conflictatlas.domain.Models.*;
import io.conflictatlas.engine.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {
  final SimulationEngine engine = new SimulationEngine();

  Country country(String id, double spending, double personnel) {
    return new Country(
        new CountryInfo(id, id, "Test", 0, 0, false),
        Map.of(
            "spending",
            new Metric(spending, 2025, "test"),
            "personnel",
            new Metric(personnel, 2025, "test")),
        "2026-11-05T00:00:00Z");
  }

  ResolvedInputs inputs() {
    return new ResolvedInputs(
        country("AAA", 1e10, 100000),
        country("BBB", 1e10, 100000),
        .2,
        .2,
        "test",
        "2026-11-05T00:00:00Z");
  }

  ScenarioRequest request(TerrainKind terrain, int diplomacy, long seed) {
    return new ScenarioRequest(
        "Test", "AAA", "BBB", terrain, 60, 1000, seed, 70, 70, 70, 70, diplomacy, 20);
  }

  @Test
  void seededResultsAreReproducible() {
    var one = engine.simulate(request(TerrainKind.OPEN, 30, 42), inputs());
    var two = engine.simulate(request(TerrainKind.OPEN, 30, 42), inputs());
    assertEquals(one.outcomes(), two.outcomes());
    assertEquals(one.timeline(), two.timeline());
  }

  @Test
  void categoriesExhaustTrialsAndBandsAreOrdered() {
    var result = engine.simulate(request(TerrainKind.URBAN, 30, 88), inputs());
    var o = result.outcomes();
    assertEquals(100, o.advantageA() + o.advantageB() + o.prolonged() + o.deescalation(), 1e-8);
    for (var point : result.timeline())
      for (var b : List.of(point.sideA(), point.sideB(), point.strain())) {
        assertTrue(Double.isFinite(b.median()));
        assertTrue(b.p10() <= b.median());
        assertTrue(b.median() <= b.p90());
        assertTrue(b.p10() >= 0);
      }
  }

  @Test
  void environmentChangesFrictionWithoutChangingObservedInputs() {
    var open = engine.simulate(request(TerrainKind.OPEN, 0, 42), inputs());
    var mountain = engine.simulate(request(TerrainKind.MOUNTAIN, 0, 42), inputs());
    assertTrue(
        mountain.timeline().getLast().strain().median()
            > open.timeline().getLast().strain().median());
    assertEquals(open.inputs(), mountain.inputs());
  }

  @Test
  void diplomacyIsAnExplicitStoppingAssumption() {
    assertEquals(
        0, engine.simulate(request(TerrainKind.OPEN, 0, 42), inputs()).outcomes().deescalation());
    assertTrue(
        engine.simulate(request(TerrainKind.OPEN, 100, 42), inputs()).outcomes().deescalation()
            > 0);
  }

  @Test
  void identicalResourcesHaveNoLargeSystematicSideAdvantage() {
    var o = engine.simulate(request(TerrainKind.OPEN, 0, 12), inputs()).outcomes();
    assertTrue(Math.abs(o.advantageA() - o.advantageB()) < 8);
  }

  @Test
  void missingResourceDataIsNeverInvented() {
    var missing =
        new Country(new CountryInfo("BBB", "Missing", "Test", 0, 0, false), Map.of(), "test");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            engine.simulate(
                request(TerrainKind.OPEN, 0, 1),
                new ResolvedInputs(inputs().sideA(), missing, 0, 0, "test", "test")));
  }

  @Test
  void rejectSameCountry() {
    var r =
        new ScenarioRequest(
            "Test", "AAA", "AAA", TerrainKind.OPEN, 30, 100, 1, 70, 70, 70, 70, 0, 0);
    assertThrows(IllegalArgumentException.class, () -> engine.simulate(r, inputs()));
  }

  @Test
  void terrainStrategiesAreDistinct() {
    assertEquals(1, TerrainModel.of(TerrainKind.OPEN).friction());
    assertTrue(TerrainModel.of(TerrainKind.MOUNTAIN).friction() > 1);
    assertTrue(
        TerrainModel.of(TerrainKind.URBAN).variability()
            > TerrainModel.of(TerrainKind.OPEN).variability());
  }
}
