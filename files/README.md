# Hotel Occupancy Forecaster (Java)

A simple base project that forecasts daily hotel occupancy using **Holt-Winters
(triple exponential smoothing)** with weekly seasonality, then turns the forecast
into demand levels and revenue-management suggestions.

Requires **JDK 17+**. No external libraries.

## Files
| File | Purpose |
|------|---------|
| `OccupancyRecord.java` | One day of data (date, rooms sold, total rooms) |
| `DataLoader.java` | Reads a CSV or generates synthetic demo data |
| `HoltWinters.java` | Level + trend + weekly seasonal model, auto-tuned alpha/beta/gamma |
| `HotelOccupancyForecaster.java` | Main: backtest, forecast table, recommendations |
| `sample_data.csv` | 180 days of example history (100-room hotel) |

## Run
```bash
./run.sh                                             # synthetic demo data
./run.sh --csv sample_data.csv --rooms 100 --days 14 # your own data
```
Or manually:
```bash
mkdir out && javac -d out src/*.java
java -cp out HotelOccupancyForecaster --csv sample_data.csv --rooms 100
```

## CSV format
```
date,rooms_sold
2026-01-01,64
2026-01-02,71
```
Consecutive daily rows, at least 14 days (a year or more works best).

## What you get
- **Backtest**: last 14 days held out; Holt-Winters vs. a seasonal-naive baseline (MAE / MAPE)
- **Forecast**: rooms, occupancy %, ~95% range, demand level (LOW / MEDIUM / HIGH)
- **Action hint**: promote on low days, raise rates on high-demand days

## Ideas to extend
- Add holidays/events and weather as features (e.g. regression on Holt-Winters residuals)
- Forecast by room type, channel or booking lead-time (pickup curves)
- Add ADR/RevPAR and dynamic pricing rules
- Expose a REST API with Spring Boot and store history in Azure SQL
- Compare with ARIMA / gradient boosting models
