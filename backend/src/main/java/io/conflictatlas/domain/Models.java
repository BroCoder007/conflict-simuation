package io.conflictatlas.domain;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public final class Models {
  private Models() {}

  public record CountryInfo(
      String id,
      String name,
      String region,
      double latitude,
      double longitude,
      boolean historical) {}

  public record Metric(double value, int year, String indicator) {}

  public record Country(CountryInfo info, Map<String, Metric> metrics, String fetchedAt) {}

  public record HistoryRow(
      String country,
      String sourceName,
      int year,
      long best,
      long low,
      long high,
      long stateBased,
      long nonState,
      long oneSided) {}

  public record CandidateRow(
      String country,
      String month,
      int events,
      long best,
      long low,
      long high,
      long stateBased,
      long nonState,
      long oneSided) {}

  public record NewsItem(String title, String url, String publishedAt, String source) {}

  public record NewsSnapshot(
      String fetchedAt, List<NewsItem> items, boolean stale, String lastError) {}

  public record ResourceSnapshot(String fetchedAt, Map<String, Map<String, Metric>> countries) {}

  public record ScenarioRequest(
      @NotBlank @Size(max = 80) String name,
      @NotNull @Pattern(regexp = "[A-Z]{3}") String countryA,
      @NotNull @Pattern(regexp = "[A-Z]{3}") String countryB,
      @NotNull TerrainKind terrain,
      @Min(7) @Max(180) int days,
      @Min(100) @Max(2000) int trials,
      @Min(0) @Max(2147483647) long seed,
      @Min(20) @Max(100) int readinessA,
      @Min(20) @Max(100) int readinessB,
      @Min(20) @Max(100) int supplyA,
      @Min(20) @Max(100) int supplyB,
      @Min(0) @Max(100) int diplomacy,
      @Min(0) @Max(100) int climateStress) {}

  public enum TerrainKind {
    OPEN,
    MOUNTAIN,
    URBAN,
    DESERT,
    COASTAL
  }

  public record Band(double p10, double median, double p90) {}

  public record TimelinePoint(int day, Band sideA, Band sideB, Band strain) {}

  public record Outcomes(
      double advantageA, double advantageB, double prolonged, double deescalation) {}

  public record Factor(String name, double sideA, double sideB, String basis) {}

  public record ResolvedInputs(
      Country sideA,
      Country sideB,
      double historicalVariabilityA,
      double historicalVariabilityB,
      String conflictVersion,
      String resourceFetchedAt) {}

  public record SimulationResult(
      String modelVersion,
      String generatedAt,
      ScenarioRequest request,
      ResolvedInputs inputs,
      Outcomes outcomes,
      List<TimelinePoint> timeline,
      List<Factor> factors,
      List<String> notes) {}

  public record SavedRun(String id, String createdAt, String name, SimulationResult result) {}

  public record RunSummary(
      String id,
      String createdAt,
      String name,
      String countryA,
      String countryB,
      String terrain,
      int days,
      Outcomes outcomes) {}
}
