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

import java.io.IOException;
import java.util.Properties;

/**
 * Singleton class that functions as a wrapper for obtaining java configuration properties.
 * @author Miguel Terra-Neves
 */
public class Configuration {

    /**
     * The configuration instance.
     */
    private static Configuration instance = null;
    
    /**
     * The java properties object with the configuration.
     */
    private Properties props = null;
    
    /**
     * Creates an instance of a configuration.
     * @throws IOException If an error occurs reading from the configuration properties file.
     */
    private Configuration() throws IOException {
        props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("config.properties"));
    }
    
    /**
     * Retrieves the configuration instance.
     * @return The configuration instance.
     */
    public static Configuration getInstance() {
        if (instance == null) {
            try {
                instance = new Configuration();
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to open configuration properties file", e);
            }
        }
        return instance;
    }
    
    /**
     * Retrieves the path to the CLASP executable.
     * @return The path to CLASP.
     */
    public String getCLASPExecutablePath() { return props.getProperty("claspexe"); }
    
}
