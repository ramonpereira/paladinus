package heuristic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import main.java.paladinus.Global;
import main.java.paladinus.Options;
import main.java.paladinus.heuristic.HMaxHeuristic;
import main.java.paladinus.parser.SasParser;
import main.java.paladinus.problem.Problem;

/**
*
* @author Ramon Fraga Pereira
*
*/
public class HMaxHeuristicTest {

	@Test
	public void testMaxHeuristicFaults1() throws IOException, InterruptedException {
		String domainFile = "benchmarks/faults/d1.pddl";
		String instanceFile = "benchmarks/faults/p1.pddl";

		this.translateFOND(domainFile, instanceFile);

		String filename = toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		HMaxHeuristic hmax = new HMaxHeuristic(problem);
		System.out.println("> MAX Heuristic ");
		System.out.println("$> h-value = " + hmax.getHeuristic(problem.getSingleInitialState()));
	}

	private String toAbsolute(String resFile) {
		File file = new File(resFile);
		return file.getAbsolutePath();
	}

	private void translateFOND(String domainFile, String instanceFile) throws IOException, InterruptedException {
		String cmd = "translator-fond/translate.py " + domainFile + " " + instanceFile;
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
	}

}
