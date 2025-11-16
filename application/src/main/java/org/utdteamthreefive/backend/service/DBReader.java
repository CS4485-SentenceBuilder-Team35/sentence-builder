package org.utdteamthreefive.backend.service;

import org.utdteamthreefive.backend.models.Word;
import org.utdteamthreefive.backend.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
* DBReader contains necessary methods for sentence building algorithms to query words from the database
* Can be used to:

 * - Find the ID of a given word
 * - Find all words that follow a given word
 * - Find max number of times a word has ended a sentence
 * - Lookup how often a specific word ends a sentence
 * - Retrieve all words stored in the database (used for random algorithms)
 
* @author Aiden Martinez
* @author Zaeem Rashid
* @author Aisha Qureshi
 */
public class DBReader {
    private static final Logger logger = Logger.getLogger(DBReader.class.getName());

    /**
     *  Given a word as text, returns the ID of a word (or -1 if not found)
     */
    public int SearchWordID(String word_token)
    {
        Connection conn = null;

        try{
            //open connection
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("Failed to open database connection");
                return -1;
            }

            //prepare statement
            String selectSql = "Select word_id from WORD where word_token = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, word_token);
                //run query
                try (ResultSet rs = selectStmt.executeQuery()) {
                    //get result and return
                    if (rs.next())
                    {
                        return rs.getInt("word_id");
                    }
                    else
                    {
                        //word does not exist in table
                        return -1;
                    }
                }
            }
        }
        catch(SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        }
        finally
        {
            DatabaseManager.close(conn);
        }

        //default
        return -1;
    }

    /**
     *  Given a word as text, returns all words that follow in descending order
     */
    public ArrayList<String> SearchWordFollows(String word_token, boolean desc_order)
    {
        ArrayList<String> results = new ArrayList<>();

        //Get ID for the given word
        int from_word_id = SearchWordID(word_token);
        if(from_word_id == -1)
        {
            logger.severe("Given word not found.");
            return null;
        }

        //Open connection
        Connection conn = null;
        try {
            //open connection
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("Failed to open database connection");
                return null;
            }

            //prepare statement
            String selectSql = """
                    select word_token
                    from word as w, word_follow as wf
                    where wf.from_word_id = ? AND w.word_id = wf.to_word_id
                    ORDER BY wf.total_count
                    """;
            //sort query results
            if(desc_order)
            {
                selectSql += " DESC;";
            }
            else
            {
                selectSql += " ASC;";
            }

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, from_word_id);
                //run query
                try (ResultSet rs = selectStmt.executeQuery()) {
                    int count = 0;

                    //add row to list of results
                    while (rs.next()) {
                        results.add(rs.getString(1));
                        count++;
                    }

                    //results were empty, return null
                    if(count <= 0)
                    {
                        results = null;
                    }
                }
            }
        }
        catch(SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        }
        finally
        {
            DatabaseManager.close(conn);
        }

        return results;
    }

    /**
     *  Query database for the max number of times any word ends a sentence
     */
    public int GetMaxEndOccurrences()
    {
        //Open connection
        Connection conn = null;
        try {
            //open connection
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("Failed to open database connection");
                return -1;
            }

            //prepare statement
            String selectSql = """
                    select MAX(end_count)
                    	from word;
                    """;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                //run query
                try (ResultSet rs = selectStmt.executeQuery()) {
                    //get result and return
                    if (rs.next())
                    {
                        return rs.getInt(1);
                    }
                    else
                    {
                        //table is empty
                        return -1;
                    }
                }
            }
        }
        catch(SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        }
        finally
        {
            DatabaseManager.close(conn);
        }

        //default
        return -1;
    }

    public int GetNumEndOccurrences(String word)
    {
        //Open connection
        Connection conn = null;
        try {
            //open connection
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("Failed to open database connection");
                return -1;
            }

            //prepare statement
            String selectSql = """
                    select end_count
                    	from word
                    	where word_token = ?;
                    """;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, word);
                //run query
                try (ResultSet rs = selectStmt.executeQuery()) {
                    //get result and return
                    if (rs.next())
                    {
                        return rs.getInt(1);
                    }
                    else
                    {
                        //table is empty
                        return -1;
                    }
                }
            }
        }
        catch(SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        }
        finally
        {
            DatabaseManager.close(conn);
        }

        //default
        return -1;
    }

/*
 *  Retrieves all words from the database for use in random sentence generation.
 *  @author Aisha Qureshi
 *  @author Zaeem Rashid
 */
    public ArrayList<String> GetAllWords() {
        ArrayList<String> results = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DatabaseManager.open();
            String selectSql = "SELECT word_token FROM word;";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                ResultSet rs = selectStmt.executeQuery()) {

                int count = 0;
                while (rs.next()) {
                    results.add(rs.getString(1));
                    count++;
                }

                if (count <= 0) {
                    results = null;
                }
            }
        } catch (SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn);
        }
        return results;
    }

     /**
     * Given a word as text, returns all words that follow along with their frequencies.
     * Returns an ArrayList of word tokens in descending order of frequency.
     * This is used by the weighted random algorithm.
     *
     * @author Aisha Qureshi
     */
    public ArrayList<String> SearchWordFollowsWithFrequency(String word_token) {
        ArrayList<String> results = new ArrayList<>();


        // Get ID for the given word
        int from_word_id = SearchWordID(word_token);
        if (from_word_id == -1) {
            logger.severe("Given word not found.");
            return null;
        }


        // Open connection
        Connection conn = null;
        try {
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("Failed to open database connection");
                return null;
            }


            // Query to get word tokens and their frequencies, ordered by total_count descending
            String selectSql = """
                    select w.word_token, wf.total_count
                    from word as w, word_follow as wf
                    where wf.from_word_id = ? AND w.word_id = wf.to_word_id
                    ORDER BY wf.total_count DESC
                    """;


            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, from_word_id);
                // run query
                try (ResultSet rs = selectStmt.executeQuery()) {
                    int count = 0;


                    // Store word tokens with their frequencies as strings (word:frequency)
                    while (rs.next()) {
                        String word = rs.getString("word_token");
                        int frequency = rs.getInt("total_count");
                        results.add(word + ":" + frequency);
                        count++;
                    }


                    // results were empty, return null
                    if (count <= 0) {
                        results = null;
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("SQL error: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn);
        }


        return results;
    }


}
