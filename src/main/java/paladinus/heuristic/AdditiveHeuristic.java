package main.java.paladinus.heuristic;

import main.java.paladinus.heuristic.FFHeuristic.RPGStrategy;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.State;

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
