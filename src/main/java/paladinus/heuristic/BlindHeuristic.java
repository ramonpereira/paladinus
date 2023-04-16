package paladinus.heuristic;

import paladinus.problem.Problem;
import paladinus.state.Condition;
import paladinus.state.State;

/**
 *
 * @author Robert Mattmueller
 *
 */
public class BlindHeuristic extends Heuristic {

	private Condition goal;

	public BlindHeuristic(Problem problem) {
		this(problem, problem.getGoal());
	}

	public BlindHeuristic(Problem problem, Condition goal) {
		super(problem, true); // Zero heuristic supports axioms.
		this.goal = goal;
	}

	/**
	 * Get heuristic value for given state.
	 *
	 * @param state state to be evaluated by heuristic
	 * @return heuristic value for given state
	 */
	@Override
	public double getHeuristic(State state) {
		if (goal.isSatisfiedIn(state)) {
			return 0.0;
		}
		return 1.0;
	}
}
