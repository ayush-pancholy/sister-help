package first_package;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Scanner;

public class Analyzer {
	private Map<String, Set<LU>> frameMap;
	private Map<LU, Set<LU>> output;
	public static String prefix = "C:\\Users\\14086\\Desktop\\FNExperiments\\fndata-1.7\\lu\\";
	public static String out_direc = "C:\\Users\\14086\\Desktop\\test_bad\\";
	public static String remove_direc = "C:\\Users\\14086\\Desktop\\LU_Removal_Experiment\\LU_Removed\\";
	public static String reaug_direc = "C:\\Users\\14086\\Desktop\\LU_Removal_Experiment\\LU_Reaugmented\\";
	public static String begin = "lu";
	public static String ext = ".xml";
	public static boolean NO_MULTIWORD_LEXEME = true;
	//TODO there is a known issue regarding multiword lexemes, so keep NO_MULTIWORD_LEXEME true for predictable behavior
	public static boolean REMOVAL_EXPERIMENT = true;
	public static int NUM_REMOVE = 1500;
	public LU EMPTY;
	
	public static final int LAST_LU = 18820;
	public static final double ANNO_THRESH = 1;
	public static final boolean SAME_POS = true;
	public static final int EMPTY_TEMPLATE = 17541;
	private Map<LU, LU> mostAnnotatedSister;
	private static final String ampRepl = Character.toString((char) 300);
	private static final String gtRepl = Character.toString((char) 301);
	private static final String ltRepl = Character.toString((char) 302);
	private static final String quotRepl = Character.toString((char) 303);

	public static void main(String[] args) {
		Analyzer analyzer = new Analyzer();
		analyzer.populateFrameMap();
		if (REMOVAL_EXPERIMENT) {
			System.out.println("Removing...");
			analyzer.findRemovals();
			System.out.println("Reaugmenting...");
			analyzer.replace();
		} else {
			analyzer.findLUs();
			analyzer.replace();
		}
		//System.out.println(analyzer.retrieveID("gripe.v", "Bragging"));
	}
	
	public Analyzer() {
		frameMap = new HashMap<String, Set<LU>>();
		output = new HashMap<LU, Set<LU>>();
		mostAnnotatedSister = new HashMap<LU, LU>();
	}
	
