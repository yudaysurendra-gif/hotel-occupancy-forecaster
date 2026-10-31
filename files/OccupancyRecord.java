import java.time.LocalDate;

/** One day of historical data: how many rooms were sold out of the hotel's inventory. */
public record OccupancyRecord(LocalDate date, int roomsSold, int totalRooms) {

    /** Occupancy as a percentage (0-100). */
    public double occupancyPct() {
        return 100.0 * roomsSold / totalRooms;
    }
}
