package paladinus.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import paladinus.Global;
import paladinus.heuristic.Heuristic;
import paladinus.problem.Problem;
import paladinus.simulator.PlanSimulator;

/**
 * A heuristic search algorithm is an explicit state search algorithm guided by
 * a heuristic function.
 *
 * @author Robert Mattmueller
 */
public abstract class HeuristicSearch extends AbstractSearch {

	protected Map<BigInteger, SearchNode> stateNodeMap = new LinkedHashMap<BigInteger, SearchNode>();
	
	/**
	 * Heuristic estimator for the initialization of leaf nodes
	 */
	protected Heuristic heuristic;

	public static ExpansionRules[][] expansionRules;
	
	protected int dumpingCounterPlan = 0;
	protected int dumpingCounterStateSpace = 0;
	
	/**
	 * 
	 * @param problem
	 * @param heuristics
	 */
	public HeuristicSearch(Problem problem, Heuristic[] heuristics) {
		super(problem);
	}
	/**
	 * Create a search guided by a heuristic.
	 *
	 * @param heuristic used during search
	 */
	public HeuristicSearch(Problem problem, Heuristic heuristic) {
		super(problem);
		this.heuristic = heuristic;
	}

	/**
	 * Perform one iteration of the search algorithm, i.e. choice of nodes to
	 * expand, expansion, and (recursive) update.
	 *
	 */
	public abstract void doIteration();
	
	public void dumpStateSpace() {
		File plan = new File("statespace_" + dumpingCounterStateSpace + ".dot");
		dumpingCounterStateSpace++;
		try {
			FileWriter writer = new FileWriter(plan);
			writer.write(new GraphvizWriter(this).createOutputStateSpace(true));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpPartialSolution() {
		dumpPartialSolution("plan" + "_" + dumpingCounterPlan);
		dumpingCounterPlan++;
	}

	public void dumpPartialSolution(String filename) {
		File plan = new File(filename + ".dot");
		try {
			FileWriter writer = new FileWriter(plan);
			writer.write(new GraphvizWriter(this).createOutputStateSpace(false));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void validatePolicy() {
		if(Global.options.validatePolicy()) {
			PlanSimulator.savePlanAsDot(problem, this.getPolicy(), Global.options.getExportDotFilename());
			
			ProcessBuilder processBuilder = new ProcessBuilder(new File("validators/graphviz_validator.py").getAbsolutePath(), PlanSimulator.goalNodes.toString());
		    processBuilder.redirectErrorStream(true);
	
		    Process process;
			try {
				process = processBuilder.start();
				InputStream result = process.getInputStream();
				StringWriter writer = new StringWriter();
				IOUtils.copy(result, writer, "UTF-8");
				String output = writer.toString();
				
				if(output.contains("Strong Cyclic: True"))
					this.getPolicy().setValid(true);
				
				System.out.println(output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(Global.options.validatePolicyPRP()) {
			this.getPolicy().printPRPpolicyToFile("policy");
			
			String domain = new File(Global.options.getDomainFilename()).getAbsolutePath();
			String problem = new File(Global.options.getInstanceFilename()).getAbsolutePath();
			
		    ProcessBuilder processBuilder = new ProcessBuilder(new File("validators/validator.py").getAbsolutePath(), "-d" + domain, "-p" + problem, "-s" + "policy-translated.out");
		    processBuilder.redirectErrorStream(true);
	
		    Process process;
			try {
				process = processBuilder.start();
				InputStream result = process.getInputStream();
				StringWriter writer = new StringWriter();
				IOUtils.copy(result, writer, "UTF-8");
				String output = writer.toString();
				
				if(output.contains("Strong Cyclic: True"))
					this.getPolicy().setValid(true);
				
				System.out.println(output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Heuristic getHeuristic() {
		return heuristic;
	}
}
