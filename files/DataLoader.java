import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Loads history from a CSV file, or generates realistic synthetic data for demos. */
public final class DataLoader {

    private DataLoader() {}

    /**
     * CSV format (header optional): date,rooms_sold   e.g. 2026-01-15,87
     * Rows must be consecutive daily records.
     */
    public static List<OccupancyRecord> fromCsv(Path file, int totalRooms) throws IOException {
        List<OccupancyRecord> out = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            line = line.trim();
            if (line.isEmpty() || Character.isLetter(line.charAt(0))) continue; // skip header
            String[] p = line.split(",");
            int sold = Math.min(totalRooms, Math.max(0, Integer.parseInt(p[1].trim())));
            out.add(new OccupancyRecord(LocalDate.parse(p[0].trim()), sold, totalRooms));
        }
        return out;
    }

    /** Synthetic hotel: weekend peaks, summer/winter season, slight growth, random noise, events. */
    public static List<OccupancyRecord> synthetic(LocalDate end, int days, int totalRooms, long seed) {
        Random rnd = new Random(seed);
        double[] weekday = {-6, -4, -2, 0, 12, 18, 2}; // Mon..Sun occupancy-point offsets
        List<OccupancyRecord> out = new ArrayList<>();
        LocalDate start = end.minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            double yearly = 8 * Math.sin(2 * Math.PI * (d.getDayOfYear() - 80) / 365.0);
            double trend = 0.01 * i;
            double event = (d.getDayOfMonth() == 15 && d.getMonthValue() % 3 == 0) ? 15 : 0; // quarterly event
            double occ = 62 + weekday[d.getDayOfWeek().getValue() - 1] + yearly + trend + event + rnd.nextGaussian() * 3;
            int sold = (int) Math.round(Math.min(100, Math.max(15, occ)) / 100.0 * totalRooms);
            out.add(new OccupancyRecord(d, sold, totalRooms));
        }
        return out;
    }
}
