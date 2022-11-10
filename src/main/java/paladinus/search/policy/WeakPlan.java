package main.java.paladinus.search.policy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;

public class WeakPlan extends Policy {

	public static class WeakPlanEntry {
		public final State state;
		public final Operator action;
		public final Set<State> unintendedOutcomes;

		public WeakPlanEntry(State state, Operator action) {
			this.state = state;
			this.action = action;
			unintendedOutcomes = new HashSet<State>();
		}
	}

	public final State reachedGoal;
	public final LinkedList<WeakPlanEntry> entries;
	public final int distanceOffset;

	public WeakPlan(Problem problem, State reachedGoal, int distanceOffset) {
		super(problem);
		this.reachedGoal = reachedGoal;
		entries = new LinkedList<WeakPlanEntry>();
		this.distanceOffset = distanceOffset;
	}

	public Policy toPolicy() {
		Policy result = new Policy(problem);
		Iterator<WeakPlanEntry> itr = entries.descendingIterator();
		int count = 0;
		while (itr.hasNext()) {
			WeakPlanEntry curr = itr.next();
			result.addEntry(curr.state, curr.action, distanceOffset + (++count));
		}
		return result;
	}

	@Override
	public boolean containsEntry(State state) {
		for (WeakPlanEntry e : entries) {
			if (e.state.equals(state)) {
				return true;
			}
		}
		return false;
	}

	public WeakPlanEntry get(State state) {
		for (WeakPlanEntry e : entries) {
			if (e.state.equals(state)) {
				return e;
			}
		}
		return null;
	}
}
