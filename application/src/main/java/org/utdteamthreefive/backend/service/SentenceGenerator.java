package org.utdteamthreefive.backend.service;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
        Implements Most Frequent Algorithm, returning a generated sentence
     @author Aiden Martinez
     */
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
            ArrayList<String> wordFollow = databaseReader.SearchWordFollows(currentWord, true);
            boolean wordFound = false;

            //Word does not exist or is never followed
            if(wordFollow == null || wordFollow.isEmpty())
            {
                String word = GetRandomWord();
                sentence = sentence + " " + word;
                currentWord = word;
                wordFound = true;
            }
            else
            {
                //Get highest occurring next word not already present in sentence
                for(String word : wordFollow)
                {
                    if(!IsInSentence(sentence, word))
                    {
                        sentence = sentence + " " + word;
                        currentWord = word;
                        wordFound = true;
                        break;
                    }
                }
            }

            //No further next word can be found
            if(!wordFound)
            {
                String word = GetRandomWord();
                sentence = sentence + " " + word;
                currentWord = word;
            }

            sentenceLength++;

            //Check if sentence should end
            int endCount = databaseReader.GetNumEndOccurrences(currentWord);
            if(endCount == -1)
                break;

            //threshold met
            if(endCount >= endThreshold)
                break;

            //Sentence max length reached
            if(sentenceLength > 15)
                break;

            endThreshold = (int)(endThreshold * 0.75);
        }

        return sentence + ".";
    }

    /**
     Implements Least Frequent Algorithm, returning a generated sentence
     @author Aiden Martinez
     */
    public static String GenerateFromLeastFrequent(String initialInput)
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
            ArrayList<String> wordFollow = databaseReader.SearchWordFollows(currentWord, false);
            boolean wordFound = false;

            //Word does not exist or is never followed
            if(wordFollow == null || wordFollow.isEmpty())
            {
                String word = GetRandomWord();
                sentence = sentence + " " + word;
                currentWord = word;
                wordFound = true;
            }
            else
            {
                //Get highest occurring next word not already present in sentence
                for(String word : wordFollow)
                {
                    if(!IsInSentence(sentence, word))
                    {
                        sentence = sentence + " " + word;
                        currentWord = word;
                        wordFound = true;
                        break;
                    }
                }
            }

            //No further next word can be found
            if(!wordFound)
            {
                String word = GetRandomWord();
                sentence = sentence + " " + word;
                currentWord = word;
            }

            sentenceLength++;

            //Check if sentence should end
            int endCount = databaseReader.GetNumEndOccurrences(currentWord);
            if(endCount == -1)
                break;

            //threshold met
            if(endCount >= endThreshold)
                break;

            //Sentence max length reached
            if(sentenceLength > 10)
                break;

            endThreshold = (int)(endThreshold * 0.5);
        }

        return sentence + ".";
    }

    /**
        Check if the given word is in a given sentence.
     @author Aiden Martinez
     */
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
    /**
        Return a random basic word to help continue the sentence
     @author Aiden Martinez
     */
    private static String GetRandomWord()
    {
        ArrayList<String> helperWords = new ArrayList<>(Arrays.asList(
                "and",
                "the",
                "is",
                "or"
        ));

        Random random = new Random();
        return helperWords.get(random.nextInt(helperWords.size() - 1));
    }
}
