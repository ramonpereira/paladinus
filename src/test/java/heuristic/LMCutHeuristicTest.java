package heuristic;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import main.java.paladinus.Global;
import main.java.paladinus.Options;
import main.java.paladinus.heuristic.LMCutHeuristic;
import main.java.paladinus.parser.SasParser;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.util.TranslateFONDUtils;

/**
*
* @author Ramon Fraga Pereira
*
*/
public class LMCutHeuristicTest {

	/**
	 * blocksworld-new
	 */
	
	@Test
	public void testLMCutHeuristicBW1() throws IOException, InterruptedException {
		String domainFile = "benchmarks/blocksworld-new/domain.pddl";
		String instanceFile = "benchmarks/blocksworld-new/p1.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 4);
	}
	
	@Test
	public void testLMCutHeuristicBW11() throws IOException, InterruptedException {
		String domainFile = "benchmarks/blocksworld-new/domain.pddl";
		String instanceFile = "benchmarks/blocksworld-new/p11.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 13);
	}
	
	/**
	 * doors
	 */
	
	@Test
	public void testLMCutHeuristicDoors3() throws IOException, InterruptedException {
		String domainFile = "benchmarks/doors/domain.pddl";
		String instanceFile = "benchmarks/doors/p3.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 4);
	}
	
	@Test
	public void testLMCutHeuristicDoors8() throws IOException, InterruptedException {
		String domainFile = "benchmarks/doors/domain.pddl";
		String instanceFile = "benchmarks/doors/p8.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 9);
	}
	
	/**
	 * triangle-tireworld
	 */
	
	@Test
	public void testLMCutHeuristicTWTruck9() throws IOException, InterruptedException {
		String domainFile = "benchmarks/tireworld-truck/domain.pddl";
		String instanceFile = "benchmarks/tireworld-truck/p9.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 3);
	}
	
	@Test
	public void testLMCutHeuristicTWTruck54() throws IOException, InterruptedException {
		String domainFile = "benchmarks/tireworld-truck/domain.pddl";
		String instanceFile = "benchmarks/tireworld-truck/p54.pddl";

		TranslateFONDUtils.translateFOND(domainFile, instanceFile);

		String filename = TranslateFONDUtils.toAbsolute("output.sas");
		InputStream sasFile = new FileInputStream(new File(filename));
		SasParser parser = new SasParser();
		Problem problem = parser.parse(sasFile);

		Global.options = new Options();

		problem.finishInitializationAndPreprocessing();
		
		LMCutHeuristic lmcut = new LMCutHeuristic(problem);
		System.out.println("> LMCut Heuristic ");
		double lmcutHvalue = lmcut.getHeuristic(problem.getSingleInitialState());
		System.out.println("$> h-value = " + lmcutHvalue);
		assertTrue(lmcutHvalue == 5);
	}
}
