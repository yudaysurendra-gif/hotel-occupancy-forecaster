import java.util.Arrays;

/**
 * Additive Holt-Winters (triple exponential smoothing) forecaster.
 * Captures level + trend + a repeating seasonal pattern (weekly, season length = 7).
 * Smoothing parameters are tuned automatically with a small grid search.
 */
public final class HoltWinters {

    private final int season;
    private double alpha, beta, gamma;
    private double level, trend;
    private double[] seasonal;
    private int n;
    private double residualStd;

    public HoltWinters(int season) {
        this.season = season;
    }

    /** Fits the model to the series, tuning alpha/beta/gamma by minimizing one-step-ahead error. */
    public HoltWinters fit(double[] y) {
        if (y.length < 2 * season) {
            throw new IllegalArgumentException("Need at least " + (2 * season) + " data points");
        }
        double bestSse = Double.MAX_VALUE;
        double[] alphas = {0.1, 0.2, 0.3, 0.5, 0.7};
        double[] betas = {0.0, 0.01, 0.05, 0.1};
        double[] gammas = {0.05, 0.1, 0.2, 0.4, 0.6};
        for (double a : alphas)
            for (double b : betas)
                for (double g : gammas) {
                    double sse = run(y, a, b, g, false);
                    if (sse < bestSse) {
                        bestSse = sse;
                        alpha = a; beta = b; gamma = g;
                    }
                }
        double sse = run(y, alpha, beta, gamma, true); // final pass keeps the state
        residualStd = Math.sqrt(sse / (y.length - season));
        n = y.length;
        return this;
    }

    /** Runs the smoothing recursion; returns the SSE of one-step-ahead forecasts. */
    private double run(double[] y, double a, double b, double g, boolean keep) {
        double mean1 = mean(y, 0, season);
        double mean2 = mean(y, season, 2 * season);
        double tr = (mean2 - mean1) / season;
        double[] s = new double[season];
        int seasons = y.length / season;
        for (int i = 0; i < season; i++) {            // average seasonal offset over all full seasons
            double sum = 0;
            for (int k = 0; k < seasons; k++) sum += y[k * season + i] - mean(y, k * season, (k + 1) * season);
            s[i] = sum / seasons;
        }
        double lv = y[0] - s[0];
        double sse = 0;
        for (int t = season; t < y.length; t++) {      // start after first season (warm-up)
            double forecast = lv + tr + s[t % season];
            double err = y[t] - forecast;
            sse += err * err;
            double prev = lv;
            lv = a * (y[t] - s[t % season]) + (1 - a) * (lv + tr);
            tr = b * (lv - prev) + (1 - b) * tr;
            s[t % season] = g * (y[t] - lv) + (1 - g) * s[t % season];
        }
        if (keep) {
            level = lv; trend = tr; seasonal = s;
        }
        return sse;
    }

    /** Point forecasts for the next {@code horizon} days. */
    public double[] forecast(int horizon) {
        double[] out = new double[horizon];
        for (int h = 1; h <= horizon; h++) {
            out[h - 1] = level + h * trend + seasonal[(n + h - 1) % season];
        }
        return out;
    }

    /** Approximate 95% prediction half-width (grows slowly with horizon). */
    public double intervalHalfWidth(int h) {
        return 1.96 * residualStd * Math.sqrt(1 + 0.1 * (h - 1));
    }

    public String params() {
        return String.format("alpha=%.2f beta=%.2f gamma=%.2f", alpha, beta, gamma);
    }

    private static double mean(double[] y, int from, int to) {
        return Arrays.stream(y, from, to).average().orElse(0);
    }
}
