package org.utdteamthreefive.backend.service;
import java.util.ArrayList;
import java.util.Random;

/**
 * SentenceGenerator builds sentences using several different algorithms that
 * rely on word data stored in the database. It can generate sentences based on:
 *  - The most frequent next word relationships
 *  - Randomly selected words from the entire vocabulary
 *  - Random follow words starting from a user provided input
 *
 * Each method constructs a sentence word by word using queries from DBReader
 * and returns the final sentence as formatted text.
 * 
 * @author Aisha Qureshi
 * @author Aiden Martinez
 * @author Zaeem Rashid
 */
public class SentenceGenerator {

    public static String GenerateFromMostFrequent(String initialInput)
    {
        DBReader databaseReader = new DBReader();
        String sentence = initialInput;
        int endThreshold = databaseReader.GetMaxEndOccurrences();


        //if given more than one word, get last word
        String[] input = initialInput.split("\\s");
        String currentWord = input[input.length - 1].toLowerCase();
        int sentenceLength = input.length;

        while(true)
        {
            ArrayList<String> wordFollow = databaseReader.SearchWordFollows(currentWord);

            //Word does not exist or is never followed
            if(wordFollow == null || wordFollow.isEmpty())
                break;

            //Get highest occurring next word not already present in sentence
            boolean wordFound = false;
            for(String word : wordFollow)
            {
                if(!IsInSentence(sentence, word))
                {
                    System.out.println(sentence + " : next-> " + word);
                    System.out.println("Threshold: " + endThreshold);
                    sentence = sentence + " " + word;
                    currentWord = word;
                    wordFound = true;
                    break;
                }
            }

            //End sentence if no further next word can be found
            if(!wordFound)
            {
                System.out.println("No next word exists");
                break;
            }
            sentenceLength++;

            //Check if sentence should end
            int endCount = databaseReader.GetNumEndOccurrences(currentWord);
            if(endCount == -1)
                break;

            if(endCount >= endThreshold)
            {
                System.out.println("Threshold met");

                break;
            }
            else if(sentenceLength > 5)
            {
                //lower end threshold

            }
            endThreshold = (int)(endThreshold * 0.75);
        }

        return sentence + ".";
    }

    //Check if the given word is in a given sentence.
    private static boolean IsInSentence(String sentence, String word)
    {
        String[] words = sentence.split(" ");
        for (String s : words) {
            if (s.equals(word))
                return true;
        }

        return false;
    }

/**
 * Builds a sentence by randomly choosing words until the target length is reached.
 * @author Zaeem Rashid
 */
    public static String GenerateFromRandomWord(int targetLength) {
        DBReader databaseReader = new DBReader();
        ArrayList<String> allWords = databaseReader.GetAllWords();

        if (allWords == null || allWords.isEmpty()) {
            return "No words available in the database.";
        }

        Random random = new Random();
        StringBuilder sentence = new StringBuilder();

        String currentWord = allWords.get(random.nextInt(allWords.size()));
        sentence.append(currentWord);
        int sentenceLength = 1;

        // Keep adding random words until we reach at least targetLength words
        while (sentenceLength < targetLength) {
            String nextWord = allWords.get(random.nextInt(allWords.size()));
            sentence.append(" ").append(nextWord);
            currentWord = nextWord;
            sentenceLength++;
        }

        return sentence.toString() + ".";
    }
/**
 * Generates a sentence by repeatedly choosing random valid follow-words until reaching the target length.
 * @author Aisha Qureshi
 */
    public static String GenerateFromRandomFollow(String initialInput, int targetLength) {
        if (initialInput == null || initialInput.trim().isEmpty()) {
            return "Please enter a starting word.";
        }

        DBReader databaseReader = new DBReader();
        Random random = new Random();

        String sentence = initialInput.trim();
        String[] input = sentence.split("\\s+");
        String currentWord = input[input.length - 1].toLowerCase();
        int sentenceLength = input.length;

        while (sentenceLength < targetLength) {
            ArrayList<String> follows = databaseReader.SearchWordFollows(currentWord);

            // If no follow words exist, stop.
            if (follows == null || follows.isEmpty()) {
                break;
            }

            // Pick a random follow word
            String nextWord = follows.get(random.nextInt(follows.size()));
            sentence = sentence + " " + nextWord;
            currentWord = nextWord;
            sentenceLength++;
        }
        return sentence + ".";
    }
}
