import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Hotel Occupancy Forecaster
 *
 * Usage:
 *   java HotelOccupancyForecaster                       (synthetic demo data)
 *   java HotelOccupancyForecaster --csv data.csv --rooms 120 --days 14
 */
public class HotelOccupancyForecaster {

    private static final int SEASON = 7;

    public static void main(String[] args) throws IOException {
        int totalRooms = 120;
        int horizon = 14;
        String csv = null;
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--csv" -> csv = args[i + 1];
                case "--rooms" -> totalRooms = Integer.parseInt(args[i + 1]);
                case "--days" -> horizon = Integer.parseInt(args[i + 1]);
                default -> { }
            }
        }

        List<OccupancyRecord> history = (csv != null)
                ? DataLoader.fromCsv(Path.of(csv), totalRooms)
                : DataLoader.synthetic(LocalDate.now().minusDays(1), 365, totalRooms, 42L);
        System.out.println(csv != null ? "Loaded " + history.size() + " days from " + csv
                : "No CSV given - using " + history.size() + " days of synthetic demo data");

        double[] occ = history.stream().mapToDouble(OccupancyRecord::occupancyPct).toArray();

        backtest(occ, 14);

        HoltWinters model = new HoltWinters(SEASON).fit(occ);
        double[] fc = model.forecast(horizon);
        LocalDate last = history.get(history.size() - 1).date();

        System.out.println("\nModel: Holt-Winters (additive, weekly)  " + model.params());
        System.out.printf("%nForecast for the next %d days (%d rooms)%n", horizon, totalRooms);
        System.out.println("-".repeat(86));
        System.out.printf("%-12s %-5s %8s %8s %-15s %-10s %s%n",
                "Date", "Day", "Rooms", "Occ %", "95% range", "Demand", "Suggested action");
        System.out.println("-".repeat(86));
        double sumOcc = 0;
        for (int h = 1; h <= horizon; h++) {
            double pct = clamp(fc[h - 1]);
            double hw = model.intervalHalfWidth(h);
            LocalDate d = last.plusDays(h);
            sumOcc += pct;
            System.out.printf("%-12s %-5s %8d %7.1f%% %-15s %-10s %s%n",
                    d, d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    Math.round(pct / 100 * totalRooms), pct,
                    String.format("%.0f%% - %.0f%%", clamp(pct - hw), clamp(pct + hw)),
                    demandLevel(pct), recommendation(pct));
        }
        System.out.println("-".repeat(86));
        System.out.printf("Average forecast occupancy: %.1f%%%n", sumOcc / horizon);
    }

    /** Hold out the last N days, compare Holt-Winters vs. a seasonal-naive baseline. */
    private static void backtest(double[] occ, int holdout) {
        double[] train = java.util.Arrays.copyOf(occ, occ.length - holdout);
        double[] actual = java.util.Arrays.copyOfRange(occ, occ.length - holdout, occ.length);
        double[] hw = new HoltWinters(SEASON).fit(train).forecast(holdout);
        double[] naive = new double[holdout];
        for (int i = 0; i < holdout; i++) naive[i] = train[train.length - SEASON + (i % SEASON)];

        System.out.printf("%nBacktest on last %d days (lower is better):%n", holdout);
        System.out.printf("  Seasonal naive : MAE %.2f pts | MAPE %.1f%%%n", mae(actual, naive), mape(actual, naive));
        System.out.printf("  Holt-Winters   : MAE %.2f pts | MAPE %.1f%%%n", mae(actual, hw), mape(actual, hw));
    }

    static double mae(double[] a, double[] f) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += Math.abs(a[i] - f[i]);
        return s / a.length;
    }

    static double mape(double[] a, double[] f) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += Math.abs((a[i] - f[i]) / a[i]);
        return 100 * s / a.length;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String demandLevel(double pct) {
        if (pct >= 85) return "HIGH";
        if (pct >= 60) return "MEDIUM";
        return "LOW";
    }

    private static String recommendation(double pct) {
        if (pct >= 85) return "Raise rates, close discount channels";
        if (pct >= 60) return "Hold rates, monitor pickup";
        return "Run promotion / target OTA & corporate deals";
    }
}
