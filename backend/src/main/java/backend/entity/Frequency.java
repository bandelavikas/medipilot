package backend.entity;

public enum Frequency {
    ONCE_DAILY(1),
    TWICE_DAILY(2),
    THREE_TIMES_DAILY(3),
    FOUR_TIMES_DAILY(4);

    private final int dosesPerDay;

    Frequency(int dosesPerDay) {
        this.dosesPerDay = dosesPerDay;
    }

    public int getDosesPerDay() {
        return dosesPerDay;
    }
}