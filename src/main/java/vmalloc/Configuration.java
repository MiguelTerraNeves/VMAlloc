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
