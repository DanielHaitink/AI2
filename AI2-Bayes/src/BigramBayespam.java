/*
 * Copyright (c) 2015 by Daniël Haitink and Jan-Willem de Wit.
 * This File is copyrighted. You are not allowed to use or redistribute this code without the consent of the copyright-holders.
 * You are also not allowed to remove this copyright message.
 * If the copyright-holders allowed you to use or redistribute the code, the copyright-holders must be credited.
 */

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.StringTokenizer;


/**
 * Created by DanielHaitink on 25-09-15.
 */
public class BigramBayespam {
    static double zeroProb = 0.1;
    static int threshold = 1, bigramLength = 3 ;
    static int truePositive = 0, trueNegative = 0, falsePositive = 0, falseNegative = 0;

    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }
 
    static class Bigram
    {
    	
        String firstWord = null, secondWord = null;

        @Override
        public String toString(){
            return firstWord +" "+ secondWord;
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((firstWord == null) ? 0 : firstWord.hashCode());
			result = prime * result
					+ ((secondWord == null) ? 0 : secondWord.hashCode());
			return result;
		}

		@Override 
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Bigram other = (Bigram) obj;
			if (firstWord == null) {
				if (other.firstWord != null)
					return false;
			} else if (!firstWord.equals(other.firstWord))
				return false;
			if (secondWord == null) {
				if (other.secondWord != null)
					return false;
			} else if (!secondWord.equals(other.secondWord))
				return false;
			return true;
		}
        
  
        
  
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;
        /// New variables which store the log probability of the word in both ham and Spam
        double probability_regular = 0;
        double probability_spam = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
                /// increase counter of total words in ham
                wordsHam++;
            } else {
                ++counter_spam;
                /// increase counter of total words in spam
                wordsSpam++;
            }
        }

    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable<Bigram, Multiple_Counter> vocab = new Hashtable <Bigram, Multiple_Counter> ();
    private static int wordsSpam, wordsHam, messagesHam, messagesSpam; /// the total words in ham and spam and the total messages in ham and spam


    // Add a word to the vocabulary
    private static void addBigram(Bigram bigram, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(bigram) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(bigram);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(bigram, counter);                       // put the word with its counter into the hashtable
    }


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 3 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[1].listFiles();
        listing_spam    = dir_listing[2].listFiles();
    }


    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<Bigram> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            Bigram bigram = new Bigram();

            bigram = e.nextElement();
            counter  = vocab.get(bigram);

            System.out.println( bigram.toString() + " | in regular: " + counter.counter_regular +
                    " in spam: "    + counter.counter_spam);
        }
    }
    private static void cleanVocab(){
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<Bigram> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            Bigram bigram = new Bigram();
            
            bigram = e.nextElement();
            
            if ((vocab.get(bigram).counter_regular+vocab.get(bigram).counter_spam)<threshold){
            	vocab.remove(bigram);
            }
        }
    }

    /// Removes Numerals from the String and returns the new string
    private static String removeNumerals(String string){
        char numerals[] = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        for (char currentChar: numerals){
            string = string.replaceAll( Character.toString(currentChar), "");
        }
        return string;
    }

    /// removes punctuation from given string and returns the new string
    private static String removePunctuation(String string){
        String strArray[] = {"\\.", "\\?", "-", ",", "/", "!", ";", ":", "_", "\"", "<", ">", "\\[", "\\]", "\\{", "\\}", "\\(", "\\)", "\\@", "\\^", "\\|", "\\*", "\\&", "\\%", "\\$", "\\€", "\\%", "\\+", "\\="};
        for (String currentChar: strArray){
            string = string.replaceAll( currentChar, "");
        }
        return string;
    }

    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not
    private static void readMessages(MessageType type)
            throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
            /// Store the number of messages of Ham
            messagesHam = messages.length;
        } else {
            messages = listing_spam;
            /// Store the number of messages of Spam
            messagesSpam = messages.length;
        }

        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String currentToken;


            while ((line = in.readLine()) != null)                      // read a line
            {
                /// Set line to lowercase, remove numerals and punctuation
                line = line.toLowerCase();
                line = removeNumerals(line);
                line = removePunctuation(line);

                Bigram bigram = new Bigram();


                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens())                  // while there are stille words left..
                {
                	Bigram oldBigram = bigram;
                	bigram = new Bigram();
                	if(oldBigram.firstWord != null) bigram.firstWord = oldBigram.firstWord;
                	if(oldBigram.secondWord != null) bigram.secondWord = oldBigram.secondWord;
                	
                    currentToken = st.nextToken();
                    
                    if (currentToken.equals(null)){
                    	bigram.firstWord = null;
                    	bigram.secondWord = null;
                        continue;
                    }
                    if (bigram.firstWord == null){
                        bigram.firstWord = currentToken;
                    }else if (bigram.secondWord == null) {
                        bigram.secondWord = currentToken;
                    }else {
                        bigram.firstWord = bigram.secondWord;
                        bigram.secondWord = currentToken;
                    }
                    if  (bigram.firstWord != null && bigram.secondWord != null 
                    		&& (bigram.firstWord.length() > bigramLength || bigram.secondWord.length() > bigramLength)){
                    	addBigram(bigram, type); // add them to the vocabular
                    	
                    }
                }
            }

            in.close();
        }

    }

    private static void classifyMessages(MessageType type)
            throws IOException
    {
        File[] messages = new File[0];
        double pRegular, pSpam;

        if (type == MessageType.NORMAL){
            messages = listing_regular;

        } else {
            messages = listing_spam;
        }

        /// calculate the probability of the regular (ham) directory and the spam directory
        pRegular = (double)messagesHam/((double)messagesHam+(double)messagesSpam);
        pSpam = (double)messagesSpam/((double)messagesHam+(double)messagesSpam);
        
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            Bigram bigram = new Bigram();
            String currentToken;
            double pMessage_Spam = -Math.log(pSpam), pMessage_Ham = -Math.log(pRegular); /// add probability of being ham or spam
            
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                /// Set line to lowercase, remove numerals and punctuation
                line = line.toLowerCase();
                line = removeNumerals(line);
                line = removePunctuation(line);

                bigram.firstWord = null;
                bigram.secondWord = null;

                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens())                  // while there are stille words left..
                {
                    currentToken = st.nextToken();

                    if (bigram.firstWord == null){
                        bigram.firstWord = currentToken;
                    }else if(bigram.secondWord == null){
                        bigram.secondWord = currentToken;
                    }else {
                        bigram.firstWord = bigram.secondWord;
                        bigram.secondWord = currentToken;
                    }
                    /// word is only added if it is longer than 4 chars
                    if (bigram.firstWord != null && bigram.secondWord != null) {
                    	
                        /// Calculate the probability
                        if(vocab.containsKey(bigram)){
                            pMessage_Spam += vocab.get(bigram).probability_spam;
                            pMessage_Ham += vocab.get(bigram).probability_regular;
                        }
                    }
                }
            }
            if(pMessage_Spam<pMessage_Ham){ ///Classified as spam
                if (type == MessageType.SPAM){
                    truePositive++;  ///spam correctly classified as spam
                }else{
            		falsePositive++; ///ham incorrectly classified as spam
                }
            }
            else{ ///Classified as ham
                if (type == MessageType.NORMAL){
                    trueNegative++; ///ham correctly classified as ham
                }else{
                	falseNegative++; ///spam incorrectly classified as ham
                }
            }
            in.close();
        }
    }



    /// Calculate the log probabilities of the words
    private static void probabilities(){
        Multiple_Counter counter = new Multiple_Counter();

        /// loop through all words in the vocab
        for (Enumeration<Bigram> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            Bigram bigram = new Bigram();

            bigram = e.nextElement();
            counter  = vocab.get(bigram);

            // calculate the log probability
           
            counter.probability_regular = -1 * ( Math.log ((double)counter.counter_regular) - Math.log((double)wordsHam) );
            counter.probability_spam = -1 * ( Math.log((double)counter.counter_spam) - Math.log((double)wordsSpam) );

            /// If the probability is infinite, set it to a standard p
            if (Double.isInfinite(counter.probability_regular)){
                counter.probability_regular = -1 * ( Math.log( zeroProb) - Math.log((wordsHam+wordsSpam)) );
            }
            if (Double.isInfinite(counter.probability_spam)){
                counter.probability_spam = -1 *( Math.log( zeroProb ) - Math.log((wordsHam+wordsSpam)) );
            }

        }
    }

    public static void main(String[] args)
            throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        File dir_location2 = new File( args[1]);
        wordsHam = 0;
        wordsSpam = 0;

        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);


        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        // Print out the hash table
        //printVocab();
        cleanVocab();
        printVocab();
        probabilities();

        if ( !dir_location2.isDirectory() )
        {  
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }
        listDirs(dir_location2);

        classifyMessages(MessageType.NORMAL);
        classifyMessages(MessageType.SPAM);
        
        ///print the confusion matrix
        System.out.println("\t \t Classified spam \t Classified ham");
        System.out.println("Actual spam \t \t" + truePositive + "\t \t \t" + falseNegative);
        System.out.println("Actual ham \t \t" + falsePositive + "\t \t \t" + trueNegative);
        
 


        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }

}
