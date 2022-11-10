package main.java.paladinus.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.javabdd.BDD;
import main.java.paladinus.Global;
import main.java.paladinus.explicit.ExplicitCondition;
import main.java.paladinus.explicit.ExplicitEffect;
import main.java.paladinus.explicit.ExplicitOperator;
import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.problem.FullyObservableProblem;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Operator;
import main.java.paladinus.symbolic.BDDManager;
import main.java.paladinus.symbolic.PartiallyObservableProblem;
import main.java.paladinus.util.Pair;

/**
 * Parser for modified SAS+ format. The format differs from the one used as
 * input to Fast Downward (Helmert 2006) in the following ways:
 * <ul>
 * <li>The operators may have nondeterministic effects.</li>
 * <li>Axioms (axiom sections) are not supported.</li>
 * <li>Sensing operators are supported which observe facts.</li>
 * </ul>
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class SasParser {

	/**
	 * Keyword for begin of version section.
	 */
	private static final String RW_BEGINVERSION = "begin_version";

	/**
	 * Keyword for end of version section.
	 */
	private static final String RW_ENDVERSION = "end_version";

	/**
	 * Keyword for begin of metric section.
	 */
	private static final String RW_BEGINMETRIC = "begin_metric";

	/**
	 * Keyword for end of metric section.
	 */
	private static final String RW_ENDMETRIC = "end_metric";

	/**
	 * Keyword for begin of each variable.
	 */
	private static final String RW_BEGINVAR = "begin_variable";

	/**
	 * Keyword for end of each variable.
	 */
	private static final String RW_ENDVAR = "end_variable";

	/**
	 * Keyword for begin of initial state section.
	 */
	private static final String RW_BEGINSTATE = "begin_state";

	/**
	 * Keyword for end of initial state section.
	 */
	private static final String RW_ENDSTATE = "end_state";

	/**
	 * Keyword for begin of goal description section.
	 */
	private static final String RW_BEGINGOAL = "begin_goal";

	/**
	 * Keyword for end of goal description section.
	 */
	private static final String RW_ENDGOAL = "end_goal";

	/**
	 * Keyword for begin of operator description.
	 */
	private static final String RW_BEGINOP = "begin_operator";

	/**
	 * Keyword for end of operator description.
	 */
	private static final String RW_ENDOP = "end_operator";

	/**
	 * Keyword for begin of a mutex group.
	 */
	private static final String RW_BEGINMUTEXGROUP = "begin_mutex_group";

	/**
	 * Keyword for end of operator description.
	 */
	private static final String RW_ENDMUTEXGROUP = "end_mutex_group";

	/**
	 * Keyword for begin of an axiom description.
	 */
	private static final String RW_BEGINAXIOM = "begin_rule";

	/**
	 * Keyword for end of an axiom description.
	 */
	private static final String RW_ENDAXIOM = "end_rule";

	/**
	 * Buffered reader reading the input stream.
	 */
	private BufferedReader reader;

	/**
	 * Number of variables.
	 */
	private int numberOfVariables;

	/**
	 * Explicit goal condition.
	 */
	private ExplicitCondition goal;

	/**
	 * Variable names.
	 */
	private ArrayList<String> variableNames;

	/**
	 * Variable domain sizes.
	 */
	private ArrayList<Integer> domainSizes;

	/**
	 * Variable axiom layers.
	 */
	private ArrayList<Integer> axiomLayer;

	/**
	 * Set of the planning task's operators.
	 */
	private LinkedHashSet<Operator> operators;

	/**
	 * Axioms.
	 */
	private Set<OperatorRule> axioms;

	/**
	 * BDDManager used for POND problems
	 */
	private BDDManager BDDManager;

	/**
	 * List of proposition names corresponding to variable-value pairs.
	 * propositionNames[i][j] is the name of the proposition corresponding to the
	 * equality (var_i = j).
	 */
	private ArrayList<List<String>> propositionNames;

	/**
	 * Denotes if problem to be parsed is full observable or not.
	 */
	private boolean fullObservable;

	/**
	 * Valuation of initial state (fully observable problem).
	 */
	private int[] initialValuation;

	/**
	 * Initial belief state as BDD (partially observable problem).
	 */
	private BDD initialBDD;

	/**
	 * Default values of derived variables.
	 */
	private ArrayList<Integer> defaultValues;

	/**
	 * Variables which are not unknown in the initial state.
	 */
	private Set<Integer> variablesWhichAreInitiallyKnown;

	/**
	 * Indicates if action costs are used or not.
	 */
	private boolean actionCostsUsed;

	/**
	 * Assert equality of an actual and a given string and raise an error if they
	 * are not equal.
	 *
	 * @param actual   Actual string value
	 * @param expected Expected string value
	 */
	private void assertEq(String actual, String expected) {
		if (actual.equals(expected)) {
			return;
		}
		System.err.println("Parse error: Expected \"" + expected + "\" but read \"" + actual + "\".");
		Global.ExitCode.EXIT_INPUT_ERROR.exit();
	}

	/**
	 * Assert that a given token array has a given length and raise an error if this
	 * is not the case.
	 *
	 * @param tokens   Token array
	 * @param expected Expected length
	 */
	private void assertLen(String[] tokens, int expected) {
		if (tokens.length == expected) {
			return;
		}
		System.err.print("Parse error: Expected " + expected + " tokens but read \"");
		for (String token : tokens) {
			System.err.print(token + " ");
		}
		System.err.println("\".");
		Global.ExitCode.EXIT_INPUT_ERROR.exit();
	}

	/**
	 * Assert that a given token array has a given minimal length and raise an error
	 * if this is not the case.
	 *
	 * @param tokens   Token array
	 * @param expected Minimal expected length
	 */
	private void assertLenGeq(String[] tokens, int expected) {
		if (tokens.length >= expected) {
			return;
		}
		System.err.print("Parse error: Expected " + expected + " tokens but read \"");
		for (String token : tokens) {
			System.err.print(token + " ");
		}
		System.err.println("\".");
		Global.ExitCode.EXIT_INPUT_ERROR.exit();
	}

	/**
	 * Initialize reader.
	 *
	 * @param stream Input stream to read from
	 */
	private void initialize(InputStream stream) {
		reader = new BufferedReader(new InputStreamReader(stream));
	}

	/**
	 * Parse a problem provided via a given input stream
	 *
	 * @param stream The input stream to read from
	 * @return Internal representation of the parsed problem
	 * @throws IOException
	 */
	public Problem parse(InputStream stream) throws IOException, FileNotFoundException {
		initialize(stream);
		parseInput();
		Problem p;
		if (fullObservable) {
			p = new FullyObservableProblem(initialValuation, goal, variableNames, propositionNames, domainSizes,
					axiomLayer, defaultValues, operators, axioms);
		} else {
			p = new PartiallyObservableProblem(BDDManager, initialBDD, goal, variableNames, propositionNames,
					domainSizes, axiomLayer, defaultValues, operators, axioms, variablesWhichAreInitiallyKnown);
		}
		return p;
	}

	public boolean isFond(InputStream stream) throws IOException {
		initialize(stream);
		parseVersion();
		return fullObservable;
	}

	/**
	 * Parse goal condition.
	 *
	 * <pre>
	 * <code>
	 * goal ::= "begin_goal"  nl  [Keyword]
	 *          int           nl  [Number of conditions]
	 *          (int int nl)*     [Variable-value pair in condition]
	 *          "end_goal"        [Keyword]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseGoalCondition() throws IOException {
		assertEq(reader.readLine(), RW_BEGINGOAL);

		int goalSize = parseInt("Parse goal condition error", reader.readLine());
		Map<Integer, Integer> variableValuePairs = new HashMap<Integer, Integer>((int) (goalSize / 0.75) + 1);

		for (int i = 0; i < goalSize; i++) {
			String[] line = splitNextLine();
			assertLen(line, 2);

			int var = parseInt("Parse goal condition error", line[0], 0, numberOfVariables - 1);
			int val = parseInt("Parse goal condition error", line[1], 0, domainSizes.get(var) - 1);
			variableValuePairs.put(var, val);
		}

		assertEq(reader.readLine(), RW_ENDGOAL);

		goal = new ExplicitCondition(variableValuePairs);
	}

	/**
	 * Parse initial state section.
	 *
	 * <pre>
	 * <code>
	 * init ::= "begin_state" nl  [Keyword]
	 *
	 *          "end_state"       [Keyword]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseInitialState() throws IOException {
		variablesWhichAreInitiallyKnown = new HashSet<Integer>(((int) (numberOfVariables / 0.75)) + 1);
		assertEq(reader.readLine(), RW_BEGINSTATE);
		Integer[] defaultVals = new Integer[numberOfVariables];
		// Fill up with -1.
		for (int i = 0; i < numberOfVariables; i++) {
			defaultVals[i] = -1;
		}

		if (!fullObservable) {
			String[] line = splitNextLine();
			int numberOfFacts = parseInt("Parse initial state error (number of facts)", line[0], 0, numberOfVariables);
			int[] factVars = new int[numberOfFacts];
			int[] factVals = new int[numberOfFacts];
			for (int i = 0; i < numberOfFacts; i++) {
				line = splitNextLine();
				assertLen(line, 2);
				factVars[i] = parseInt("Parse initial state error", line[0], 0, numberOfVariables);
				factVals[i] = parseInt("Parse initial state error", line[1], 0, domainSizes.get(factVars[i]) - 1);
				if (axiomLayer.get(factVars[i]) > -1) {
					// Derived variable.
					defaultVals[factVars[i]] = factVals[i];
				}
				variablesWhichAreInitiallyKnown.add(factVars[i]);
			}
			line = splitNextLine();
			int numberOfOneOf = parseInt("Parse initial state error", line[0], 0);
			int[][] oneOfVars = new int[numberOfOneOf][];
			int[][] oneOfVals = new int[numberOfOneOf][];
			for (int i = 0; i < numberOfOneOf; i++) {
				line = splitNextLine();
				assert line.length % 2 == 0;
				oneOfVars[i] = new int[line.length / 2];
				oneOfVals[i] = new int[line.length / 2];
				// collect vars
				for (int j = 0; j < line.length; j += 2) {
					oneOfVars[i][j / 2] = parseInt("Parse initial state error", line[j], 0, numberOfVariables);
				}
				// collect vals
				for (int j = 1; j < line.length; j += 2) {
					oneOfVals[i][(j - 1) / 2] = parseInt("Parse initial state error", line[j], 0,
							domainSizes.get(oneOfVars[i][(j - 1) / 2]));
				}
			}
			line = splitNextLine();
			int numberOfFormulae = parseInt("Parse Initial state error", line[0], 0);
			String[] formulae = new String[numberOfFormulae];
			for (int i = 0; i < numberOfFormulae; i++) {
				formulae[i] = reader.readLine();
			}
			assertEq(reader.readLine(), RW_ENDSTATE);
			initialBDD = BDDManager.initializeInitialStateBDD(factVars, factVals, oneOfVars, oneOfVals, formulae);
			// Assert that every derived variable has a default value.
			for (int var = 0; var < numberOfVariables; var++) {
				if (axiomLayer.get(var) > -1) {
					assert defaultVals[var] > -1;
				}
			}
		} else {
			int[] factVals = new int[numberOfVariables];
			for (int var = 0; var < numberOfVariables; var++) {
				String[] line = splitNextLine();
				assertLen(line, 1);
				factVals[var] = parseInt("Parse initial state error", line[0], 0, domainSizes.get(var) - 1);
				if (axiomLayer.get(var) > -1) {
					// Derived variable.
					defaultVals[var] = factVals[var];
				}
				variablesWhichAreInitiallyKnown.add(var);
			}
			assertEq(reader.readLine(), RW_ENDSTATE);
			initialValuation = factVals;
		}
		defaultValues = new ArrayList<Integer>(Arrays.asList(defaultVals));
	}

	/**
	 * Parse the input.
	 *
	 * <pre>
	 * <code>
	 * input ::= (meta nl)?  [Meta data]
	 *           vars   nl  [Variables]
	 *           init   nl  [Initial state]
	 *           goal   nl  [Goal condition]
	 *           ops        [Operators]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseInput() throws IOException {
		parseVersion();
		parseMetric();
		parseVariables();
		parseMutexGroups();
		parseInitialState();
		parseGoalCondition();
		parseOperators();
		parseAxioms();

		// assert that we are at the end of the file
		// (except for possible trailing whitespace).
		String rest;
		while ((rest = reader.readLine()) != null) {
			assert rest.trim().equals("");
		}
	}

	/**
	 * Parse an integer value.
	 *
	 * @param string String to be parsed
	 * @return The parsed integer value
	 */
	private int parseInt(String errorMessage, String string) {
		int result = -1;
		try {
			result = Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.err.println(errorMessage + ": Expected an integer but read \"" + string + "\".");
			Global.ExitCode.EXIT_INPUT_ERROR.exit();
		}
		return result;
	}

	/**
	 * Parse an integer value which must have a minimum value and raise an error if
	 * it does not.
	 *
	 * @param string   String to be parsed
	 * @param minValue Lower bound on value (inclusive)
	 * @return The parsed integer value
	 */
	private int parseInt(String errorMessage, String string, int minValue) {
		int result = parseInt(errorMessage, string);
		if (result < minValue) {
			System.err.println(errorMessage + ": Expected an integer greater than or equal to " + minValue
					+ " but read " + result + ".");
			Global.ExitCode.EXIT_INPUT_ERROR.exit();
		}
		return result;
	}

	/**
	 * Parse an integer value which must fall into a given interval and raise an
	 * error if it does not.
	 *
	 * @param string   String to be parsed
	 * @param minValue Lower bound on value (inclusive)
	 * @param maxValue Upper bound on value (inclusive)
	 * @return The parsed integer value
	 */
	private int parseInt(String errorMessage, String string, int minValue, int maxValue) {
		int result = parseInt(errorMessage, string, minValue);
		if (result > maxValue) {
			System.err.println(errorMessage + ": Expected an integer less than or equal to " + maxValue + " but read "
					+ result + ".");
			Global.ExitCode.EXIT_INPUT_ERROR.exit();
		}
		return result;
	}

	/**
	 * Parse version.
	 *
	 * <pre>
	 * <code>
	 * version ::= "begin_version" nl      [Keyword]
	 *                 int               nl       [Version number]
	 *                "end_version"           [Keyword]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseVersion() throws IOException {
		assertEq(reader.readLine(), RW_BEGINVERSION);
		String version = reader.readLine();
		// We support version 3 (deterministic) and version 3 with FOND/POND adaptions.
		assert version.equals("3.FOND") || version.equals("3.POND") || version.equals("3");
		if (version.equals("3.POND")) {
			fullObservable = false;
		} else {
			fullObservable = true;
		}
		assertEq(reader.readLine(), RW_ENDVERSION);
	}

	/**
	 * Parse metric which indicated whether action costs are used or not.
	 *
	 * <pre>
	 * <code>
	 * version ::= "begin_metric" nl      [Keyword]
	 *                 int              nl      [Version number]
	 *                "end_metric"                 [Keyword]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseMetric() throws IOException {
		assertEq(reader.readLine(), RW_BEGINMETRIC);
		int metric = parseInt("Parse metric error", reader.readLine(), 0, 1);
		if (metric == 1) {
			actionCostsUsed = true;
		}
		assertEq(reader.readLine(), RW_ENDMETRIC);
	}

	/**
	 * Parse a single operator.
	 *
	 * <pre>
	 * <code>
	 * op  ::=    "begin_operator" nl     [Keyword]
	 *            string           nl     [Name]
	 *            int              nl     [Number of prevail conditions]
	 *            (int int nl)*           [Prevail condition (variable-value pair)]
	 *            int              nl     [Number of nondeterministic choices]
	 *            (nondet nl)*            [Nondeterministic effect]
	 *            "end_operator"          [Keyword]
	 * nondet ::= int              nl     [Number of effects]
	 *            (eff nl)*               [Effect]
	 * eff ::=    int int* int int int    [NumberOfConditions Conditions AffectedVar OldValue NewValue]
	 * </code>
	 * </pre>
	 *
	 * @return An internal representation of the operator.
	 * @throws IOException
	 */
	private ExplicitOperator parseOperator() throws IOException {
		assertEq(reader.readLine(), RW_BEGINOP);

		String name = reader.readLine().trim();

		int commentIdx = name.indexOf("//");
		if (commentIdx > -1) {
			name = name.substring(0, commentIdx).trim();
		}

		int numPrevails = parseInt("Parse operator error", reader.readLine(), 0);

		List<Integer> prevailVarsList = new ArrayList<Integer>();
		List<Integer> prevailValsList = new ArrayList<Integer>();

		for (int i = 0; i < numPrevails; i++) {
			String[] line = splitNextLine();
			assertLen(line, 2);

			prevailVarsList.add(i, parseInt("Parse operator error", line[0], 0, numberOfVariables - 1));
			prevailValsList.add(i,
					parseInt("Parse operator error", line[1], 0, domainSizes.get(prevailVarsList.get(i)) - 1));
		}

		int numNondetChoices = parseInt("Parse operator error (numNondetChoices)", reader.readLine(), 1);

		Set<Set<ExplicitEffect>> nondeterministicChoices = new HashSet<Set<ExplicitEffect>>(
				(int) (numNondetChoices / 0.75) + 1);

		for (int ch = 0; ch < numNondetChoices; ch++) {
			int numEffects = parseInt("Parse operator error (numEffects)", reader.readLine(), 0);

			Set<ExplicitEffect> effects = new HashSet<ExplicitEffect>((int) (numEffects / 0.75) + 1);

			for (int i = 0; i < numEffects; i++) {
				String[] line = splitNextLine();
				assertLenGeq(line, 1);

				int numEffectConditions = parseInt("Parse operator error (numEffectConditions)", line[0], 0);
				assert (numEffectConditions == 0);
				assertLen(line, 2 * numEffectConditions + 4);

				Map<Integer, Integer> variableValuePairs = new HashMap<Integer, Integer>(
						(int) (numEffectConditions / 0.75) + 1);

				for (int j = 0; j < numEffectConditions; j++) {
					int var = parseInt("Parse operator error (effect condition var)", line[2 * j + 1], 0,
							numberOfVariables - 1);
					int val = parseInt("Parse operator error (effect condition val)", line[2 * j + 2], 0,
							domainSizes.get(var) - 1);
					variableValuePairs.put(var, val);
				}

				int var = parseInt("Parse operator error", line[2 * numEffectConditions + 1], 0, numberOfVariables - 1);
				int oldVal = parseInt("Parse operator error", line[2 * numEffectConditions + 2], -1,
						domainSizes.get(var) - 1);
				int newVal = parseInt("Parse operator error", line[2 * numEffectConditions + 3], 0,
						domainSizes.get(var) - 1);

				if (oldVal != -1 && !prevailVarsList.contains(var)) {
					prevailVarsList.add(var);
					prevailValsList.add(oldVal);
				}

				effects.add(new ExplicitEffect(new ExplicitCondition(variableValuePairs), var, newVal));
			}

			nondeterministicChoices.add(effects);
		}
		assert !nondeterministicChoices.isEmpty();
		// Note: A nondeterministic effect which has only one deterministic effect, the
		// empty effect,
		// does not
		// change the current state. To be more efficient when working with BDDs, we
		// represent this
		// special effect
		// by null.
		if (nondeterministicChoices.size() == 1 && nondeterministicChoices.iterator().next().isEmpty()) {
			nondeterministicChoices = null;
		}

		Map<Integer, Integer> prevailVariableValuePairs = new HashMap<Integer, Integer>(
				(int) (prevailVarsList.size() / 0.75) + 1);
		for (int i = 0; i < prevailVarsList.size(); i++) {
			int var = prevailVarsList.get(i);
			int val = prevailValsList.get(i);
			prevailVariableValuePairs.put(var, val);
		}

		int actionCost = parseInt("Parse action cost error", reader.readLine(), 0);
		if (!actionCostsUsed && actionCost != 0) {
			System.err.println("WARNING: Action costs are not used because of the metric section of the SAS+ file.");
			System.err.println("But it seems that there are costs defined in the SAS+ file. Please check!");
		}
		if (!actionCostsUsed) {
			actionCost = 1;
		}

		Set<Pair<Integer, Integer>> observation;
		if (!fullObservable) {
			int numberOfObservations = parseInt("Parse observation error", reader.readLine(), 0);
			observation = new HashSet<Pair<Integer, Integer>>((int) (numberOfObservations / 0.75) + 1);
			if (numberOfObservations > 0) {
				for (int i = 0; i < numberOfObservations; i++) {
					String[] line = splitNextLine();
					assert line.length == 2;
					int var = parseInt("Parse observation error", line[0], 0, numberOfVariables - 1);
					int val = parseInt("Parse observation error", line[1], 0, domainSizes.get(var) - 1);
					observation.add(new Pair<Integer, Integer>(var, val));
				}
			}
		} else {
			observation = Collections.emptySet();
		}

		assertEq(reader.readLine(), RW_ENDOP);

		if (nondeterministicChoices == null && observation.isEmpty()) {
			return null;
		}
		return new ExplicitOperator(name, new ExplicitCondition(prevailVariableValuePairs), nondeterministicChoices,
				observation, false, actionCost);
	}

	/**
	 * Parse operators.
	 *
	 * <pre>
	 * <code>
	 * ops ::= int      nl  [Number of  operators]
	 *         (op nl)*     [Operator]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseOperators() throws IOException {
		int numOps = parseInt("Parse operator error", reader.readLine(), 0);
		operators = new LinkedHashSet<Operator>((int) (numOps / 0.75) + 1);
		for (int i = 0; i < numOps; i++) {
			ExplicitOperator op = parseOperator();
			if (op == null) {
				continue;
			}
			operators.add(op);
		}
	}

	private void parseAxioms() throws IOException {
		int numberOfAxioms = parseInt("Parse axioms error", reader.readLine(), 0);
		axioms = new LinkedHashSet<OperatorRule>((int) (numberOfAxioms / 0.75) + 1);
		for (int i = 0; i < numberOfAxioms; i++) {
			assertEq(reader.readLine(), RW_BEGINAXIOM);
			// body
			int numConditions = parseInt("Error while parsing number of axiom conditions.", reader.readLine(), 0);
			Set<Pair<Integer, Integer>> conditions = new HashSet<Pair<Integer, Integer>>(
					(int) (numConditions / 0.75) + 1);
			for (int j = 0; j < numConditions; j++) {
				String[] line = splitNextLine();
				assertLen(line, 2);
				int var = parseInt("Error while parsing variable of an axiom condition.", line[0], 0,
						numberOfVariables - 1);
				int val = parseInt("Error while parsing value of an axiom condition.", line[1], 0,
						domainSizes.get(var) - 1);
				conditions.add(new Pair<Integer, Integer>(var, val));
			}
			// head
			String[] line = splitNextLine();
			assertLen(line, 3);
			int var = parseInt("Error while parsing variable of an axiom head.", line[0], 0, numberOfVariables - 1);
			int newVal = parseInt("Error while parsing post value of an axiom head.", line[2], 0,
					domainSizes.get(var) - 1);
			OperatorRule axiom = new OperatorRule(conditions, new Pair<Integer, Integer>(var, newVal));
			axioms.add(axiom);
			assertEq(reader.readLine(), RW_ENDAXIOM);
		}
	}

	/**
	 * Parse the variables section.
	 *
	 * <pre>
	 * <code>
	 * vars ::= "begin_variables" nl  [Keyword]
	 *          int               nl  [Number of variable]
	 *          (var nl)*             [Single variable]
	 *          "end_variables"       [Keyword]
	 * var  ::= string int int        [Name DomainSize AxiomLayer]
	 * </code>
	 * </pre>
	 *
	 * @throws IOException
	 */
	private void parseVariables() throws IOException {
		numberOfVariables = parseInt("Parse variables error", reader.readLine(), 0);
		variableNames = new ArrayList<String>(numberOfVariables);
		domainSizes = new ArrayList<Integer>(numberOfVariables);
		axiomLayer = new ArrayList<Integer>(numberOfVariables);
		propositionNames = new ArrayList<List<String>>(numberOfVariables * 4);
		for (int var = 0; var < numberOfVariables; var++) {
			assertEq(reader.readLine(), RW_BEGINVAR);
			variableNames.add(var, reader.readLine());
			axiomLayer.add(var, parseInt("Parse variables error", reader.readLine(), -1));
			domainSizes.add(var, parseInt("Parse variables error", reader.readLine(), 1));
			List<String> names = new ArrayList<String>(domainSizes.get(var));
			for (int val = 0; val < domainSizes.get(var); val++) {
				String name = processPropositionName(reader.readLine().trim(), names);
				names.add(name);
			}
			propositionNames.add(names);
			assertEq(reader.readLine(), RW_ENDVAR);
		}
		if (!fullObservable) {
			if (BDDManager == null) {
				BDDManager = new BDDManager();
			}
			BDDManager.initialize(numberOfVariables, domainSizes);
		}
	}

	private void parseMutexGroups() throws IOException {
		int numMutexGroups = parseInt("Parse mutex gropus error", reader.readLine(), 0);
		for (int i = 0; i < numMutexGroups; i++) {
			assertEq(reader.readLine(), RW_BEGINMUTEXGROUP);
			int numMutexes = parseInt("Parse mutex gropus error", reader.readLine());
			for (int j = 0; j < numMutexes; j++) {
				reader.readLine();
			}
			assertEq(reader.readLine(), RW_ENDMUTEXGROUP);
		}
	}

	/**
	 * Transform proposition back to list prefix syntax.
	 *
	 * @param token Proposition to be rewritten.
	 * @return Proposition in prefix notation.
	 */

	private String processPropositionName(String token, List<String> propositionsSoFar) {
		if (token.equals("<none of those>")) {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < propositionsSoFar.size(); i++) {
				buffer.append("(not (");
				buffer.append(propositionsSoFar.get(i));
				buffer.append("))");
				if (i < propositionsSoFar.size() - 1) {
					buffer.append(" ");
				}
			}
			return buffer.toString();
		}
		assert token.startsWith("Atom ") || token.startsWith("NegatedAtom");
		if (token.endsWith("()")) {
			// zero parameters
			if (token.startsWith("Atom ")) {
				token = "(" + token.substring(5).replace("(", "").replace(",", "");
			} else {
				token = "(not (" + token.substring(12).replace("(", "").replace(",", "") + ")";
			}
		} else {
			// at least one parameter
			if (token.startsWith("Atom ")) {
				token = "(" + token.substring(5).replace("(", " ").replace(",", "");
			} else {
				token = "(not (" + token.substring(12).replace("(", " ").replace(",", "") + ")";
			}
		}
		return token;
	}

	/**
	 * Split a string into tokens separated by whitespace.
	 *
	 * @param line String to be tokenized.
	 * @return Array of tokens
	 */
	private String[] split(String line) {
		return line.split("\\s+");
	}

	/**
	 * Read the next line and split it into tokens separated by whitespace.
	 *
	 * @return Array of tokens
	 * @throws IOException
	 */
	private String[] splitNextLine() throws IOException {
		return split(reader.readLine());
	}

	// /**
	// * Get the set of variables which are uncertain in the initial state or which
	// * can become uncertain because of nondeterministic effects.
	// *
	// * @return set of variables which can be uncertain
	// */
	// public Set<Integer> getVariablesWhichCouldBecomeUncertain() {
	// Set<Integer> vars = new HashSet<Integer>(numStateVars);
	//
	// }
}
