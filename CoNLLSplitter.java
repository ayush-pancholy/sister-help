package first_package;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CoNLLSplitter {
	public static String sents = "C:\\\\Users\\\\14086\\\\Documents\\\\Cross Validation Data\\\\LUs_With_Removal\\\\open_sesame_v1_data\\\\fn1.7\\\\fn1.7.exemplar.train.syntaxnet.conll.sents";
	public static String conll = "C:\\Users\\14086\\Documents\\Cross Validation Data\\LUs_With_Removal\\open_sesame_v1_data\\fn1.7\\fn1.7.exemplar.train.syntaxnet.conll";
	public static String output = "C:\\Users\\14086\\Documents\\Cross Validation Data\\ordered.txt";
	public static String anno_out = "C:\\Users\\14086\\Documents\\Cross Validation Data\\anno_out.txt";
	public static String pieces = "C:\\Users\\14086\\Documents\\Cross Validation Data\\five_pieces\\piece_";
	public static int numPieces = 5;
	public List<ConllPair> conllSents;
	public List<String> sentsSents;
	public List<String> anomalies;
	
	public static void main(String[] args) {
		CoNLLSplitter splitter = new CoNLLSplitter();
		try {
			//splitter.conllRead();
			//splitter.sentsRead();
			//splitter.writeOrdered();
			//splitter.crossExamine();
			splitter.conllSplit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public CoNLLSplitter() {
		conllSents = new ArrayList<ConllPair>();
		sentsSents = new ArrayList<String>();
		anomalies = new ArrayList<String>();
	}
	
	public class ConllPair {
		public String sent;
		public String anno;
		
		public ConllPair(String sentIn, String annoIn) {
			sent = sentIn;
			anno = annoIn;
		}
		
		public String getSent() {
			return sent;
		}
		
		public String getAnno() {
			return anno;
		}
	}
	
	public void conllSplit() throws IOException {
		Scanner scanner = new Scanner(new File(conll), "utf-8");
		int numSentences = 0;
		String line;
		int last = -1;
		BufferedWriter writer;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line.length() > 0 && line.startsWith("1\t")) {
				numSentences++;
			}
		}
		scanner.close();
		int div = (int)(((double)numSentences)/((double)numPieces));
		
		scanner = new Scanner(new File(conll), "utf-8");
		int sent = 0;
		int filename = 1;
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pieces + Integer.toString(filename) + ".conll"), "UTF-8"));
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line.length() > 0 && line.startsWith("1\t")) {
				sent++;
			}
			if (sent > div) {
				filename++;
				writer.close();
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pieces + Integer.toString(filename) + ".conll"), "UTF-8"));
				sent = 1;
			}
			writer.write(line.concat("\n"));
			writer.flush();
			int pct = (int)(100*((((double)sent)/((double)div))));
			if (pct != last) {
				System.out.println("Piece " + filename + ": " + pct + "% done!");
				last = pct;
			}
		}
		writer.close();
		System.out.println(div);
	}
	
	public void writeOrdered() throws IOException {
		System.out.println("Writing to file...");
		FileWriter writer = new FileWriter(output);
		FileWriter writer2 = new FileWriter(anno_out);
		int num = 0;
		
		for (ConllPair c : conllSents) {
			for (String s: sentsSents) {
				if (sentCompare(s, c.getSent())) {
					writer.write((s + "\n"));
					writer.flush();
					writer2.write(c.getAnno() + "\n");
					writer2.flush();
					sentsSents.remove(s);
					//System.out.println("Sentence done!");
					num++;
					break;
				}
			}
			if (num % 500 == 0) {
				System.out.println(num + " sentences done!");
			}
		}
		
		//writer.write("hello world");
		writer.close();
		writer2.close();
	}
	
	public void crossExamine() {
		System.out.println("Cross-examining...");
		double i = 0;
		double len = sentsSents.size();
		boolean set = false;
		boolean found;
		for (String s : sentsSents) {
			found = false;
			int temp = (int)(100.0*i/len);
			
			for (ConllPair c : conllSents) {
				if (sentCompare(s, c.getSent())) {
					found = true;
					conllSents.remove(c);
					break;
				}
			}
			if (!found) {
				anomalies.add(s);
				System.out.println(s);
			}
			
			if (!set && temp % 5 == 0) {
				System.out.println(temp + "% done!");
				set = true;
			} else if (temp % 5 != 0){
				set = false;
			}
		}
		System.out.println(anomalies.size());
		System.out.println(anomalies.get(0));
	}
	
	public void conllRead() throws FileNotFoundException {
		System.out.println("Reading from CoNLL...");
		Scanner scanner = new Scanner(new File(conll), "utf-8");
		String cur;
		String temp;
		String anno;
		char start = 'b';
		char end = '_';
		int begin;
		int finish;
		
		while (scanner.hasNextLine()) {
			temp = scanner.nextLine();
			cur = "";
			anno = "";
			while (scanner.hasNextLine()) {
				begin = temp.indexOf(start) + 2;
				finish = temp.indexOf(end, begin + 2) - 2;
				if (begin < 2) {
					break;
				}
				anno += (temp + "\n");
				try {
				cur += temp.substring(begin, finish);
				} catch (Exception e) {
					System.out.println(temp);
					System.exit(-1);
				}
				cur += " ";
				temp = scanner.nextLine();
			}
			//System.out.println(anno);
			conllSents.add(new ConllPair(cur, anno));
			//System.out.println(cur);
		}
		//System.out.println(conllSents.size());
		scanner.close();
	}
	
	public void sentsRead() throws FileNotFoundException {
		System.out.println("Reading from sents...");
		Scanner scanner = new Scanner(new File(sents), "utf-8");
		while (scanner.hasNextLine()) {
			sentsSents.add(scanner.nextLine());
		}
		scanner.close();
		//System.out.println(sentsSents.get(0));
	}
	
	public boolean sentCompare(String sent, String con) {
		Scanner ss = new Scanner(sent);
		Scanner cs = new Scanner(con);
		String st; String ct;
		while (ss.hasNext()) {
			if (!cs.hasNext()) {
				ss.close();
				cs.close();
				return false;
			}
			st = ss.next();
			ct = cs.next();
			
			while (!st.matches("[a-zA-Z]+") && ss.hasNext()) {
				st = ss.next();
			}
			while (!ct.matches("[a-zA-Z]+") && cs.hasNext()) {
				ct = cs.next();
			}
			if (!st.contentEquals(ct)) {
				ss.close();
				cs.close();
				return false;
			}
		}
		if (cs.hasNext()) {
			ss.close();
			cs.close();
			return false;
		}
		ss.close();
		cs.close();
		return true;
		
	}
}
