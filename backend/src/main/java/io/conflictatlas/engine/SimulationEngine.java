package io.conflictatlas.engine;

import io.conflictatlas.domain.Models.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class SimulationEngine {
  public static final String VERSION = "aggregate-resource-v1";

  public SimulationResult simulate(ScenarioRequest request, ResolvedInputs inputs) {
    if (request.countryA().equals(request.countryB()))
      throw new IllegalArgumentException("Choose two different countries.");
    TerrainModel terrain = TerrainModel.of(request.terrain());
    double spendingA = metric(inputs.sideA(), "spending"),
        spendingB = metric(inputs.sideB(), "spending");
    double personnelA = metric(inputs.sideA(), "personnel"),
        personnelB = metric(inputs.sideB(), "personnel");
    // Pair-relative shares avoid interpreting monetary input measures as measured combat
    // capability.
    double spendingShare = share(spendingA, spendingB),
        personnelShare = share(personnelA, personnelB);
    double baseShare = .55 * spendingShare + .45 * personnelShare;
    double readinessShare = share(request.readinessA(), request.readinessB());
    double supplyShare = share(request.supplyA(), request.supplyB());
    double initialA = 100 * (.65 * baseShare + .20 * readinessShare + .15 * supplyShare);
    double initialB = 100 - initialA;
    double uncertaintyA = .10 + terrain.variability() + .18 * inputs.historicalVariabilityA();
    double uncertaintyB = .10 + terrain.variability() + .18 * inputs.historicalVariabilityB();
    double[][] a = new double[request.days() + 1][request.trials()],
        b = new double[request.days() + 1][request.trials()],
        strain = new double[request.days() + 1][request.trials()];
    int aCount = 0, bCount = 0, prolonged = 0, deescalation = 0;
    Random random = new Random(request.seed());
    double climate = 1 + request.climateStress() / 150.0;
    for (int trial = 0; trial < request.trials(); trial++) {
      double levelA =
          initialA
              * Math.exp(random.nextGaussian() * uncertaintyA - .5 * uncertaintyA * uncertaintyA);
      double levelB =
          initialB
              * Math.exp(random.nextGaussian() * uncertaintyB - .5 * uncertaintyB * uncertaintyB);
      double pressure = 0;
      boolean stopped = false;
      for (int day = 0; day <= request.days(); day++) {
        if (day > 0 && !stopped) {
          double common = terrain.friction() * climate * (.85 + .30 * random.nextDouble());
          levelA *=
              Math.exp(
                  -.0035 * common * (1.5 - request.supplyA() / 100.0)
                      + random.nextGaussian() * .009);
          levelB *=
              Math.exp(
                  -.0035 * common * (1.5 - request.supplyB() / 100.0)
                      + random.nextGaussian() * .009);
          pressure =
              Math.min(
                  100,
                  pressure + .20 * common * (2 - (request.supplyA() + request.supplyB()) / 200.0));
          // This is an explicit scenario assumption, not an empirically estimated ceasefire rate.
          if (random.nextDouble() < (request.diplomacy() / 100.0) * .012) stopped = true;
        }
        a[day][trial] = round(levelA);
        b[day][trial] = round(levelB);
        strain[day][trial] = round(pressure);
      }
      if (stopped) deescalation++;
      else if (levelA > levelB * 1.35) aCount++;
      else if (levelB > levelA * 1.35) bCount++;
      else prolonged++;
    }
    List<TimelinePoint> timeline = new ArrayList<>();
    for (int day = 0; day <= request.days(); day++)
      timeline.add(new TimelinePoint(day, band(a[day]), band(b[day]), band(strain[day])));
    List<Factor> factors =
        List.of(
            new Factor(
                "Spending share",
                100 * spendingShare,
                100 * (1 - spendingShare),
                "Latest published military expenditure in current USD; 55% of resource proxy."),
            new Factor(
                "Personnel share",
                100 * personnelShare,
                100 * (1 - personnelShare),
                "Latest published armed-forces personnel; 45% of resource proxy."),
            new Factor(
                "Readiness assumption",
                request.readinessA(),
                request.readinessB(),
                "User-set assumption, not an observed readiness score."),
            new Factor(
                "Supply assumption",
                request.supplyA(),
                request.supplyB(),
                "User-set sustainment assumption, not a route or logistics assessment."),
            new Factor(
                "Historical variability",
                100 * inputs.historicalVariabilityA(),
                100 * inputs.historicalVariabilityB(),
                "Variability of log(1 + fatalities) over up to ten observed country-years; widens"
                    + " uncertainty only."));
    List<String> notes =
        new ArrayList<>(
            List.of(
                "Illustrative Monte Carlo scenario shares, not calibrated probabilities of war"
                    + " outcomes. No winner, casualty forecast, territorial gain or attack plan is"
                    + " predicted.",
                terrain.description()
                    + " Terrain and climate are user-assigned assumptions, not automatically"
                    + " inferred from the map.",
                "Spending and headcount are incomplete resource proxies. Equipment quality,"
                    + " alliances, intelligence, doctrine, morale and nuclear escalation are not"
                    + " modelled.",
                "Historical observations are violence within country borders, not evidence of"
                    + " military effectiveness. Historical context changes dispersion, not the"
                    + " resource balance.",
                "P10–P90 bands describe the model's sampled range, not validated real-world"
                    + " confidence intervals. Classification uses a 1.35 index ratio; other"
                    + " surviving trials are a prolonged contest."));
    for (Country c : List.of(inputs.sideA(), inputs.sideB()))
      for (String key : List.of("spending", "personnel")) {
        Metric value = c.metrics().get(key);
        if (value.year() < java.time.Year.now().getValue() - 2)
          notes.add(
              c.info().name()
                  + ": "
                  + key
                  + " observation is from "
                  + value.year()
                  + "; it is not a current strength assessment.");
      }
    return new SimulationResult(
        VERSION,
        Instant.now().toString(),
        request,
        inputs,
        new Outcomes(
            pct(aCount, request.trials()),
            pct(bCount, request.trials()),
            pct(prolonged, request.trials()),
            pct(deescalation, request.trials())),
        timeline,
        factors,
        notes);
  }

  private static double metric(Country c, String key) {
    Metric m = c.metrics().get(key);
    if (m == null || m.value() <= 0)
      throw new IllegalArgumentException(
          "Published "
              + key
              + " data is unavailable for "
              + c.info().name()
              + ". Choose another country; missing data is never invented.");
    return m.value();
  }

  private static double share(double a, double b) {
    return Math.sqrt(a) / (Math.sqrt(a) + Math.sqrt(b));
  }

  private static double pct(int count, int total) {
    return count * 100.0 / total;
  }

  private static double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static Band band(double[] values) {
    Arrays.sort(values);
    return new Band(
        values[(int) Math.floor((values.length - 1) * .1)],
        values[(values.length - 1) / 2],
        values[(int) Math.ceil((values.length - 1) * .9)]);
  }
}
