package vmalloc;

/**
 * Singleton class for a clock used to count elapsed time.
 * @author Miguel Terra-Neves
 */
public class Clock {

    /**
     * The single instance of the clock.
     */
    private static Clock instance = null;
    
    /**
     * The time instant to be used as the origin when counting elapsed time.
     */
    private long start;
    
    /**
     * Creates an instance of a clock.
     */
    private Clock() { }
    
    /**
     * Retrieves the clock instance.
     * @return The clock instance.
     */
    public static Clock getInstance() {
        if (instance == null) {
            instance = new Clock();
        }
        return instance;
    }
    
    /**
     * Resets the clock. The current time instant becomes 0.
     */
    public void reset() { this.start = System.nanoTime(); }
    
    /**
     * Retrieves the elapsed time since the last call to {@link #reset()}. Assumes that {@link #reset()} was
     * called at least once.
     * @return The elapsed time.
     */
    public double getElapsed() { return (double)(System.nanoTime() - this.start) / 1000000000.0; }
    
}
