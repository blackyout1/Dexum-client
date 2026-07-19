package wtf.dexum.utility;

public class Timer {
    private long lastMS = System.currentTimeMillis();

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasPassed(double milliseconds) {
        return System.currentTimeMillis() - lastMS >= milliseconds;
    }
}