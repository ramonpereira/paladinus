package main.java.paladinus.heuristic;

import main.java.paladinus.heuristic.FFHeuristic.RPGStrategy;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Condition;
import main.java.paladinus.state.State;

/**
 *
 * @author Ramon Fraga Pereira
 *
 */
public class BlindDeadEndHeuristic extends Heuristic {

	private Condition goal;

	public BlindDeadEndHeuristic(Problem problem) {
		this(problem, problem.getGoal());
	}

	public BlindDeadEndHeuristic(Problem problem, Condition goal) {
		super(problem, true);
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
		FFHeuristic h = new FFHeuristic(problem, RPGStrategy.MAX);
		if(h.getHeuristic(state) == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		return 1.0;
	}
}
