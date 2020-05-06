package first_package;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public class Cleaner {
    String in;
    String contents = "";
    List<Character> eos = Arrays.asList('.', '!', '?');
    List<Character> whitespace = Arrays.asList(' ', '\t', '\n', '\r');
    boolean PROMPT_FOR_EOS = false;
    int dist = 22;
    int left = 4;
    int right = dist - left;

    public static void main(String[] args) {
        try {
            Cleaner x = new Cleaner();
            x.parse();
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    public boolean isWhiteSpace(char c) {
        return whitespace.contains(c);
    }

    public void loadFile() throws FileNotFoundException {
        Scanner inScanner = new Scanner(new File(in), "UTF-8");
        while (inScanner.hasNextLine()) {
            contents += (inScanner.nextLine() + " ");
        }
        inScanner.close();
    }

    public void saveFile() throws Exception {
        if (askYesNo("File parsing complete. Would you like to save your changes to a new file?", "Finished")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Where would you like to save your changes?");
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File save = chooser.getSelectedFile();
                if (save.createNewFile()) {
                    FileWriter writer = new FileWriter(save);
                    writer.write(contents);
                    writer.close();
                } else {
                    throw new Exception("File already exists");
                }
            } else {
                throw new Exception("No file chosen");
            }
        }
    }

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

    public void getInputFile() throws Exception {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            in = chooser.getSelectedFile().toString();
        } else {
            throw new Exception("No file chosen");
        }
    }

    public void parse() throws Exception {
        getInputFile();
        loadFile();
        parseEOS();
        parseEquation();
        saveFile();
    }

    public void parseEOS() {
        String cur = "";
        char[] contentArray = contents.toCharArray();
        int prevEOS = -1;
        for (int i = 0; i < contentArray.length - 1; i++) {
            cur += Character.toString(contentArray[i]);
            if (isEOS(contentArray[i]) && isWhiteSpace(contentArray[i + 1])) {
                if (!PROMPT_FOR_EOS || askYesNo("Is this a sentence?\n\n" + breakString(contents.substring(prevEOS + 1, i + 1)),
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
        scanner.close();
    }
    
    public Pair findNextEq(String sentence, int loc) {
        char[] curSentArr = sentence.toCharArray();
        for (int i = loc; i < curSentArr.length; i++) {
            if (isForeign(curSentArr[i])) {
                if (askYesNo("Would you like to make changes to the following sentence?\n\n"
                        + breakString(sentence), "Abnormal Character Found")){
                    Object[] options = new UniqueIdentifier[Math.min(curSentArr.length, i + right) - Math.max(0, i - left) + 1];
                    for (int j = Math.max(0, i - left); j < Math.min(curSentArr.length, i + right); j++) {
                        options[j - Math.max(0, i - left)] = new UniqueIdentifier(Character.toString(curSentArr[j]));
                    }
                    options[options.length - 1] = new UniqueIdentifier("NONE");
                    int selection1 = JOptionPane.showOptionDialog(null, "Select starting character:", "Equation",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (selection1 != options.length - 1) {
                        int selection2 = JOptionPane.showOptionDialog(null, "Select ending character:", "Equation",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                        String eq = sentence.substring(Math.max(0, i - left) + selection1, Math.max(0, i - left) + selection2 + 1);
                        String[] type = {"EQUATION", "INEQUALITY", "EXPRESSION", "Custom Replace", "Delete Selection"};
                        int selection3 = JOptionPane.showOptionDialog(null, "What would you like to do with '" + eq + "'?",
                                "Equation", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, type, type[0]);
                        return new Pair(Math.max(0, i - left) + selection1,
                                Math.max(0, i - left) + selection2 + 1, type[selection3]);
                    }
                } else {
                	return null;
                }
            }
        }
        return null;
    }
    
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

    public String getInput(String prompt) {
        return JOptionPane.showInputDialog(prompt);
    }

    public static class Pair {
        int begin; int end; String data;
        public Pair(int _begin, int _end, String _data) {
            begin = _begin; end = _end; data = _data;
        }
        public String toString() {
            return "(" + begin + ", " + end + "): " + data;
        }
    }

    public static class UniqueIdentifier {
        String identifier;
        public UniqueIdentifier(String _identifier) {
            identifier = _identifier;
        }
        public String toString() {
            return identifier;
        }
    }

    public boolean isEOS(char c) {
        return eos.contains(c);
    }

    public boolean isForeign(char c) {
        int intVal = (int) c;
        return !((intVal >= 32 && intVal <= 39) || (intVal >= 44 && intVal <= 59)
                || (intVal >= 63 && intVal <= 90) || (intVal >= 95 && intVal <= 122));
    }

    public boolean askTrueYesNo(String question, String title) {
        return JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }
    
    public boolean askYesNo(String question, String title) {
    	String[] temp = {"Yes", "No", "Stop & Discard Changes"};
    	int choice = JOptionPane.showOptionDialog(null, question, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, temp, temp[0]);
    	if (temp[choice].contentEquals("Yes")) {
    		return true;
    	} else if (temp[choice].contentEquals("No")) {
    		return false;
    	} 
    	if (askTrueYesNo("Are you sure you want to exit? Changes will not be saved.", "Confirmation")) {
    		System.exit(0);
    		return false;
    	}
    	return askYesNo(question, title);
    }
}
