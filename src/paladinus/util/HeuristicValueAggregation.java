package paladinus.util;

import java.util.Collection;

import paladinus.explicit.ExplicitState;
import paladinus.heuristic.Heuristic;

/**
 *
 * @author Manuela Ortlieb
 *
 */

public class HeuristicValueAggregation {

	/**
	 * Return maximal heuristic value of given explicit states.
	 *
	 * @param states collection of explicit states
	 * @param h      heuristic
	 * @return maximal heuristic vlaue of given states
	 */
	public static double maximize(Collection<ExplicitState> states, Heuristic h) {
		assert !states.isEmpty();
		double max = -1;
		for (ExplicitState state : states) {
			double hValue = h.getHeuristic(state);
			if (hValue > max) {
				max = hValue;
			}
		}
		assert max >= 0;
		return max;
	}

	/**
	 * Return sum of given explicit states' heuristic values.
	 *
	 * @param states collection of explicit states
	 * @param h      heuristic
	 * @return sum of given states' heuristic values
	 */
	public static double add(Collection<ExplicitState> states, Heuristic h) {
		assert !states.isEmpty();
		double sum = 0;
		for (ExplicitState state : states) {
			sum += h.getHeuristic(state);
		}
		assert sum >= 0;
		return sum;
	}

	/**
	 * Return average heuristic value of given explicit states.
	 *
	 * @param states collection of explicit states
	 * @param h      heuristic
	 * @return average heuristic value of given states
	 */
	public static double average(Collection<ExplicitState> states, Heuristic h) {
		assert !states.isEmpty();
		double sum = add(states, h);
		return sum / states.size();
	}

}
