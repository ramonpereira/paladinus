package paladinus.heuristic.pdb;

import java.util.Set;

import paladinus.problem.Problem;
import paladinus.state.State;

/**
 * @author Manuela Ortlieb
 *
 */
public abstract class PDB {
	/**
	 * The problem this PDB is computed
	 */
	Problem problem;

	/**
	 * Number of abstract states when using a given pattern.
	 *
	 * @param problem
	 * @param pattern The pattern to be used
	 * @return The number of abstract states with respect to the given pattern
	 */
	public static int numAbstractStates(Problem problem, Set<Integer> pattern) {
		int numAbstractStates = 1;
		for (int element : pattern) {
			numAbstractStates *= problem.domainSizes.get(element);
		}
		return numAbstractStates;
	}

	/**
	 * Look up heuristic value in pattern database.
	 */
	public abstract double getHeuristic(State state);

	public abstract double averageHeuristicValue();

	/**
	 * Do not print any outputs on System.out or System.err, except DEBUG is true.
	 */
	public static boolean noOutputs = false;

	/**
	 * For POND: use explicit PDBs under assumption of full observability instead of
	 * belief state PDBs (under partial observability).
	 */
	public static boolean buildExplicitPDBs = false;

	public PDB(Problem problem) {
		this.problem = problem;
	}
}
