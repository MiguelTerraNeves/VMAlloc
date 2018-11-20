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

package vmalloc.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Class with several static utility methods and constants for input/output operations.
 * @author Miguel Terra-Neves
 */
public class IOUtils {

    /**
     * The platform's line separator.
     */
    public static final String NEW_LINE = System.getProperty("line.separator");
    
    /**
     * Reference to the standard output.
     */
    private static final PrintStream STDOUT = System.out;
    
    /**
     * Dummy output stream that writes nothing.
     */
    private static final PrintStream DUMMY_OUT = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {}
    });
    
    /**
     * Disables the standard output. Can be re-enabled with {@link #stdoutEnable()}.
     */
    public static void stdoutDisable() { System.setOut(DUMMY_OUT); }
    
    /**
     * Enables standard output.
     */
    public static void stdoutEnable() { System.setOut(STDOUT); }

    /**
     * Makes a fresh temporary file.
     * @param prefix The prefix for the temporary file. 
     * @param suffix The suffix for the temporary file.
     * @param unique True if the file should have a unique name, false otherwise.
     * @param tmp_dir The directory in which the temporary file should be created.
     * @return The temporary file.
     */
    public static File makeTemporaryFile(String prefix, String suffix, boolean unique, File tmp_dir) {
        if (unique) {
            prefix += "_" + Long.toString(System.currentTimeMillis());
        }
        File tmp_file = null;
        try {
            if (tmp_dir != null) {
                tmp_file = File.createTempFile(prefix, suffix, tmp_dir);
            }
            else {
                tmp_file = File.createTempFile(prefix, suffix);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file " + prefix + suffix, e);
        }
        tmp_file.deleteOnExit();
        return tmp_file;
    }
    
    /**
     * Makes a fresh temporary file in the default temporary directory.
     * @param prefix The prefix for the temporary file. 
     * @param suffix The suffix for the temporary file.
     * @param unique True if the file should have a unique name, false otherwise.
     * @return The temporary file.
     */
    public static File makeTemporaryFile(String prefix, String suffix, boolean unique) {
        return makeTemporaryFile(prefix, suffix, unique, null);
    }
    
}
