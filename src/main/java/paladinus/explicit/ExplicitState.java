package main.java.paladinus.explicit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.paladinus.heuristic.pdb.Abstraction;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Condition;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;
import main.java.paladinus.util.Pair;

/**
 * A state is a function mapping state variables to values from their respective
 * domains. This explicit state is a helper class and is not used in the search.
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class ExplicitState extends State {

	/**
	 * Variable value mapping describing this state.
	 */
	public final Map<Integer, Integer> variableValueAssignment;

	/**
	 * Number of variables describing this state.
	 */
	public final int size;

	private Set<Pair<Integer, Integer>> varsPropositions = new HashSet<>();

	/**
	 * Axiom evaluator.
	 */
	public final ExplicitAxiomEvaluator axiomEvaluator;

	/**
	 * Debug mode for additional outputs.
	 */
	public static final boolean DEBUG = false;
	
	private double heuristic;

	/**
	 * Creates a new state from a given variable assignment.
	 *
	 * @param problem the problem this state originates from
	 * @param values  variable assignment
	 */
	public ExplicitState(Problem problem, int[] values, ExplicitAxiomEvaluator axiomEvaluator) {
		this(problem, axiomEvaluator.evaluate(valuesToVariableValueMap(values)), false, null, axiomEvaluator);
	}

	/**
	 * Creates a new state from given assignment.
	 *
	 * @param problem                 the problem this state originates from
	 * @param variableValueAssignment
	 */
	public ExplicitState(Problem problem, Map<Integer, Integer> variableValueAssignment,
			ExplicitAxiomEvaluator axiomEvaluator) {
		this(problem, axiomEvaluator.evaluate(variableValueAssignment), false, null, axiomEvaluator);
		assert variableValueAssignment.size() == problem.numStateVars;
	}

	/**
	 * Creates a new abstracted state from given assignment and abstraction.
	 *
	 * @param problem                 the problem this state originates from
	 * @param variableValueAssignment assignment of variables to values
	 * @param isAbstractedState       denote if the state is abstracted by a pattern
	 * @param abstractedProblem       denotes the abstracted problem of an
	 *                                abstracted state
	 */
	public ExplicitState(Problem problem, Map<Integer, Integer> variableValueAssignment, Abstraction abstraction,
			ExplicitAxiomEvaluator axiomEvaluator) {
		this(problem, axiomEvaluator.evaluate(variableValueAssignment), true, abstraction, axiomEvaluator);
	}

	private ExplicitState(Problem problem, Map<Integer, Integer> variableValueAssignment, boolean isAbstractedState,
			Abstraction abstraction, ExplicitAxiomEvaluator axiomEvaluator) {
		super(problem, computeUniqueID(problem, variableValueAssignment), isAbstractedState, abstraction);
		assert assertVariableOrder(variableValueAssignment);
		this.variableValueAssignment = variableValueAssignment;

		for (Integer i : variableValueAssignment.keySet()) {
			int j = variableValueAssignment.get(i);
			this.varsPropositions.add(new Pair<Integer, Integer>(i, j));
		}

		size = variableValueAssignment.size();
		if (isAbstractedState) {
			assert (size <= problem.numStateVars);
		} else {
			assert (size == problem.numStateVars);
		}
		this.axiomEvaluator = axiomEvaluator;

		// It is important to assert that there is no overflow for abstract states.
		assert (!isAbstractedState || (uniqueID.intValue() == hashCode));
	}

	/**
	 * Test if a given operator is applicable in this state. Operators without
	 * effects are omitted (i.e. not applicable), because they are not useful under
	 * full observability.
	 *
	 * @param operator
	 * @return true iff given operator is applicable in this state and it is
	 *         causative.
	 */
	@Override
	public boolean isApplicable(Operator operator) {
		return operator.isCausative && operator.isEnabledIn(this);
	}

	/**
	 * Apply given operator to this state.
	 *
	 * @param op operator to apply
	 * @return set of successor states reached by applying given operator
	 */
	@Override
	public Set<State> apply(Operator op) {
		assert (op.isCausative);
		ExplicitOperator explicitOp = (ExplicitOperator) op;
		Set<State> states = new HashSet<State>();
		for (Set<ExplicitEffect> choice : explicitOp.getNondeterministicEffect()) {
			ExplicitState newState = progress(choice);
			states.add(newState);
		}
		return states;
	}

	/**
	 * Apply a deterministic effect to this state.
	 *
	 * @param effect effect to apply in this state
	 * @return successor state obtained by applying the given effect to this state.
	 */
	public ExplicitState progress(Set<ExplicitEffect> effect) {
		if (DEBUG) {
			System.out.println("Current state: " + toString());
		}
		// make a copy
		Map<Integer, Integer> succVariableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(size / 0.75));
		succVariableValuePairs.putAll(variableValueAssignment);

		// apply effects
		for (ExplicitEffect eff : effect) {
			if (eff.isEnabledIn(variableValueAssignment)) {
				assert (succVariableValuePairs.containsKey(eff.variable));
				succVariableValuePairs.put(eff.variable, eff.value);
			} else {
				assert false; // effect conditions are not supported.
			}
		}
		if (isAbstractedState) {
			assert (axiomEvaluator != null);
			return new ExplicitState(problem, succVariableValuePairs, abstraction, axiomEvaluator);
		} else {
			assert (axiomEvaluator != null);
			return new ExplicitState(problem, succVariableValuePairs, axiomEvaluator);
		}

	}

	/**
	 * Create a variable value assignment from given values.
	 *
	 * @param values
	 * @return variable value assignment
	 */
	private static Map<Integer, Integer> valuesToVariableValueMap(int[] values) {
		// Note: LinkedHashMap has a fix iteration order which is important for unique
		// hash values.
		Map<Integer, Integer> variableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(values.length / 0.75));
		for (int var = 0; var < values.length; var++) {
			variableValuePairs.put(var, values[var]);
		}
		return variableValuePairs;
	}

	/**
	 * Compute unique BigInteger value for this state.
	 */
	private static BigInteger computeUniqueID(Problem problem, Map<Integer, Integer> variableValueMap) {
		BigInteger uniqueID = BigInteger.ZERO;
		for (int var = 0; var < problem.numStateVars; var++) {
			if (variableValueMap.containsKey(var)) {
				uniqueID = uniqueID.multiply(BigInteger.valueOf(problem.domainSizes.get(var)));
				uniqueID = uniqueID.add(BigInteger.valueOf(variableValueMap.get(var)));
			}
		}
		return uniqueID;
	}

	/**
	 * Abstract this state to the pattern of the given abstraction.
	 *
	 * @param abstraction
	 */
	public State abstractToPattern(Abstraction abstraction) {
		return new ExplicitState(problem,
				Abstraction.abstractVariableValueMap(variableValueAssignment, abstraction.pattern), abstraction,
				abstraction.getExplicitAxiomEvaluator());
	}

	/**
	 * Get all explicit world states of this explicit state which is exactly one,
	 * namely itself.
	 *
	 * @return list containing this explicit state
	 */
	@Override
	public List<ExplicitState> getAllExplicitWorldStates() {
		return new ArrayList<ExplicitState>(Arrays.asList(this));
	}

	/**
	 * Asserts that variables are sorted in ascending order.
	 *
	 * @param variableValueAssignment
	 * @return true iff variables are sorted in ascending order
	 */
	public static boolean assertVariableOrder(Map<Integer, Integer> variableValueAssignment) {
		return assertVariableOrder(variableValueAssignment.keySet());
	}

	/**
	 * Asserts that variables are sorted in ascending order.
	 *
	 * @param variables set of variables
	 * @return true iff variables are sorted in ascending order
	 */
	public static boolean assertVariableOrder(Set<Integer> variables) {
		int oldVar = -1;
		for (int var : variables) {
			if (var <= oldVar) {
				return false;
			}
			oldVar = var;
		}
		return true;
	}

	/**
	 * String representation of this state with proposition names.
	 *
	 * @return string representation of this state
	 */
	@Override
	public String toStringWithPropositionNames() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		int i = 0;
		for (int var : variableValueAssignment.keySet()) {
			buffer.append(problem.propositionNames.get(var).get(variableValueAssignment.get(var)));
			if (i < variableValueAssignment.size() - 1) {
				buffer.append(", ");
			}
			i++;
		}
		buffer.append(" }");
		return buffer.toString();
	}
	
	/**
	 * String representation of this state with proposition names.
	 *
	 * @return string representation of this state
	 */
	@Override
	public String toStringPropositionNames() {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		for (int var : variableValueAssignment.keySet()) {
			buffer.append(problem.propositionNames.get(var).get(variableValueAssignment.get(var)));
			if (i < variableValueAssignment.size() - 1) {
				buffer.append(", ");
			}
			i++;
		}
		return buffer.toString();
	}

	/**
	 * String representation of this state.
	 *
	 * @return string representation of this state
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		int count = 0;
		for (int var : variableValueAssignment.keySet()) {
			buffer.append("var" + var + ":" + variableValueAssignment.get(var) + (count >= variableValueAssignment.keySet().size()-1 ? "" : " "));
			count++;
		}
		return buffer.toString();
	}

	/**
	 * Dump this explicit state.
	 */
	@Override
	public void dump() {
		System.out.println(toStringWithPropositionNames());
	}

	/**
	 * Return this state's variable value assignment as condition.
	 *
	 * @return condition representing this state's variable value assignment
	 */
	@Override
	public Condition toCondition() {
		return new ExplicitCondition(variableValueAssignment);
	}

	public Set<Pair<Integer, Integer>> getVarsPropositions() {
		return varsPropositions;
	}
	
	public void setHeuristic(double heuristic) {
		this.heuristic = heuristic;
	}
	
	public double getHeuristic() {
		return heuristic;
	}
}
