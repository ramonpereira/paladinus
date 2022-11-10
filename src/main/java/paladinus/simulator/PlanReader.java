package main.java.paladinus.simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import main.java.paladinus.Global.ExitCode;
import main.java.paladinus.explicit.ExplicitAxiomEvaluator;
import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.parser.SasParser;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.search.policy.Policy;
import main.java.paladinus.state.Operator;
import main.java.paladinus.util.Pair;

/**
 *
 * @author Robert Mattmueller
 *
 */
public class PlanReader {
	static Problem problem;

	ExplicitAxiomEvaluator axiomEvaluator;

	@Argument(index = 0, usage = "path to SAS file", required = true, metaVar = "sas_file")
	private String sasFile;

	@Argument(index = 1, usage = "path to plan file", required = true, metaVar = "plan_file")
	private String planFile;

	@Option(name = "-h", aliases = "-help", usage = "print this message")
	private boolean help = false;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		PlanReader reader = new PlanReader(null);
		final CmdLineParser parser = new CmdLineParser(reader);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			if (reader.help) {
				reader.printHelp(parser);
				reader.help = false;
			} else {
				// Handling of wrong arguments.
				System.err.println("Error: " + e.getMessage() + "\n");
				reader.help = true;
			}
		}

		if (reader.help) {
			reader.printHelp(parser);
			ExitCode.EXIT_INPUT_ERROR.exit();
		}
		System.out.println("Parsing " + reader.sasFile);
		PlanReader.problem = new SasParser().parse(new FileInputStream(reader.sasFile));
		assert (problem.isFullObservable);
		// Global.problem.getSingleInitialState().uniqueID; TODO check this? was
		// uniqueID();

		System.out.println("Parsing " + reader.planFile);
		reader.axiomEvaluator = new ExplicitAxiomEvaluator(PlanReader.problem);
		Policy plan = reader.readPlan(problem, reader.planFile);

		// System.out.println(plan);
		double planCost = new PlanSimulator(PlanReader.problem).performValueIteration(plan);

		System.out.println("Plan cost (expected number of steps to goal): " + planCost);
		System.out.println(plan.toString());

	}

	public PlanReader(Problem problem) {
		if (problem != null) {
			axiomEvaluator = new ExplicitAxiomEvaluator(problem);
		}
	}

	private void printHelp(CmdLineParser parser) {
		System.err.println("Arguments for PlanReader:");
		parser.printUsage(System.err);

		System.err.println(
				"\nExample: ../data/benchmarks-fond/blocksworld_p1.sas ../data/plans/blocksworld_p1.fond_plan");
	}

	private int parseEntry(List<Pair<Integer, Integer>> propositionIndices, List<Operator> operators, String[] policy,
			Policy plan, int currentIndex) {
		int numPreconditions = Integer.parseInt(policy[currentIndex]);

		List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();
		for (int i = 0; i < numPreconditions; i++) {
			int precond = Integer.parseInt(policy[currentIndex + i + 1]);
			Pair<Integer, Integer> pair = propositionIndices.get(precond);
			if (pair != null) {
				pairs.add(pair);
			} else {
				// System.err.println("WARNING: Variable not found in SAS encoding. Maybe
				// compiled away?");
			}
		}

		// FIXME Can we really be sure that, if a proposition is false in the
		// Boolean encoding, and if there is no proposition in a variable group
		// that leads to the corresponding variable getting a defined value,
		// then that variable *must* have a "<none of those>" value, which is
		// the correct one to chose?
		int[] values = new int[PlanReader.problem.variableNames.size()];
		Arrays.fill(values, -1);
		for (int i = 0; i < pairs.size(); i++) {
			int var = pairs.get(i).first;
			int val = pairs.get(i).second;
			assert var < values.length;
			assert values[var] == -1;
			values[var] = val;
		}
		for (int var = 0; var < values.length; var++) {
			if (values[var] == -1) {
				values[var] = PlanReader.problem.domainSizes.get(var) - 1;

				String propositionName = PlanReader.problem.propositionNames.get(var).get(values[var]);

				if (!propositionName.startsWith("(not ")) {
					System.err.println("Ooops. Wrong value ...");
					ExitCode.EXIT_CRITICAL_ERROR.exit();
				}
			}
		}

		int action = Integer.parseInt(policy[currentIndex + numPreconditions + 1]);
		Operator operator = operators.get(action);

		if (operator != null) {
			plan.addEntry(new ExplicitState(problem, values, axiomEvaluator), operator);
		} else {
			// System.err.println("WARNING: Operator not found in SAS encoding. Maybe
			// compiled away?");
		}

		return numPreconditions + 2;
	}

	private void parsePolicy(List<Pair<Integer, Integer>> propositionIndices, List<Operator> operators, String[] policy,
			Policy plan) {
		int numEntries = Integer.parseInt(policy[1]);
		int currentIndex = 2;
		for (int i = 0; i < numEntries; i++) {
			System.out.println("Parsing policy entry " + i + " of " + numEntries);
			int offset = parseEntry(propositionIndices, operators, policy, plan, currentIndex);
			currentIndex += offset;
		}

	}

	private List<String> parsePropositionsOrActions(String propositions) {
		Pattern p = Pattern.compile("\\(.*?\\)");
		Matcher m = p.matcher(propositions);

		List<String> result = new ArrayList<String>();

		while (m.find()) {
			String find = m.group();
			// System.err.println(find);
			result.add(find);
		}

		return result;
	}

	public static List<String> readFile(String filename) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		String line;
		List<String> lines = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.equals("")) {
				lines.add(line);
			}
		}
		reader.close();
		return lines;
	}

	public Policy readPlan(Problem problem, String filename) throws FileNotFoundException, IOException {
		Policy plan = new Policy(problem);
		List<String> lines = readFile(filename);
		assert lines.size() == 5;
		assert lines.get(1).equals("%%");
		assert lines.get(3).equals("%%");
		List<String> propositions = parsePropositionsOrActions(lines.get(0));
		List<String> actions = parsePropositionsOrActions(lines.get(2));

		List<Pair<Integer, Integer>> propositionIndices = translatePropositionIndices(propositions);
		List<Operator> operators = translateActionIndices(actions);

		String[] policy = lines.get(4).split(" ");

		parsePolicy(propositionIndices, operators, policy, plan);

		return plan;
	}

	private List<Operator> translateActionIndices(List<String> actions) {
		List<Operator> result = new ArrayList<Operator>();
		Set<Operator> ops = PlanReader.problem.getOperators();
		Map<String, Operator> inverseOperatorNames = new HashMap<String, Operator>();
		for (Operator op : ops) {
			String name = op.getName();
			name = "(" + name + ")";
			inverseOperatorNames.put(name, op);
		}
		for (String action : actions) {
			Operator op = inverseOperatorNames.get(action);

			if (op == null) {
				System.err.println("WARNING: Removed unnecessary action " + action);
			}

			result.add(op);
		}
		return result;
	}

	private List<Pair<Integer, Integer>> translatePropositionIndices(List<String> propositions) {
		List<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
		List<List<String>> propositionNamesInProblem = PlanReader.problem.propositionNames;
		Map<String, Pair<Integer, Integer>> inversePropositionNames = new HashMap<String, Pair<Integer, Integer>>();
		for (int var = 0; var < propositionNamesInProblem.size(); var++) {
			for (int val = 0; val < propositionNamesInProblem.get(var).size(); val++) {
				String propName = propositionNamesInProblem.get(var).get(val);
				inversePropositionNames.put(propName, new Pair<Integer, Integer>(var, val));
			}
		}
		for (String prop : propositions) {
			Pair<Integer, Integer> pair = inversePropositionNames.get(prop);
			result.add(pair);
		}
		return result;
	}
}