	public Scanner getLUScanner(int num) {
		File file = new File(prefix + begin + Integer.toString(num) + ext);
		try {
			return new Scanner(file, "UTF-8");
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	public String getLUString(int num) {
		File file = new File(prefix + begin + Integer.toString(num) + ext);
		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			return null;
		}
	}
	
	public int retrieveID(String luIn, String frame) {
		for (LU lu : frameMap.get(frame)) {
			if (lu.getName().equals(luIn)) {
				return lu.getNum();
			}
		}
		return -1;
	}
	
	public void replace() {
		long begin = System.currentTimeMillis();
		double total = 0;
		String temp;
		PrintWriter writer;
		int last = -1;
		int cur;
		double endpoint = (double)output.size();
		for (LU lu : mostAnnotatedSister.keySet()) {
			//if (lu.getNum() == 16806) {
			
			temp = replaceLUXML(mostAnnotatedSister.get(lu), lu); 
			try {
				writer = new PrintWriter((REMOVAL_EXPERIMENT ? reaug_direc : out_direc) + "lu" + lu.getNum() + ".xml", "UTF-8");
				writer.print(temp);
				writer.flush();
				writer.close();
			} catch (Exception e) {}
			cur = ((int)(total/endpoint*100));
			if (cur % 5 == 0 && cur != last) {
				System.out.print((int)(total/endpoint*100) + "%...");
				last = cur;
			}
			total++;
			
			//}
		}
		long end = System.currentTimeMillis();
		System.out.println();
		System.out.println("\nCompleted. Time taken: " + (end - begin) / 1000 + " seconds.");
	}
	
	public void findRemovals() {
		Set<LU> annSisters;
		LU mostAnnotatedLU;
		System.out.println("Finding all annotated LUs...");
		for (String frame : frameMap.keySet()) {
			if (percentAnnotated(frame) >= ANNO_THRESH) {
				for (LU lu : frameMap.get(frame)) {
					annSisters = annotatedSisters(lu, SAME_POS);
					if (lu.isAnnotated() && !annSisters.isEmpty()) {
						mostAnnotatedLU = mostAnnotated(annSisters);
						if (NO_MULTIWORD_LEXEME && !lu.multiword() && !mostAnnotatedLU.multiword()) {
							output.put(lu, annSisters);
							mostAnnotatedSister.put(lu, mostAnnotatedLU);
						}
					}
				}
			}
		}
		System.out.println("Shuffling LUs...");
		List<LU> keys = Arrays.asList((LU[]) output.keySet().toArray(new LU[output.keySet().size()]));
		Collections.shuffle(keys);
		System.out.println("Randomly keeping " + NUM_REMOVE + " LUs...");
		for (int i = 0; i < keys.size() - NUM_REMOVE; i++) {
			output.remove(keys.get(i));
			mostAnnotatedSister.remove(keys.get(i));
		}
		System.out.println("Writing emptied LUs to file...");
		PrintWriter writer;
		for (LU lu : output.keySet()) {
			String temp = replaceLUXML(EMPTY, lu);
			try {
				writer = new PrintWriter(remove_direc + "lu" + lu.getNum() + ".xml", "UTF-8");
				writer.print(temp);
				writer.flush();
				writer.close();
			} catch (Exception e) {}
		}
	}
	
	
	public void findLUs() {
		Set<LU> annSisters;
		int totalOptimizableLexemes = 0;
		LU mostAnnotatedLU;
		for (String frame : frameMap.keySet()) {
			if (percentAnnotated(frame) >= ANNO_THRESH) {
				for (LU lu : frameMap.get(frame)) {
					annSisters = annotatedSisters(lu, SAME_POS);
					if (!lu.isAnnotated() && !annSisters.isEmpty()) {
						mostAnnotatedLU = mostAnnotated(annSisters);
						totalOptimizableLexemes++;
						if (NO_MULTIWORD_LEXEME && !lu.multiword() && !mostAnnotatedLU.multiword()) {
							output.put(lu, annSisters);
							mostAnnotatedSister.put(lu, mostAnnotatedLU);
						}
					}
				}
			}
		}
		if (NO_MULTIWORD_LEXEME) {
			System.out.println(totalOptimizableLexemes + " optimizable LU's found, of which " + output.size() 
			+ " did not contain multiword lexemes in the repalcer/replacee pair..");
		} else {
			System.out.println(output.size() + " optimizable LU's found.\n");
		}
	}
	
	public void displayMostAnnoSnippet(int max) {
		int count = 0;
		for (LU lu : mostAnnotatedSister.keySet()) {
			if (count >= max) {
				break;
			}
			System.out.println(lu + " (" + lu.getFrame() + "): " + mostAnnotatedSister.get(lu) + " (" + mostAnnotatedSister.get(lu).getAnnotated() + " annotation(s))");
			count++;
		}
	}
	
	public void displayOutputSnippet(int max) {
		int maxLUs = max;
		int count = 0;
		for (LU lu : output.keySet()) {
			if (count >= maxLUs) {
				break;
			}
			System.out.println(lu + " (" + lu.getFrame() + "): " + output.get(lu).toString());
			count++;
		}
		System.out.println("...");
	}
	
	public int countUnannotated() {
		int count = 0;
		for (String frame : frameMap.keySet()) {
			for (LU lu : frameMap.get(frame)) {
				if (!lu.isAnnotated()) {
					count++;
				}
			}
		}
		return count;
	}
	
	public double percentAnnotated(String frame) {
		double total = 0;
		double ann = 0;
		for (LU lu : frameMap.get(frame)) {
			total++;
			if (lu.isAnnotated()) {
				ann++;
			}
		}
		return total == 0 ? 0 : 100*ann/total;
	}
	
	public Set<LU> annotatedSisters(LU lu, boolean samePOS) {
		Set<LU> annSisters = new HashSet<LU>();
		for (LU sister : frameMap.get(lu.getFrame())) {
			if (!lu.equals(sister) && sister.isAnnotated() && (!samePOS || lu.getPOS().equals(sister.getPOS()))) {
				annSisters.add(sister);
			}
		}
		return annSisters;
	}
	
	public LU mostAnnotated(Set<LU> lus) {
		LU cur = lus.iterator().next();
		for (LU lu : lus) {
			cur = lu.getAnnotated() > cur.getAnnotated() ? lu : cur;
		}
		return cur;
	}
	
	public void populateFrameMap() {
		LU cur;
		for (int i = 0; i < LAST_LU; i++) {
			cur = readLU(i);
			if (cur != null) {
				if (!frameMap.containsKey(cur.getFrame())) {
					frameMap.put(cur.getFrame(), new HashSet<LU>());
				}
				frameMap.get(cur.getFrame()).add(cur);
				if (cur.getNum() == EMPTY_TEMPLATE) {
					EMPTY = cur;
				}
			}
		}
	}
	
	public LU readLU(int num) {
		Scanner scanner = getLUScanner(num);
		if (scanner == null) {
			return null;
		}
		scanner.nextLine();
		scanner.nextLine();
		String temp = scanner.next();
		while (!temp.contains("name")) {
			temp = scanner.next();
		}
		int spot = temp.indexOf('=');
		String begin = temp.substring(spot + 2, temp.length());
		while (!begin.contains("\"")) {
			begin += " " + scanner.next();
		}
		String name = begin.substring(0, begin.length() - 1);
		
		while (!temp.contains("frame")) {
			temp = scanner.next();
		}
		spot = temp.indexOf('=');
		String frame = temp.substring(spot + 2, temp.length() - 1);
		
		while (!temp.contains("frameID")) {
			temp = scanner.next();
		}
		spot = temp.indexOf('=');
		String frameID = temp.substring(spot + 2, temp.length() - 1);
		
		while (!temp.contains("totalAnnotated")) {
			temp = scanner.next();
		}
		spot = temp.indexOf('=');
		int ann = Integer.parseInt(temp.substring(spot + 2, temp.length() - 1));
		
		String frameInfo = "";
		while (!temp.contains("<frame>")) {
			temp = scanner.next();
		}
		while (!temp.contains("</frame>")) {
			frameInfo += (temp + " ");
			temp = scanner.next();
		}
		frameInfo += temp;
		
		//here is where we could add phrase type information
		
		scanner.close();
		return new LU(name, num, frame, frameID, frameInfo, ann, ann != 0 && getLUString(num).contains("sentence"));
	}
	
	class LU {
		private String name;
		private int num;
		private String frame;
		private int annotated;
		private String POS;
		private String label;
		private boolean isAnn;
		private String frameID;
		private String frameInfo;
		LU(String nameIn, int numIn, String frameIn, String frameIDIn, String frameInfoIn, int annotatedIn, boolean isAnnIn) {
			name = nameIn; num = numIn; frame = frameIn; annotated = annotatedIn; isAnn = isAnnIn; frameID = frameIDIn; frameInfo = frameInfoIn;
			int loc = name.indexOf('.');
			POS = name.substring(loc + 1);
			label = name.substring(0, loc);
		}
		String getName() {
			return name;
		}
		int getNum() {
			return num;
		}
		String getFrame() {
			return frame;
		}
		String getFrameInfo() {
			return frameInfo;
		}
		boolean isAnnotated() {
			return isAnn;
		}
		int getAnnotated() {
			return annotated;
		}
		String getFrameID() {
			return frameID;
		}
		@Override
		public String toString() {
			return getName();
		}
		public String getPOS() {
			return POS;
		}
		public String getLabel() {
			return label;
		}
		public boolean multiword() {
			return label.contains(" ");
		}
	}
	
	class Location implements Comparable<Location> {
		String start, end;
		int replen;
		public Location(String startIn, String endIn) {
			start = startIn; end = endIn; replen = -1;
		}
		int start() {
			return Integer.parseInt(start.substring(7, start.length() - 1));
		}
		int end() {
			return Integer.parseInt(end.substring(5, end.length() - 1));
		}
		int len() {
			return end() - start() + 1;
		}
		int replen() {
			return replen;
		}
		void setReplen(int replen) {
			this.replen = replen;
		}
		@Override
		public String toString() {
			return "Start: " + start() + "; End: " + end();
		}
		@Override
		public int compareTo(Location arg0) {
			return end() - arg0.end();
		}
	}
	
	public int peel(String input) {
		return Integer.parseInt(input.contains("start") ? input.substring(7, input.length() - 1) : input.substring(5, input.length() - 1));
	}
	
	public String wrap(int val, boolean start) {
		return start ? "start=\"" + val + "\"" : "end=\"" + val + "\"";
	}
	
	public String removeLeadingWhitespace(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isWhitespace(str.toCharArray()[i])) {
				return str.substring(i);
			}
		}
		return "";
	}
	
	public String replaceLUXML(LU replacer, LU replacee) {
		String replacement = replacee.getLabel();
		int lu = replacer.getNum();
		List<ArrayList<Location>> targets = (!NO_MULTIWORD_LEXEME && replacer.getName().contains(" ")) ? findTargetsMWord(lu) : findTargetsSWord(lu);
		String one = parseTexts(replacement, targets, lu);
		String two = parseSentences(one, replacement, targets);
		String three = parseTopLex(replacement, replacee.getNum(), replacee.getFrame(), replacee.getFrameID(), replacee.getPOS(), replacee.getFrameInfo(), two);
		String four = parseMidLex(replacement, replacee, three);
		return four.replace(gtRepl, "&gt;").replace(ltRepl, "&lt;").replace(quotRepl, "&quot;").replace(ampRepl, "&amp;");
	}
	
	public String findLexeme(LU replacee) {
		Scanner scanner = getLUScanner(replacee.getNum());
		String out = "";
		String temp;
		while (scanner.hasNext()) {
			temp = scanner.nextLine();
			if (temp.contains("<lexeme")) {
				out += temp + "\n";
			}
		}
		scanner.close();
		return out;
	}
	
	public String parseMidLex(String replacement, LU replacee, String current) {
		String lexrepl = findLexeme(replacee);
		Scanner scanner = new Scanner(current);
		String out = "";
		String temp;
		boolean added = false;
		while (scanner.hasNext()) {
			temp = scanner.nextLine();
			if (temp.contains("<lexeme") && !added) {
				out += lexrepl;
				added = true;
			} else if (!temp.contains("<lexeme")){
				out += temp + "\n";
			}
		}
		scanner.close();
		return out;
	}
	
	public String parseTopLex(String replacement, int replId, String frame, String frameID, String POS, String frameInfo, String current) {
		Scanner scanner = new Scanner(current);
		String out = "";
		String temp;
		String temp2;
		String temp3;
		while (scanner.hasNext()) {
			temp = scanner.nextLine();
			if (temp.contains("<lexUnit")) {
				temp2 = "";
				Scanner sub = new Scanner(temp);
				while (sub.hasNext()) {
					temp3 = sub.next();
					if (temp3.indexOf("name=\"") == 0) {
						while (temp3.indexOf(".") == -1) {
							temp3 = sub.next();
						}
						//temp3 = "name=\"" + replacement + temp3.substring(temp3.indexOf("."));
						temp3 = "name=\"" + replacement + "." + POS + "\"";
					} else if (temp3.indexOf("ID=\"") == 0) {
						temp3 = "ID=\"" + replId + "\"";
					} else if (temp3.indexOf("frame=\"") == 0) {
						//do something
						temp3 = "frame=\"" + frame + "\"";
					} else if (temp3.indexOf("POS=\"") == 0) {
						//do something
						temp3 = "POS=\"" + POS.toUpperCase() + "\"";
					} else if (temp3.indexOf("frameID=\"") == 0) {
						//do something
						temp3 = "frameID=\"" + frameID + "\"";
					}
					temp2 += temp3 + " ";
				}
				sub.close();
				temp = temp2;
			}
			out += temp + "\n";
		}
		scanner.close();
		
		int frameStart = out.indexOf("<frame>");
		int frameEnd = out.indexOf("</frame>") + 8;
		out = out.substring(0, frameStart) + frameInfo + out.substring(frameEnd);
		return out;
	}
	
	public String parseTexts(String replacement, List<ArrayList<Location>> targets, int num) {
		//Scanner scanner = new Scanner(current);
		//System.out.println(num);
		Scanner scanner = getLUScanner(num);
		if (scanner == null) {
			return null;
		}
		LU old = readLU(num);
		String oldform;
		String replform;
		
		String out = "";
		String temp;
		String temp2;
		int count = 0;
		int last;
		while (scanner.hasNext()) {
			//temp = scanner.nextLine();
			temp = scanner.nextLine().replace("&amp;", ampRepl).replace("&quot;", quotRepl).replace("&lt;", ltRepl).replace("&gt", gtRepl);
			if (temp.contains("<text>")) {
				temp = removeLeadingWhitespace(temp).substring(6);
				temp2 = "<text>";
				last = 0;
				for (int i = 0; i < targets.get(count).size(); i++) {
					try {
						temp2 += temp.substring(last, targets.get(count).get(i).start());
					} catch (StringIndexOutOfBoundsException e) {
						//Issue when target is identified twice
					}
					oldform = temp.substring(targets.get(count).get(i).start(), targets.get(count).get(i).end() + 1).toLowerCase();
					replform = magic(old, oldform, replacement);
					targets.get(count).get(i).setReplen(replform.length());
					//compare to old base to get word ending and capture length
					temp2 += replform;
					last = targets.get(count).get(i).end() + 1;
					if (i == targets.get(count).size() - 1) {
						temp2 += temp.substring(last, temp.length());
					} 
					
				}
				temp = temp2;
				//Here is where we could do conjugation matching
				count++;
			}
			out += temp + "\n";
		}
		scanner.close();
		return out;
	}
	
	public String magic(LU old, String oldform, String replacement) {
		//return replacement;
		if (old.getLabel().equalsIgnoreCase(oldform)) {
			return replacement;
		}
		oldform = oldform.toLowerCase();
		if (old.getPOS().equalsIgnoreCase("v")) {
			//verb
			if (oldform.endsWith("ing")) {
				replacement = (replacement.endsWith("e") ? replacement.substring(0, replacement.length() - 1) : replacement) + "ing";
			} else if (oldform.endsWith("s")) {
				replacement += "s";
			} else if (oldform.endsWith("d")) {
				replacement = (replacement.endsWith("e") ? replacement : replacement + "e") + "d";
			}
		} else if (old.getPOS().equalsIgnoreCase("n")) {
			//noun
			if (oldform.endsWith("s")) {
				if (replacement.endsWith("s") || replacement.endsWith("sh") || replacement.endsWith("ch") || replacement.endsWith("x") || replacement.endsWith("z")) {
					replacement += "es";
				} else {
					replacement += "s";
				}
			}
		}
		//System.out.println("\"" + oldform + "\" has been replaced with \"" + replacement + "\"");
		return replacement;
	}
	
	public String parseSentences(String current, String replacement, List<ArrayList<Location>> targets) {
		//Scanner scanner = getLUScanner(num);
		Scanner scanner = new Scanner(current);
		//if (scanner == null) {
		//	return null;
		//}
		String temp = "";
		String out = "";
		int count = 0;
		int offset;
		String temp2 = "";
		String temp3 = "";
		int oldEnd;
		//int oldStart;
		while (scanner.hasNext()) {
			temp = scanner.nextLine();
			while (!temp.contains("<sentence") && scanner.hasNext()) {
				out += temp + "\n";
				temp = scanner.nextLine();
			}
			if (!scanner.hasNext()) {
				break;
			}
			while (!temp.contains("</sentence>")) {
				if (temp.contains("end=\"") || temp.contains("start=\"")) {
					temp3 = "";
					Scanner scan = new Scanner(temp);
					while (scan.hasNext()) {
						temp2 = scan.next();
						if (temp2.contains("end=\"") || temp2.contains("start=\"")) {
							oldEnd = peel(temp2);
							offset = 0;
							for (Location loc : targets.get(count)) {
								if (oldEnd >= loc.end()) {
									//offset += replacement.length() - loc.len();
									offset += loc.replen() - loc.len();
								}
								//ind++;
							}
							temp2 = wrap(oldEnd + offset, temp2.contains("start=\""));
						} 
						/** else if (temp2.contains("start=\"")) {
							oldStart = peel(temp2);
							offset = 0;
							for (Location loc : targets.get(count)) {
								if (oldStart >= loc.end()) {
									offset += replacement.length() - loc.len();
								}
							}
							temp2 = wrap(oldStart + offset, true);
						}
						*/
						temp3 += temp2 + " ";
					}
					scan.close();
					temp = temp3;
				}
				out += temp + "\n";
				temp = scanner.nextLine();
			}
			out += temp + "\n";
			count++;
		}
		out += temp;
		scanner.close();
		//System.out.println(ind);
		//return out.replace("&amp;", ampRepl).replace("&quot;", quotRepl).replace("&lt;", ltRepl).replace("&gt", gtRepl);
		return out;
	}
	
	public List<ArrayList<Location>> findTargetsSWord(int num) {
		Scanner scanner = getLUScanner(num);
		if (scanner == null) {
			return null;
		}
		List<ArrayList<Location>> targets = new ArrayList<>();
		int count = 0;
		String temp, end, start;
		while (scanner.hasNext()) {
			temp = ""; end = ""; start = "";
			while (!(temp.contains("Target\">") || temp.contains("</sentence>")) && scanner.hasNext()) {
				temp = scanner.next();
			}
			if (temp.contains("</sentence>")) {
				count++;
			} else if (scanner.hasNext()){
				while (!temp.contains("end") && scanner.hasNext()) {
					temp = scanner.next();
				}
				end = temp;
				while (!temp.contains("start") && scanner.hasNext()) {
					temp = scanner.next();
				}
				start = temp;
				while (!temp.contains("Target\"/>") && scanner.hasNext()) {
					temp = scanner.next();
				}
				if (count >= targets.size()) {
					targets.add(new ArrayList<Location>());
				}
				targets.get(count).add(new Location(start, end));
			}
		}
		scanner.close();
		for (ArrayList<Location> list : targets) {
			Collections.sort(list);
		}
		return targets;
	}
	
	public List<ArrayList<Location>> findTargetsMWord(int num) {
		Scanner scanner = getLUScanner(num);
		if (scanner == null) {
			return null;
		}
		List<ArrayList<Location>> targets = new ArrayList<>();
		int count = 0;
		String temp = "";
		String end, start;
		
		while (!(temp.contains("Target\">")) && scanner.hasNext()) {
			temp = scanner.next();
		}
		
		while (scanner.hasNext()) {
			end = ""; start = "";
			/**
			while (!(temp.contains("Target\">") || temp.contains("</sentence>")) && scanner.hasNext()) {
				temp = scanner.next();
			}
			*/
			if (temp.contains("</sentence>")) {
				count++;
				while (!(temp.contains("Target\">")) && scanner.hasNext()) {
					temp = scanner.next();
				}
			} else if (scanner.hasNext()){
				while (!temp.contains("end") && scanner.hasNext()) {
					temp = scanner.next();
				}
				end = temp;
				while (!temp.contains("start") && scanner.hasNext()) {
					temp = scanner.next();
				}
				start = temp;
				//System.out.println(start);
				/**
				while (!temp.contains("Target\"/>") && scanner.hasNext()) {
					temp = scanner.next();
				}
				*/
				if (count >= targets.size()) {
					targets.add(new ArrayList<Location>());
				}
				while (!(temp.contains("Target\">") || temp.contains("</sentence>")) && scanner.hasNextLine()) {
					temp = scanner.nextLine();
					//System.out.println(temp);
					if (temp.contains("end") && temp.contains("Target\"/>")) {
						Scanner line = new Scanner(temp);
						while (line.hasNext()) {
							String word = line.next();
							if (word.contains("end") && peel(word) > peel(end)) {
								end = word;
							} else if (word.contains("start") && peel(word) < peel(start)) {
								start = word;
							}
						}
						line.close();
						//System.out.println(temp);
					}
					//temp = scanner.next();
				}
				//System.out.println(new Location(start, end));
				targets.get(count).add(new Location(start, end));
				if (new Location(start, end).end() < new Location(start, end).start()) {
					System.out.println("Error: " + num);
				}
			}
		}
		scanner.close();
		for (ArrayList<Location> list : targets) {
			Collections.sort(list);
		}
		return targets;
	}
}
