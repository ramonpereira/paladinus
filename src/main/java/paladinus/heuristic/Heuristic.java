package main.java.paladinus.heuristic;

import main.java.paladinus.Global;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.State;
import main.java.paladinus.util.FactPartitioner;

/**
 *
 * @author Robert Mattmueller
 *
 */
public abstract class Heuristic {
	/**
	 * The problem this heuristic operates on
	 */
	protected Problem problem;

	/**
	 * How to accumulate heuristic values of sample world states in case of partial
	 * observability.
	 */
	public enum HeuristicStrategy {
		MAX, ADD, AVERAGE
	};

	/**
	 * Indicates an infinite heuristic value.
	 */
	public static final double INFINITE_HEURISTIC = Double.POSITIVE_INFINITY;

	/**
	 * Get heuristic value for given state.
	 *
	 * @param state state to be evaluated by heuristic
	 * @return heuristic value for given state
	 */
	public abstract double getHeuristic(State state);
	FactPartitioner partitioner;

	public Heuristic(Problem problem, boolean supportsAxioms) {
		this.problem = problem;
		this.partitioner = FactPartitioner.getInstance(problem);
		if (!supportsAxioms) {
			if (problem.numAxioms != 0) {
				System.err.println("Heuristic does not support axioms. Planner will be terminated.");
				Global.ExitCode.EXIT_UNSUPPORTED.exit();
			}
		}
	}
}
