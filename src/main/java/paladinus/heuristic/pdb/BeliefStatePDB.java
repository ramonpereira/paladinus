package paladinus.heuristic.pdb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import paladinus.heuristic.Heuristic;
import paladinus.heuristic.graph.Node;
import paladinus.problem.Problem;
import paladinus.state.State;
import paladinus.symbolic.BeliefState;
import paladinus.symbolic.PartiallyObservableProblem;
import paladinus.symbolic.SymbolicCondition;

/**
 * Pattern database.
 *
 * @author Manuela Ortlieb
 */
public class BeliefStatePDB extends PDB {

	/**
	 * Abstraction which is induced by given pattern.
	 */
	private final Abstraction abstraction;

	/**
	 * Mapping from abstract belief states to heuristic values.
	 */
	private Map<Integer, Double> patternDatabase;

	/**
	 * Set true for debug output information.
	 */
	public final static boolean DEBUG = false;

	/**
	 * Constructor.
	 *
	 * @param problem The problem to compute the database for
	 * @param pattern The variables contained in the pattern.
	 */
	public BeliefStatePDB(Problem problem, Set<Integer> pattern, SymbolicCondition goal) {
		super(problem);
		long start = System.currentTimeMillis();
		assert !pattern.isEmpty();
		abstraction = ((PartiallyObservableProblem) problem).abstractToPattern(pattern, goal);
		if (DEBUG) {
			System.out.println("Computed abstraction for pattern " + pattern);
			abstraction.dump();
		}
		patternDatabase = new HashMap<Integer, Double>((int) Math.ceil(numAbstractStates(problem, pattern) / 0.75));
		fillPDB();
		if (!noOutputs || DEBUG) {
			System.out.print("Created new Belief State PDB for variables ");
			System.out.println(pattern);
			System.out.println("in " + (System.currentTimeMillis() - start) / 1000 + "s");
		}
		if (DEBUG) {
			System.out.println("pdb = " + patternDatabase);
		}
	}

	/**
	 * Actual computation and storage of abstract cost values
	 */
	private void fillPDB() {
		AbstractCostComputation comp = new AbstractCostComputation(problem, abstraction);
		Collection<Node> nodes = comp.run();
		for (Node node : nodes) {
			if (node.getCostEstimate() != Node.UNINITIALIZED_COST_ESTIMATE) {
				patternDatabase.put(node.index, node.getCostEstimate());
			}
		}
	}

	/**
	 * Look up heuristic value in pattern database.
	 *
	 * @param state concrete state to evaluate
	 * @return heuristic value of the given state
	 */
	@Override
	public double getHeuristic(State state) {
		BeliefState abstractedBeliefState = ((BeliefState) state).abstractToPattern(abstraction);
		if (patternDatabase.containsKey(abstractedBeliefState.hashCode())) {
			return patternDatabase.get(abstractedBeliefState.hashCode());
		} else {
			return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Get a string representation of this pattern database.
	 *
	 * @return string representation of this pdb
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(patternDatabase.toString());
		return buffer.toString();
	}

	/**
	 * Get average heuristic value of this pattern database. Infinity values are
	 * ignored.
	 *
	 * @return average heuristic value
	 */
	@Override
	public double averageHeuristicValue() {
		double average = 0;
		for (double h : patternDatabase.values()) {
			assert (h != Heuristic.INFINITE_HEURISTIC); // for Blocksworld
			average += h;
		}
		return (average / patternDatabase.size());
	}
}
