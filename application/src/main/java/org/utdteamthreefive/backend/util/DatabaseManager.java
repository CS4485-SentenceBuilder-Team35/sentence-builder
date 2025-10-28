package org.utdteamthreefive.backend.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.util.EnvConfig;

/**
 * Simple helper to open and close a MySQL database connection using values from
 * your .env file (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS).
 * 
 * @author Zaeem Rashid
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_USER = EnvConfig.get("DB_USER");
    private static final String DB_PASS = EnvConfig.get("DB_PASS");
    private static final String DB_PORT = EnvConfig.get("DB_PORT");
    private static final String DB_NAME = EnvConfig.get("DB_NAME");
    private static final String ENCODED_DB = java.net.URLEncoder.encode(DB_NAME,
            java.nio.charset.StandardCharsets.UTF_8);
    private static final String DB_HOST = EnvConfig.get("DB_HOST");

    private static final String URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + ENCODED_DB
        + "?useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=UTC"
        + "&useUnicode=true&characterEncoding=UTF-8"
        + "&cachePrepStmts=true&prepStmtCacheSize=256&prepStmtCacheSqlLimit=2048"
        + "&rewriteBatchedStatements=true";

    private DatabaseManager() {
    } // Private constructor to prevent instantiation

    /**
     * Open a new JDBC connection using the URL and credentials from .env.
     * 
     * @return a live {@link Connection} to the database
     */
    public static Connection open() {
        logger.fine("Opening database connection.");
        try {
            Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            logger.fine("Database connection established.");
            return conn;
        } catch (SQLException e) {
            logger.severe("Failed to establish database connection: " + e.getMessage());
        }
        return null;
    }

    /**
     * Close the given connection if it is not {@code null}. Safe to call even if
     * the connection is already closed.
     *
     * @param conn the connection to close
     */
    public static void close(Connection conn) {
        if (conn == null) {
            logger.fine("Connection is null, nothing to close.");
            return;
        }
        try {
            conn.close();
            logger.fine("Database connection closed.");
        } catch (SQLException e) {
            logger.severe("Failed to close database connection: " + e.getMessage());
        }
    }

}