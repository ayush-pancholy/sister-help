package first_package;

import javax.swing.*;
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
    }

    public void parseEquation() {
        Scanner scanner = new Scanner(contents);
        String cur = "";
        while (scanner.hasNextLine()) {
            cur += parseSentenceForEquation(scanner.nextLine()) + "\n";
        }
        contents = cur;
    }

    public ArrayList<Pair> findEquations(String sentence) {
        char[] curSentArr = sentence.toCharArray();
        ArrayList<Pair> eqLocations = new ArrayList<>();
        boolean noResponse = false;
        for (int i = 0; i < curSentArr.length; i++) {
            if (!noResponse && isForeign(curSentArr[i])) {
                if (askYesNo("Would you like to make further changes to the following sentence?\n\n"
                        + breakString(sentence), "Potential Equation Found")){
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
                        eqLocations.add(new Pair(Math.max(0, i - left) + selection1,
                                Math.max(0, i - left) + selection2 + 1, type[selection3]));
                        i = Math.max(0, i - left) + selection2;
                    }
                } else {
                    noResponse = true;
                }
            }
        }
        return eqLocations;
    }

    public String parseSentenceForEquation(String sentence) {
        ArrayList<Pair> eqLocations = findEquations(sentence);
        for (int i = eqLocations.size() - 1; i >= 0; i--) {
            String first = sentence.substring(0, eqLocations.get(i).begin);
            String second = sentence.substring(eqLocations.get(i).end, sentence.length());
            if (eqLocations.get(i).data.equals("Custom Replace")) {
                String input = getInput("Enter a replacement for '"
                        + sentence.substring(eqLocations.get(i).begin, eqLocations.get(i).end) + "'");
                if (input == null) {
                    sentence = first + second;
                } else {
                    sentence = first + input + second;
                }
            } else if (eqLocations.get(i).data.equals("Delete Selection")) {
                sentence = first + second;
            } else {
                sentence = first + eqLocations.get(i).data + second;
            }
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

    public boolean askYesNo(String question, String title) {
        return JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }
}
