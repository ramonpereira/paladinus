package main.java.paladinus.problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import main.java.paladinus.Global;
import main.java.paladinus.Preprocessor;
import main.java.paladinus.explicit.ExplicitAxiomEvaluator;
import main.java.paladinus.explicit.ExplicitCondition;
import main.java.paladinus.explicit.ExplicitOperator;
import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.heuristic.pdb.Abstraction;
import main.java.paladinus.state.Condition;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;
import main.java.paladinus.util.Pair;

/**
 * A fully observable problem consists of a set of multi-valued state variables,
 * an initial state, a goal condition, and a set of operators. There is no
 * uncertainty compared to partially observable planning problems. Therefore no
 * sensing operators are needed.
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class FullyObservableProblem extends Problem {

	/**
	 * Explicit initial state.
	 */
	private ExplicitState initialState;

	/**
	 * Explicit axiom evaluator.
	 */
	public final ExplicitAxiomEvaluator explicitAxiomEvaluator;

	/**
	 * Set DEBUG to true for more console outputs.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Constructor to create a fully observable planning problem.
	 */
	public FullyObservableProblem(int[] initialValuation, ExplicitCondition goal, ArrayList<String> variableNames,
			List<List<String>> propositionNames, ArrayList<Integer> domainSizes, ArrayList<Integer> axiomLayer,
			ArrayList<Integer> defaultAxiomValues, LinkedHashSet<Operator> causativeOperators,
			Set<OperatorRule> axioms) {
		super(goal, variableNames, propositionNames, domainSizes, axiomLayer, defaultAxiomValues, causativeOperators,
				axioms, true);
		performSanityCheck();
		explicitAxiomEvaluator = new ExplicitAxiomEvaluator(this);
		initialState = new ExplicitState(this, initialValuation, explicitAxiomEvaluator);

		Set<Operator> nonApplicableOperators = new HashSet<>();
		for (Operator op : this.operators)
			if (!Operator.applicableOperator(op))
				nonApplicableOperators.add(op);
		for (Operator nOp : nonApplicableOperators)
			this.operators.remove(nOp);
	}

	/**
	 * Sanity check: Make sure that all variables have domain sizes of at least one.
	 */
	private void checkDomainSizes() {
		for (int var = 0; var < domainSizes.size(); var++) {
			if (domainSizes.get(var) < 1) {
				System.err.println("Variable " + var + " has a domain size of " + domainSizes.get(var) + ".");
				Global.ExitCode.EXIT_INPUT_ERROR.exit();
			}
		}
	}

	/**
	 * Sanity check: Test for equal state sizes
	 */
	private void checkStateSizes() {
		if (variableNames.size() != domainSizes.size()) {
			if (DEBUG) {
				System.out.println("Variable names length = " + variableNames.size());
				System.out.println("Domain size length = " + domainSizes.size());
			}
			System.err.println("Numbers of state variables in inputs differ.");
			Global.ExitCode.EXIT_INPUT_ERROR.exit();
		}

	}

	/**
	 * Perform a sanity check on the inputs.
	 */
	private void performSanityCheck() {
		if (variableNames != null && domainSizes != null && propositionNames != null) {
			checkStateSizes();
			checkDomainSizes();
		}
	}

	/**
	 * Get explicit initial state. Special case: there is only one explicit initial
	 * state in a fully observable planning problem.
	 *
	 * @return set which contains the single initial state of this planning task
	 */
	@Override
	public List<ExplicitState> getExplicitInitialStates() {
		if (explicitInitialStateList == null) {
			explicitInitialStateList = Collections
					.unmodifiableList(new ArrayList<ExplicitState>(Arrays.asList(initialState)));
		}
		return explicitInitialStateList;
	}

	@Override
	public Problem newProblemFromAbstraction(Abstraction abstraction) {
		// FIXME The variable list should actually be adjusted at some point maybe?

		// compute new shortened info lists:
		int newVariableCount = numStateVars;
		ArrayList<String> newVariableNames = new ArrayList<String>(variableNames);
		ArrayList<Integer> newDomainSizes = new ArrayList<Integer>(domainSizes);
		List<List<String>> newPropositionNames = propositionNames;
		ArrayList<Integer> newAxiomLayer = new ArrayList<Integer>(axiomLayer);
		ArrayList<Integer> newDefaultAxiomValues = new ArrayList<Integer>(defaultAxiomValues);

		// get new initial state:
		State initialState = abstraction.getInitialState().iterator().next();
		assert (initialState instanceof ExplicitState);
		initialState = getSingleInitialState();
		initialState = ((ExplicitState) initialState).abstractToPattern(abstraction);
		Map<Integer, Integer> initialAssignmentMap = ((ExplicitState) initialState).variableValueAssignment;

		// get new initial assignment array:
		int[] newInitialValuation = new int[abstraction.pattern.size()];
		int newIndex = 0;
		for (int var = 0; var < newVariableCount; var++) {
			if (abstraction.pattern.contains(var)) {
				newInitialValuation[newIndex] = initialAssignmentMap.get(var);
				newIndex++;
			}
		}

		// get new explicitGoal:
		ExplicitCondition newGoal = abstraction.explicitGoal;
		assert (newGoal != null);

		// Get new abstracted explicit ops.
		LinkedHashSet<Operator> newExplicitOps = new LinkedHashSet<Operator>();
		for (Operator normalOp : operators) {
			ExplicitOperator op = (ExplicitOperator) normalOp;
			// System.out.println("op before abstraction: " + op.toString());
			Operator newOp = op.abstractToPattern(abstraction.pattern);
			if (newOp != null) { // only add if still there after abstracting (non-empty)
				newExplicitOps.add(newOp);
			}
		}
		assert (newExplicitOps != null);

		// instantiate new problem instance:
		FullyObservableProblem newProblem = new FullyObservableProblem(newInitialValuation, newGoal, newVariableNames,
				newPropositionNames, newDomainSizes, newAxiomLayer, newDefaultAxiomValues, newExplicitOps,
				abstraction.axioms);

		// public FullyObservableProblem(int[] initialValuation, ExplicitCondition goal,
		// ArrayList<String> variableNames, List<List<String>> propositionNames,
		// ArrayList<Integer>
		// domainSizes, ArrayList<Integer> axiomLayer, ArrayList<Integer>
		// defaultAxiomValues,
		// LinkedHashSet<Operator> causativeOperators, Set<OperatorRule> axioms) {

		initialState.setProblem(newProblem);
		newProblem.setInitialState(initialState);

		// done!
		return newProblem;
	}

	/**
	 * Abstract fully observable problem to given pattern.
	 *
	 * @param pattern the set of state variables to which this problem is abstracted
	 * @return abstraction induced by given pattern
	 */
	@Override
	public Abstraction abstractToPattern(Set<Integer> pattern, Condition goal) {
		assert (initialState != null);

		// Abstract goal condition.
		ExplicitCondition abstractGoal = explicitGoal.abstractToPattern(pattern);

		// Abstract operators.
		Set<Operator> abstractedOperators = new LinkedHashSet<Operator>(getOperators().size());
		for (Operator op : getOperators()) {
			Operator absOp = op.abstractToPattern(pattern);
			if (absOp != null) {
				assert absOp.isAbstracted;
				abstractedOperators.add(absOp);
			}
		}
		assert Operator.assertNoDuplicateInNames(abstractedOperators);
		if (DEBUG) {
			System.out.println("Abstracted ops:");
			for (Operator op : abstractedOperators) {
				((ExplicitOperator) op).dump();
			}
		}

		// An axiom is either completely conserved or completely removed in an
		// abstraction.
		Set<OperatorRule> abstractedAxioms = new LinkedHashSet<ExplicitOperator.OperatorRule>(axioms.size());
		for (OperatorRule axiom : axioms) {
			if (pattern.contains(axiom.head.first)) {
				for (Pair<Integer, Integer> fact : axiom.body) {
					assert pattern.contains(fact.first);
				}
				abstractedAxioms.add(axiom);
			}
		}

		// Abstract and set initial state.
		SortedSet<Integer> sortedPattern = new TreeSet<Integer>(pattern);
		Abstraction abstraction = new Abstraction(this, sortedPattern, abstractGoal, abstractGoal, abstractedOperators,
				abstractedAxioms);
		abstraction.setInitialState(Arrays.asList(initialState.abstractToPattern(abstraction)));
		return abstraction;
	}

	/**
	 * Get string representation of this fully observable problem.
	 *
	 * @return string representation of this fully observable planning problem
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Init:    " + initialState.toString() + "\n");
		buffer.append("Goal:    " + explicitGoal.toString() + "\n");
		buffer.append("Vars:    ");
		for (int var = 0; var < variableNames.size(); var++) {
			buffer.append(var + ":" + domainSizes.get(var) + " ");
		}
		buffer.append("\n");
		buffer.append("Num ops: " + getOperators().size() + "\n");
		return buffer.toString();
	}

	/**
	 * Return the single initial state of this planning task.
	 *
	 * @return the initial state
	 */
	@Override
	public State getSingleInitialState() {
		assert (initialState != null);
		return initialState;
	}

	/**
	 * Preprocess operators.
	 */
	@Override
	public void finishInitializationAndPreprocessing() {
		// Store operators as original operators before their preprocessing.
		setOriginalOperators(this.operators);
		Set<Operator> ops;
		if(Global.options.getPolicyType() != null && Global.options.getPolicyType().equals("STRONG")) {
			ops = Preprocessor.preprocessForStrongPlanning(getOperators());
		} else if(Global.options.getPolicyType() != null && Global.options.getPolicyType().equals("STRONG_CYCLIC")) {
			ops = Preprocessor.preprocessForStrongCyclicPlanning(getOperators());
		} else {
			ops = getOperators();
		}
		setModifiedOperators(ops);
		this.operators = ops;
	}

	/**
	 * Get explicit goal of this planning task.
	 *
	 * @return goal
	 */
	@Override
	public Condition getGoal() {
		return this.explicitGoal;
	}

	@Override
	public ExplicitAxiomEvaluator getExplicitAxiomEvaluator() {
		return this.explicitAxiomEvaluator;
	}

	@Override
	public void setInitialState(State initState) {
		this.initialState = (ExplicitState) initState;
	}

	@Override
	protected void setOriginalOperators(Set<Operator> ops) {
		// FIXME Duplicate code (since of op.copy()).
		// See FullyObservable Problem. Constructor not possible since symbolic operators are build with BDDManager.
		Map<String, Operator> original = new HashMap<String, Operator>((int) (operators.size() / 0.75) + 1);
		int index = 0; // Each operator should get a unique name. Append a index if it has a duplicate name.
		for (Operator op : operators) {
			if (original.containsKey(op.getName())) {
				original.put(op.getName() + index, op.copy());
				op.setName(op.getName() + index);
				++index;
			} else {
				original.put(op.getName(), op.copy());
			}
		}
		originalOperators = Collections.unmodifiableMap(original);
	}

	@Override
	protected void setModifiedOperators(Set<Operator> ops) {
		Map<String, Operator> modifiedOperators = new HashMap<String, Operator>();
		int index = 0;
		for (Operator op : operators) {
			if (modifiedOperators.containsKey(op.getName())) {
				modifiedOperators.put(op.getName() + index, op.copy());
				op.setName(op.getName() + index);
				++index;
			} else {
				modifiedOperators.put(op.getName(), op.copy());
			}
		}
		this.modifiedOperators = Collections.unmodifiableMap(modifiedOperators);
	}
}
