package first_package;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

/**
 * The Cleaner class implements a complete GUI for getting technical documents
 * ready for FrameNet use. No command-line interaction required, just run the
 * main method without any arguments.
 * 
 * @author Ayush Pancholy
 *
 */
public class Cleaner {
	/** IN represents path to the file to be read. */
    String in;
    /** CONTENTS represents the "output" text. */
    String contents = "";
    /** EOS denotes a List that contains end-of-sentence characters. */
    List<Character> eos = Arrays.asList('.', '!', '?');
    /** WHITESPACE denotes a List that contains whitespace characters. */
    List<Character> whitespace = Arrays.asList(' ', '\t', '\n', '\r');
    /** PROMPT_FOR_EOS is a flag boolean that indicates whether user should be
     * prompted for creating each sentence. */
    boolean PROMPT_FOR_EOS = false;
    /** DIST is the maximum number of characters that can be displayed in a 
     * menu. */
    int dist = 22;
    /** LEFT is the number of characters by which the 'foreign' character 
     * should be offset from the leftmost option in a menu. */
    int left = 4;
    int right = dist - left;

    /**
     * Static main method. Takes no command line arguments. Displays an icon 
     * panel for several seconds before instantiating a new Cleaner object and
     * running its parse method. Handles errors.
     */
    public static void main(String[] args) {
        try {
        	JFrame frame = new JFrame();
        	frame.setUndecorated(true);
        	frame.getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2,
        			2, 2, Color.BLACK));
        	frame.getContentPane().add(new JLabel("<html><h1>FileCleaner</h1>"
        			+ "Clean technical docs fast!<br><br>Version 1.2.0<br>"
        			+ "Updated May 2020<br><br>Built in Java 10</html>", 
        			new ImageIcon(System.getProperty("user.dir") 
        					+ File.separator + "icsi.jpg"), JLabel.CENTER));
        	frame.setSize(430, 250);
        	frame.setLocationRelativeTo(null);
        	frame.setVisible(true);
        	Thread.sleep(4000);
        	frame.setVisible(false);
            Cleaner x = new Cleaner();
            x.parse();
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, "Error: something went wrong." 
        			+ " FileCleaner aborted.");
            System.exit(-1);
        }
    }

    /**
     * Determines whether a character is whitespace depending on the WHITESPACE
     * List.
     * @param c The character to be checked.
     * @return Boolean value determining whether c is whitespace.
     */
    public boolean isWhiteSpace(char c) {
        return whitespace.contains(c);
    }

    /**
     * Builds a Scanner based on the path to the input file, assuming UTF-8
     * encoding, and fills the CONTENTS String accordingly.
     * 
     * @throws FileNotFoundException
     */
    public void loadFile() throws FileNotFoundException {
        Scanner inScanner = new Scanner(new File(in), "UTF-8");
        while (inScanner.hasNextLine()) {
            contents += (inScanner.nextLine() + " ");
        }
        inScanner.close();
    }

    /**
     * Asks the user whether the contents of CONTENTS should be written to a
     * file. If so, asks for location for file to be saved. Throws exception
     * if file already exists.
     * 
     * @throws Exception
     */
    public void saveFile() throws Exception {
        if (askYesNo("File parsing complete. Would you like to save your " 
        		+ "changes to a new file?", "Finished")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Where would you like to save your" 
            		+ " changes?");
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File save = chooser.getSelectedFile();
                if (save.createNewFile()) {
                    FileWriter writer = new FileWriter(save);
                    writer.write(contents);
                    writer.close();
                    JOptionPane.showMessageDialog(null, 
                    		"File written successfully.");
                } else {
                    throw new Exception("File already exists");
                }
            } else {
                throw new Exception("No file chosen");
            }
        }
    }

    /**
     * Breaks a given String every 10 words.
     * 
     * @param input The String to be broken.
     * @return The String broken every 10 words.
     */
    public String breakString(String input) {
        Scanner scanner = new Scanner(input);
        int numWords = 0;
        String out = "";
        while (scanner.hasNext()) {
            numWords++;
            out += scanner.next() + " ";
            if (numWords % 10 == 0) {
                out += "\n";
            }
        }
        scanner.close();
        return out;
    }

    /**
     * Retrieves the file to be parsed with a GUI. Throws an Exception if no
     * file is chosen.
     * 
     * @throws Exception
     */
    public void getInputFile() throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a File for Parsing");
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            in = chooser.getSelectedFile().toString();
        } else {
            throw new Exception("No file chosen");
        }
    }

    /**
     * The central method for the Cleaner class. Calls other methods to get the
     * input file, parse sentences, find formatting issues, and save the
     * output.
     * 
     * @throws Exception
     */
    public void parse() throws Exception {
        getInputFile();
        loadFile();
        parseEOS();
        parseEquation();
        saveFile();
    }

    /**
     * Goes through the CONTENTS and breaks at EOS markers. Removes all other
     * arbitrary newlines. Prompts the user only if PROMPT_FOR_EOS is true.
     */
    public void parseEOS() {
        String cur = "";
        char[] contentArray = contents.toCharArray();
        int prevEOS = -1;
        for (int i = 0; i < contentArray.length - 1; i++) {
            cur += Character.toString(contentArray[i]);
            if (isEOS(contentArray[i]) && isWhiteSpace(contentArray[i + 1])) {
                if (!PROMPT_FOR_EOS || askYesNo("Is this a sentence?\n\n" 
                		+ breakString(contents.substring(prevEOS + 1, i + 1)),
                        "Potential EOS Found")) {
                    prevEOS = i;
                    cur += "\n";
                    i++;
                }
            }
        }

        String cur2 = "";
        Scanner scanner = new Scanner(cur);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (isWhiteSpace(line.toCharArray()[0])) {
                cur2 += line.substring(1, line.length()) + "\n";
            } else {
                cur2 += line + "\n";
            }
        }
        contents = cur2;
        scanner.close();
    }

    /**
     * Implements a GUI-style approach to dealing with bad formatting or
     * foreign characters. Displays processed sentences on a background
     * JFrame.
     */
    public void parseEquation() {
        Scanner scanner = new Scanner(contents);
        String cur = "";     
        JTextArea textArea = new JTextArea(2, 20);
        textArea.setText("Processed Sentences:\n\n");
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(UIManager.getBorder("Label.border"));
        JFrame frame = new JFrame();
        frame.getContentPane().add(textArea, BorderLayout.CENTER);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setTitle("Progress");
        frame.setVisible(true);
    	String temp;
        while (scanner.hasNextLine()) {
        	temp = parseSentenceForEquation(scanner.nextLine());
            cur += temp + "\n";
            textArea.setText(textArea.getText() + temp + "\n\n");
            frame.setVisible(true);
        }
        contents = cur;
        frame.setVisible(false);
        scanner.close();
    }
    
    /**
     * Finds the location of the next broken formatting position, as long as
     * the next position is numerically greater than LOC. Returns NULL if no
     * more locations are found. Implements a GUI-based approach to determine
     * the type of formatting issue.
     * 
     * @param sentence The input String.
     * @param loc The position after which broken formatting is to be found.
     * @return A Pair object representing the location and type of issue found.
     */
    public Pair findNextEq(String sentence, int loc) {
        char[] curSentArr = sentence.toCharArray();
        for (int i = loc; i < curSentArr.length; i++) {
            if (isForeign(curSentArr[i])) {
                if (askYesNo("Would you like to make changes to the following" 
                		+ " sentence?\n\n" + breakString(sentence), 
                		"Abnormal Character Found")){
                    Object[] options = new UniqueIdentifier[Math.min(
                    		curSentArr.length, i + right) 
                          - Math.max(0, i - left) + 1];
                    for (int j = Math.max(0, i - left); 
                    		j < Math.min(curSentArr.length, i + right); j++) {
                        options[j - Math.max(0, i - left)] = 
                        		new UniqueIdentifier(
                        				Character.toString(curSentArr[j]));
                    }
                    options[options.length - 1] = new UniqueIdentifier("NONE");
                    int selection1 = JOptionPane.showOptionDialog(null, 
                    		"Select starting character:", "Equation",
                            JOptionPane.DEFAULT_OPTION, 
                            JOptionPane.WARNING_MESSAGE, null, options, 
                            options[0]);
                    if (selection1 != options.length - 1) {
                        int selection2 = JOptionPane.showOptionDialog(null, 
                        		"Select ending character:", "Equation",
                                JOptionPane.DEFAULT_OPTION, 
                                JOptionPane.WARNING_MESSAGE, null, options, 
                                options[0]);
                        String eq = sentence.substring(Math.max(0, i - left) 
                        		+ selection1, Math.max(0, i - left) 
                        		+ selection2 + 1);
                        String[] type = {"EQUATION", "INEQUALITY", 
                        		"EXPRESSION", "Custom Replace", 
                        		"Delete Selection"};
                        int selection3 = JOptionPane.showOptionDialog(null, 
                        		"What would you like to do with '" + eq + "'?",
                                "Equation", JOptionPane.DEFAULT_OPTION, 
                                JOptionPane.WARNING_MESSAGE, null, type, 
                                type[0]);
                        return new Pair(Math.max(0, i - left) + selection1,
                                Math.max(0, i - left) + selection2 + 1, 
                                type[selection3]);
                    }
                } else {
                	return null;
                }
            }
        }
        return null;
    }
    
    /**
     * Given a sentence, resolves all of the formatting issues in the sentence.
     * Cycles through all the characters in the sentence to find the issues.
     * Implements a GUI-based approach to fix issues such as deletion or
     * replacements.
     * 
     * @param sentence The String in which to resolve the issues.
     * @return The fixed input String.
     */
    public String parseSentenceForEquation(String sentence) {
    	Pair cur = findNextEq(sentence, 0);
    	int nextInd = 0;
    	while (cur != null) {
    		String first = sentence.substring(0, cur.begin);
    		String second = sentence.substring(cur.end, sentence.length());
    		if (cur.data.equals("Custom Replace")) {
                String input = getInput("Enter a replacement for '"
                        + sentence.substring(cur.begin, cur.end) + "'");
                if (input == null) {
                    sentence = first + second;
                    nextInd = first.length();
                } else {
                    sentence = first + input + second;
                    nextInd = first.length() + input.length();
                }
            } else if (cur.data.equals("Delete Selection")) {
                sentence = first + second;
                nextInd = first.length();
            } else {
                sentence = first + cur.data + second;
                nextInd = first.length() + cur.data.length();
            }
    		cur = findNextEq(sentence, nextInd);
    	}
    	return sentence;
    }

    /**
     * Constructs a JOptionPane given a String prompt.
     * 
     * @param prompt The String prompt to construct the JOptionPane.
     * @return A new JOptionPane with the given prompt.
     */
    public String getInput(String prompt) {
        return JOptionPane.showInputDialog(prompt);
    }

    /**
     * A Class that represents the location of a formatting issue in a sentence
     * as well as the type of issue as a String. This is a tiny helper class.
     *
     */
    public static class Pair {
    	/** BEGIN and END together represent the span of the formatting issue,
    	 * and DATA represents the type of formatting issue. */
        int begin; int end; String data;
        
        /** Self-evident constructor. */
        public Pair(int _begin, int _end, String _data) {
            begin = _begin; end = _end; data = _data;
        }
        
        /** Self-evident toString. */
        @Override
        public String toString() {
            return "(" + begin + ", " + end + "): " + data;
        }
    }

    /**
     * Dummy class to differentiate Strings.
     */
    public static class UniqueIdentifier {
        String identifier;
        public UniqueIdentifier(String _identifier) {
            identifier = _identifier;
        }
        public String toString() {
            return identifier;
        }
    }

    /**
     * Determines whether a given character is an end-of-sentence marker.
     * 
     * @param c The character to be checked.
     * @return A boolean value depending on whether C is an EOS marker.
     */
    public boolean isEOS(char c) {
        return eos.contains(c);
    }

    /**
     * Determines whether a character is "foreign." A "foreign" character is
     * any character that is not thought to generally appear in typically
     * FrameNet corpora, such as equals signs, math type, or any unrecognized
     * formatting. Exact specifications can be observed by reading this method
     * against an ASCII chart.
     * 
     * @param c The character to be checked.
     * @return A boolean value depending on whether C is foreign.
     */
    public boolean isForeign(char c) {
        int intVal = (int) c;
        return !((intVal >= 32 && intVal <= 39) 
        		|| (intVal >= 44 && intVal <= 59)
                || (intVal >= 63 && intVal <= 90) 
                || (intVal >= 95 && intVal <= 122));
    }

    /**
     * Generates a JOptionPane dialog given a QUESTION and a TITLE. Returns
     * true only if the user has selected 'yes.'
     * 
     * @param question The prompt for the user, represented by a String.
     * @param title The title of the dialog, represented by a String.
     * @return A boolean value representing whether the user has chosen 'yes.'
     */
    public boolean askTrueYesNo(String question, String title) {
        return JOptionPane.showConfirmDialog(null, question, title, 
        		JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    
    /**
     * Implements a yes/no dialog with the additional option of stop and
     * discarding changes. If 'Stop and Discard Changes' is selected, a
     * confirmation is generated, and the System exits if confirmed.
     * 
     * @param question The prompt for the user, represented by a String.
     * @param title The title of the dialog, represented by a String.
     * @return A boolean value representing whether the user has chosen 'yes.'
     */
    public boolean askYesNo(String question, String title) {
    	String[] temp = {"Yes", "No", "Stop & Discard Changes"};
    	int choice = JOptionPane.showOptionDialog(null, question, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                temp, temp[0]);
    	if (temp[choice].contentEquals("Yes")) {
    		return true;
    	} else if (temp[choice].contentEquals("No")) {
    		return false;
    	} 
    	if (askTrueYesNo("Are you sure you want to exit? Changes will not be" 
    			+ " saved.", "Confirmation")) {
    		System.exit(0);
    		return false;
    	}
    	return askYesNo(question, title);
    }
}
