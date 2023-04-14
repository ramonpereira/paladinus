package paladinus.heuristic;

import paladinus.heuristic.FFHeuristic.RPGStrategy;
import paladinus.problem.Problem;
import paladinus.state.State;

public class AdditiveHeuristic extends Heuristic {

	FFHeuristic heuristic;

	public AdditiveHeuristic(Problem problem) {
		super(problem, true);
		heuristic = new FFHeuristic(problem, RPGStrategy.ADD);
	}

	@Override
	public double getHeuristic(State state) {
		return heuristic.getHeuristic(state);
	}
}
