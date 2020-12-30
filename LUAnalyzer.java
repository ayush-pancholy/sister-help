package first_package;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LUAnalyzer {
	public static String prefix = "C:\\Users\\14086\\Downloads\\fndata-1.7\\fndata-1.7\\lu\\";
	public static String begin = "lu";
	public static String ext = ".xml";
	public static final int LAST_LU = 18820;
	public static final double ANNO_THRESH = 50;
	
	public static void main(String[] args) {
		LUAnalyzer analyzer = new LUAnalyzer();
		analyzer.findLUs();
	}
	
	public Scanner readLU(int num) {
		File file = new File(prefix + begin + Integer.toString(num) + ext);
		try {
			return new Scanner(file);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	public String getLUFrame(int num) {
		Scanner scanner = readLU(num);
		if (scanner == null) {
			return null;
		} else {
			scanner.nextLine();
			scanner.nextLine();
			String temp = scanner.next();
			while (!temp.contains("frame")) {
				temp = scanner.next();
			}
			int spot = temp.indexOf('=');
			scanner.close();
			return temp.substring(spot + 2, temp.length() - 1);
		}
	}
	
	public String getLUName(int num) {
		Scanner scanner = readLU(num);
		if (scanner == null) {
			return null;
		} else {
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
			scanner.close();
			return begin.substring(0, begin.length() - 1);
		}
	}
	
	public int findLU(String lu) {
		for (int i = 0; i < LAST_LU; i++) {
			if (lu.equals(getLUName(i))) {
				return i;
			}
		}
		return -1;
	}
	
	public Set<Integer> getSisters(int num) {
		Set<Integer> sisters = new HashSet<Integer>();
		String frame = getLUFrame(num);
		if (frame == null) {
			return null;
		}
		for (int i = 0; i < LAST_LU; i++) {
			if (num != i && frame.equals(getLUFrame(i))) {
				sisters.add(i);
			}
		}
		return sisters;
	}
	
	public Set<Integer> getFrameLUs(String frame) {
		Set<Integer> lus = new HashSet<Integer>();
		for (int i = 0; i < LAST_LU; i++) {
			if (frame.equals(getLUFrame(i))) {
				lus.add(i);
			}
		}
		return lus;
	}
	
	
	public void findLUs() {
		HashMap<Integer, Set<Integer>> vals = new HashMap<>();
		HashMap<String, Set<Integer>> frames = new HashMap<>();
		for (int i = 0; i < LAST_LU; i++) {
			String frame = getLUFrame(i);
			if (isUnannotated(i) && frame != null) {
				if (!frames.containsKey(frame)) {
					frames.put(frame, getFrameLUs(frame));
				}
				Set<Integer> annoSisters = annotatedSisters(i, frames.get(frame));
				if (percentAnnotated(frame, frames.get(frame)) >= ANNO_THRESH && annoSisters != null && !annoSisters.isEmpty()) {
					vals.put(i, annoSisters);
				}
			}
			if (i % 500 == 0) {
				System.out.println((int)((100*(((double)i))) / ((double)LAST_LU) + 0.5) + "% complete. Optimizable LU's found: " + vals.size());
			}
		}
	}
	
	public double percentAnnotated(String frame, Set<Integer> frameLUs) {
		double total = 0;
		double ann = 0;
		for (Integer lu : frameLUs) {
			total++;
			if (!isUnannotated(lu)) {
				ann++;
			}
		}
		return total == 0 ? 0 : 100*ann/total;
	}
	
	public boolean isUnannotated(int num) {
		Scanner scanner = readLU(num);
		if (scanner == null) {
			return false;
		} else {
			scanner.nextLine();
			scanner.nextLine();
			String temp = scanner.next();
			while (!temp.contains("totalAnnotated")) {
				temp = scanner.next();
			}
			int spot = temp.indexOf('=');
			int ann = Integer.parseInt(temp.substring(spot + 2, temp.length() - 1));
			scanner.close();
			return ann == 0;
		}
	}
	
	public Set<Integer> annotatedSisters(int num, Set<Integer> frameLUs) {
		Set<Integer> annSisters = new HashSet<Integer>();
		if (frameLUs == null) {
			return null;
		}
		for (Integer sister : frameLUs) {
			if (num != sister && !isUnannotated(sister)) {
				annSisters.add(sister);
			}
		}
		return annSisters;
	}
	
	public int countUnannotated() {
		int temp = 0;
		for (int i = 0; i < LAST_LU; i++) {
			if (isUnannotated(i)) {
				temp++;
			}
		}
		return temp;
	}
	

}
