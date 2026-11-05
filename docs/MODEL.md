# Aggregate resource model v1

The engine is a transparent educational model with hand-chosen coefficients. It is **not trained, fitted or calibrated to predict military outcomes**. Historical data provides context and a dispersion input; it is not a training set for a war predictor.

## Inputs

- Two distinct current countries/economies with positive published military expenditure and personnel observations. Null or zero values are not replaced with invented strength scores.
- Readiness and supply: user-set 20–100 assumptions for each side.
- Generic terrain: open, mountainous, urban, desert or coastal. Climate stress and diplomatic pressure: user-set 0–100.
- Horizon: 7–180 days. Trials: 100–2,000. Seed: 0–2,147,483,647.

Resource snapshots retain actual years. A 2020 personnel value is an older observation, even if downloaded today. Military expenditure in current USD is affected by exchange rates and national accounting; headcount does not encode training, equipment quality or readiness.

## Initial indices

For a positive resource pair `a, b`, define the pair-relative share:

```text
share(a,b) = sqrt(a) / (sqrt(a) + sqrt(b))
resourceShareA = 0.55 * spendingShareA + 0.45 * personnelShareA
initialA = 100 * (0.65 * resourceShareA
                  + 0.20 * readinessShareA
                  + 0.15 * supplyShareA)
initialB = 100 - initialA
```

The square root compresses large scale differences. All weights are illustrative choices. The index is in arbitrary units and is not a count of personnel or equipment.

## Environment strategies

| Terrain | Shared friction | Initial dispersion addition |
| --- | --- | --- |
| Open | 1.00 | 0.12 |
| Mountain | 1.65 | 0.27 |
| Urban | 1.50 | 0.30 |
| Desert | 1.40 | 0.22 |
| Coastal | 1.30 | 0.24 |

These strategies apply to both sides. They do not locate a battlefield, choose a route or infer terrain from an entire country. Natural Earth landform polygons are context only, not an elevation model or an on-the-ground survey.

## Historical dispersion

Use up to ten most recent observed country-years. Compute the population standard deviation of `log(1 + reported annual fatalities)`, divide by 5 and cap at 1. Fewer than three observed years use a declared neutral value of 0.5. Missing years are not inserted as zero.

```text
sigma = 0.10 + terrainDispersion + 0.18 * historicalVariability
initialTrialIndex = initialIndex * exp(N(0,1)*sigma - 0.5*sigma²)
```

This measures variation in recorded violence within borders, not combat experience or effectiveness. More historical violence does not directly improve a country’s resource share. The relationship to uncertainty is a modelling assumption.

## Daily sampling

```text
commonFriction = terrainFriction * (1 + climateStress/150) * U(0.85,1.15)
index *= exp(-0.0035 * commonFriction * (1.5 - supply/100) + N(0,1)*0.009)
strain = min(100, strain + 0.20 * commonFriction * (2 - (supplyA+supplyB)/200))
dailyStoppingChance = (diplomacy/100) * 0.012
```

Stopped trials freeze their indices/strain for the rest of the horizon and are classified as de-escalation. This stopping chance is an explicit assumption, not a measured daily ceasefire probability. The strain index is an arbitrary model indicator, not an economic-loss or humanitarian-impact estimate.

## Classification and bands

- Continuing trial with final `indexA > 1.35 * indexB`: side A model advantage.
- Continuing trial with final `indexB > 1.35 * indexA`: side B model advantage.
- Other continuing trial: prolonged contest.
- Stopped trial: de-escalation.

Shares are counts divided by trials and exhaust the sample. They are not validated probabilities of real-world outcomes. They must not be interpreted as a predicted winner.

For each day, sort the sampled indices and strain values. P10 uses `floor((n-1)*0.1)`, median uses integer `(n-1)/2`, and P90 uses `ceil((n-1)*0.9)`. Bands are order statistics describing sampled model variation, not calibrated confidence intervals.

## Reproducibility and limits

`java.util.Random(seed)` drives sampling. Identical resolved inputs, seed and engine version reproduce the numerical timeline and classifications; the generation timestamp/run identifier differ. JSON exports preserve resolved inputs, observation years, variability, request and model version. Importing inputs into the UI deliberately uses the current snapshot, so it may differ after data/model updates.

No weapons inventory, force deployments, manoeuvres, targeting, terrain raster, alliance interventions, nuclear escalation, casualty model or territorial-control model is implemented. GDP/population are contextual profile indicators and are not inputs to the v1 simulation. No predictive-accuracy claim or historical backtest is supplied. Substantive forecasting would require a defensible empirical model, independent validation and a different uncertainty treatment.
