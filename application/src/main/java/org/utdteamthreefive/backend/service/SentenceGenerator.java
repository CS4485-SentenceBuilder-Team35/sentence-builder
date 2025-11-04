package org.utdteamthreefive.backend.service;
import java.util.ArrayList;

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
}
