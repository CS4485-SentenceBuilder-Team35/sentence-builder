package org.utdteamthreefive.backend.util;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads and provides access to environment variables from the .env file. String
 * user = EnvConfig.get("<variable from .env file>");
 * 
 * @author Zaeem Rashid
 */
public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.load();

    /**
     * Get a value from the .env file by its key.
     * 
     * @param key the name of the environment variable
     * @return the value of the variable, or null if not found
     */
    public static String get(String key) {
        return dotenv.get(key);
    }
}
