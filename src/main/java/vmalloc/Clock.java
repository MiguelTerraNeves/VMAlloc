/*
 * VMAlloc, Copyright (c) 2018, Miguel Terra-Neves, Vasco Manquinho, Ines Lynce
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
