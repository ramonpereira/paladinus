package paladinus.heuristic.pdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javabdd.BDD;
import paladinus.explicit.ExplicitAxiomEvaluator;
import paladinus.explicit.ExplicitCondition;
import paladinus.explicit.ExplicitOperator.OperatorRule;
import paladinus.problem.Problem;
import paladinus.state.Condition;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.symbolic.BDDManager;
import paladinus.symbolic.PartiallyObservableProblem;
import paladinus.symbolic.SymbolicAxiomEvaluator;

/**
 *
 * @author Manuela Ortlieb
 *
 */
public class Abstraction {

	/**
	 * The original problem of this abstraction
	 */
	public final Problem problem;

	/**
	 * The BDDManager used by this abstraction.
	 */
	public final BDDManager BDDManager;

	/**
	 * Pattern which induced this abstraction.
	 */
	public final Set<Integer> pattern;

	/**
	 * Abstracted explicit state(s).
	 */
	private final List<State> initialState;

	/**
	 * Abstracted goal.
	 */
	public final Condition goal;

	/**
	 * Abstracted explicit goal.
	 */
	public final ExplicitCondition explicitGoal;

	/**
	 * Abstracted operators.
	 */
	public final Set<Operator> operators;

	/**
	 * Symbolic pattern complement.
	 */
	public final BDD symbolicPatternComplement;

	/**
	 * the BDD VarSet for the pattern variables
	 */
	public final BDD patternVarSet;

	/**
	 * Explicit axiom evaluator.
	 */
	private ExplicitAxiomEvaluator explicitAxiomEvaluator;

	/**
	 * Symbolic axiom evaluator.
	 */
	private SymbolicAxiomEvaluator symbolicAxiomEvaluator;

	/**
	 * Abstracted axioms.
	 */
	public final Set<OperatorRule> axioms;

	public Abstraction(Problem problem, SortedSet<Integer> pattern, Condition goal, ExplicitCondition explicitGoal,
			Set<Operator> operators, Set<OperatorRule> axioms) {
		this(problem, pattern, goal, explicitGoal, operators, axioms, null);
	}

	public Abstraction(Problem problem, SortedSet<Integer> pattern, Condition goal, ExplicitCondition explicitGoal,
			Set<Operator> operators, Set<OperatorRule> axioms, BDD symbolicPatternComplement) {
		this.pattern = Collections.unmodifiableSortedSet(pattern);
		this.problem = problem;
		if (problem instanceof PartiallyObservableProblem) {
			BDDManager = ((PartiallyObservableProblem) problem).BDDManager;
			patternVarSet = BDDManager.varSetBDD(this.pattern);
		} else {
			BDDManager = null;
			patternVarSet = null;
		}
		initialState = new ArrayList<State>();
		this.goal = goal;
		this.explicitGoal = explicitGoal;
		this.operators = Collections.unmodifiableSet(operators);
		this.axioms = Collections.unmodifiableSet(axioms);
		this.symbolicPatternComplement = symbolicPatternComplement;
	}

	private void initializeExplicitAxiomEvaluator() {
		assert explicitAxiomEvaluator == null;
		explicitAxiomEvaluator = new ExplicitAxiomEvaluator(problem, axioms);
	}

	private void initializeSymbolicAxiomEvaluator() {
		assert symbolicAxiomEvaluator == null;
		assert BDDManager != null;
		symbolicAxiomEvaluator = new SymbolicAxiomEvaluator(BDDManager, axioms);
	}

	public void setInitialState(List<State> initialState) {
		assert this.initialState.isEmpty();
		for (State state : initialState) {
			this.initialState.add(state);
		}
	}

	public List<State> getInitialState() {
		assert initialState != null;
		return Collections.unmodifiableList(initialState);
	}

	/**
	 * Abstract a given mapping to a given pattern.
	 *
	 * @param variableValueMap mapping from variables to values
	 * @param pattern          set of variables
	 * @return abstracted mapping
	 */
	public static Map<Integer, Integer> abstractVariableValueMap(Map<Integer, Integer> variableValueMap,
			Set<Integer> pattern) {
		Map<Integer, Integer> abstractedVariableValueMap = new LinkedHashMap<Integer, Integer>(variableValueMap.size());
		for (int var : variableValueMap.keySet()) {
			if (pattern.contains(var)) {
				abstractedVariableValueMap.put(var, variableValueMap.get(var));
			}
		}
		return abstractedVariableValueMap;
	}

	/**
	 * Get complement of given pattern.
	 *
	 * @param pattern set of state variables
	 * @return complement set of state variables
	 */
	public static Set<Integer> getPatternComplement(Problem problem, Set<Integer> pattern) {
		Set<Integer> patternComplement = new HashSet<Integer>((int) (problem.numStateVars / 0.75));
		for (int var = 0; var < problem.numStateVars; var++) {
			if (!pattern.contains(var)) {
				patternComplement.add(var);
			}
		}
		return patternComplement;
	}

	public SymbolicAxiomEvaluator getSymbolicAxiomEvaluator() {
		if (symbolicAxiomEvaluator == null) {
			initializeSymbolicAxiomEvaluator();
		}
		return symbolicAxiomEvaluator;
	}

	public ExplicitAxiomEvaluator getExplicitAxiomEvaluator() {
		if (explicitAxiomEvaluator == null) {
			initializeExplicitAxiomEvaluator();
		}
		return explicitAxiomEvaluator;
	}

	/**
	 * Dump this abstraction. Only for debugging.
	 */
	public void dump() {
		System.out.println("Dumping abstraction induced by pattern:");
		System.out.println(pattern);
		System.out.println("Abstracted initial state:");
		for (State state : initialState) {
			state.dump();
		}
		System.out.println("Abstracted goal: ");
		// goal.dump(); // FIXME
		System.out.println(goal);
		System.out.println("Abstracted operators (" + operators.size() + "):");
		for (Operator op : operators) {
			op.dump();
		}
	}
}
