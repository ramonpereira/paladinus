package paladinus.heuristic;

import paladinus.heuristic.FFHeuristic.RPGStrategy;
import paladinus.problem.Problem;
import paladinus.state.State;

public class HMaxHeuristic extends Heuristic {

	FFHeuristic heuristic;

	public HMaxHeuristic(Problem problem) {
		super(problem, true);
		heuristic = new FFHeuristic(problem, RPGStrategy.MAX);
	}

	@Override
	public double getHeuristic(State state) {
		return heuristic.getHeuristic(state);
	}
}
