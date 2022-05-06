package paladinus.simulator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class PlanTester {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		String planFilename = args[0];
		System.out.println("Parsing " + planFilename);
		readPlan(planFilename);
	}

	public static HashMap<String, String> readPlan(String planFilename) throws FileNotFoundException, IOException {
		HashMap<String, String> simplePolicy = new HashMap<String, String>();
		List<String> lines = PlanReader.readFile(planFilename);
		String nextState = "";
		String nextOperator;
		for (String l : lines) {
			// System.out.println(l);
			if (l.contains("if")) {
				int idx = l.lastIndexOf(">");
				nextState = l.substring(3, idx + 1);
				// System.out.println(nextState);
			} else if (l.contains("then")) {
				int idx = l.indexOf(";");
				nextOperator = l.substring(5, idx);
				// System.out.println(nextOperator);
				simplePolicy.put(nextState, nextOperator);
			}
		}
		System.out.println("simple policy: " + simplePolicy);
		return simplePolicy;
	}
}
