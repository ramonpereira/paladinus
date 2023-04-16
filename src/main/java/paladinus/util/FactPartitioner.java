package paladinus.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import paladinus.explicit.ExplicitCondition;
import paladinus.explicit.ExplicitEffect;
import paladinus.explicit.ExplicitOperator;
import paladinus.explicit.ExplicitState;
import paladinus.problem.Problem;
import paladinus.state.Operator;
import paladinus.state.State;

/**
 * 
 * @author Ramon Fraga Pereira
 *
 */
public class FactPartitioner {

	private static FactPartitioner INSTANCE;

	private Set<Pair<Integer, Integer>> strictlyTerminal = new HashSet<>();
	private Set<Pair<Integer, Integer>> unstableActivating = new HashSet<>();
	private Set<Pair<Integer, Integer>> unstableActivatingNegatedFacts = new HashSet<>();
	private Set<Pair<Integer, Integer>> strictlyActivating = new HashSet<>();
	private Set<Pair<Integer, Integer>> unstableTerminal = new HashSet<>();

	private Set<String> strictlyTerminalNames = new HashSet<>();
	private Set<String> strictlyActivatingNames = new HashSet<>();
	private Set<String> unstableTerminalNames = new HashSet<>();
	private Set<String> unstableActivatingNames = new HashSet<>();
	private Set<String> unstableActivatingNegatedNames = new HashSet<>();
	
	private Map<Pair<Integer, Integer>, Set<Operator>> unstableActivatingFactToOperators = new HashMap<>();
	
	private Problem problem;

	private FactPartitioner(Problem problem) {
		this.problem = problem;
		this.partitionFacts(problem);
	}

	public static FactPartitioner getInstance(Problem problem) {
		if (INSTANCE == null)
			INSTANCE = new FactPartitioner(problem);

		return INSTANCE;
	}

	public static FactPartitioner getInstance() {
		if (INSTANCE == null)
			return null;

		return INSTANCE;
	}

	private void partitionFacts(Problem problem) {
		Set<Pair<Integer, Integer>> varsPropositions = new HashSet<>();
		for (int i = 0; i < problem.propositionNames.size(); i++) {
			List<String> props = problem.propositionNames.get(i);
			for (int j = 0; j < props.size(); j++)
				varsPropositions.add(new Pair<Integer, Integer>(i, j));
		}

		Set<Operator> operators = problem.getOperators();
		for (Pair<Integer, Integer> prop : varsPropositions) {
			String propositionName = problem.propositionNames.get(prop.first).get(prop.second);
			if (propositionName.contains("not"))
				continue;

			/* Strictly Terminal */
			/*
			if (this.isStrictlyTerminal(problem, prop, operators)) {
				this.strictlyTerminal.add(prop);
				this.strictlyTerminalNames.add(propositionName);
			}
			*/

			/* Unstable Activating */
			if (this.isUnstableActivating(problem, prop, operators)) {
				this.unstableActivating.add(prop);
				this.unstableActivatingNames.add(propositionName);
			}

			/* Strictly Activating */
			/*
			if (this.isStrictlyActivating(problem, prop, operators)) {
				this.strictlyActivating.add(prop);
				this.strictlyActivatingNames.add(propositionName);
			}
			*/

			/* Unstable Terminal */
			/*
			if (this.isUnstableTerminal(problem, prop, operators)) {
				this.unstableTerminal.add(prop);
				this.unstableTerminalNames.add(propositionName);
			}
			*/
		}
		for (String uA : this.unstableActivatingNames) {
			for (Pair<Integer, Integer> prop : varsPropositions) {
				String propositionName = problem.propositionNames.get(prop.first).get(prop.second);

				if (propositionName.contains(uA) && propositionName.contains("not")) {
					this.unstableActivatingNegatedFacts.add(new Pair<Integer, Integer>(prop.first, prop.second));
					this.unstableActivatingNegatedNames.add(propositionName);
				}
			}
		}
	}

	/**
	 * Returns true if the Fact appears in any action's delete effects or
	 * preconditions. This means that if true is returned that the Fact will remain
	 * in true throughout execution.
	 * 
	 * @param p
	 * @param actions
	 * @return
	 */
	public boolean isStrictlyTerminal(Problem problem, Pair<Integer, Integer> f, Set<Operator> operators) {
		boolean foundStrictlyTerminal = false;
		String propositionName = problem.propositionNames.get(f.first).get(f.second);
		for (Operator o : operators) {
			ExplicitOperator eO = (ExplicitOperator) o;
			if (((ExplicitCondition) eO.getPrecondition()).getVariableValueAssignmentAsPairs().contains(f))
				return false;

			Set<Set<ExplicitEffect>> possibleEffects = eO.getNondeterministicEffect();

			for (Set<ExplicitEffect> effects : possibleEffects) {
				for (ExplicitEffect eff : effects) {
					Pair<Integer, Integer> varValue = new Pair<Integer, Integer>(eff.variable, eff.value);
					String effName = problem.propositionNames.get(eff.variable).get(eff.value);

					/* Delete Effects */
					if (effName.contains("not") && effName.contains(propositionName))
						return false;

					/* Add Effects */
					if (varValue.equals(f))
						foundStrictlyTerminal = true;
				}
			}
		}
		return foundStrictlyTerminal;
	}

	public boolean stateContainsUnstableActivating(State state) {
		for (ExplicitState s : state.getAllExplicitWorldStates()) {
			for (Pair<Integer, Integer> uA : this.unstableActivatingNegatedFacts) {
				if (s.getVarsPropositions().contains(uA))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns true only if the Fact appears as a Precondition, but also as a Delete
	 * effect somewhere.
	 * 
	 * @param p
	 * @param actions
	 * @return
	 */
	public boolean isUnstableActivating(Problem problem, Pair<Integer, Integer> f, Set<Operator> operators) {
		int preCount = 0;
		int delCount = 0;
		String propositionName = problem.propositionNames.get(f.first).get(f.second);
		Set<Operator> operatorsUA = new HashSet<>();
		for (Operator o : operators) {
			ExplicitOperator eO = (ExplicitOperator) o;
			if (((ExplicitCondition) eO.getPrecondition()).getVariableValueAssignmentAsPairs().contains(f))
				preCount++;

			Set<Set<ExplicitEffect>> possibleEffects = eO.getNondeterministicEffect();

			for (Set<ExplicitEffect> effects : possibleEffects) {
				for (ExplicitEffect eff : effects) {
					Pair<Integer, Integer> varValue = new Pair<Integer, Integer>(eff.variable, eff.value);
					String effName = problem.propositionNames.get(eff.variable).get(eff.value);

					/* Delete Effects */
					if (effName.contains("not") && effName.contains(propositionName)) {
						operatorsUA.add(eO);
						delCount++;
					}

					/* Add Effects */
					if (varValue.equals(f))
						return false;
				}
			}
		}
		if (preCount > 0 && delCount > 0) {
			this.unstableActivatingFactToOperators.put(f, operatorsUA);
			return true;
		} else
			return false;
	}

	/**
	 * Returns true if the specified Fact only ever appears as a precondition, and
	 * never as an add OR delete effect. This means that unless true in the initial
	 * tmstate, the Fact can never become true during execution.
	 * 
	 * @param p
	 * @param actions
	 * @return
	 */
	public boolean isStrictlyActivating(Problem problem, Pair<Integer, Integer> f, Set<Operator> operators) {
		int preCount = 0;
		String propositionName = problem.propositionNames.get(f.first).get(f.second);
		for (Operator o : operators) {
			ExplicitOperator eO = (ExplicitOperator) o;
			if (((ExplicitCondition) eO.getPrecondition()).getVariableValueAssignmentAsPairs().contains(f))
				preCount++;

			Set<Set<ExplicitEffect>> possibleEffects = eO.getNondeterministicEffect();

			for (Set<ExplicitEffect> effects : possibleEffects) {
				for (ExplicitEffect eff : effects) {
					Pair<Integer, Integer> varValue = new Pair<Integer, Integer>(eff.variable, eff.value);
					String effName = problem.propositionNames.get(eff.variable).get(eff.value);

					/* Delete Effects */
					if (effName.contains("not") && effName.contains(propositionName))
						return false;

					/* Add Effects */
					if (varValue.equals(f))
						return false;
				}
			}
		}
		return preCount > 0;
	}

	/**
	 * Returns true if the specified Fact appears in the effects of actions but that
	 * it can also be deleted once made true.
	 * 
	 * @param p
	 * @param actions
	 * @return True if the Fact appears at least once in any action's add and delete
	 *         effects.
	 */
	public boolean isUnstableTerminal(Problem problem, Pair<Integer, Integer> f, Set<Operator> operators) {
		int addCount = 0;
		int delCount = 0;
		String propositionName = problem.propositionNames.get(f.first).get(f.second);
		for (Operator o : operators) {
			ExplicitOperator eO = (ExplicitOperator) o;
			if (((ExplicitCondition) eO.getPrecondition()).getVariableValueAssignmentAsPairs().contains(f))
				return false;

			Set<Set<ExplicitEffect>> possibleEffects = eO.getNondeterministicEffect();

			for (Set<ExplicitEffect> effects : possibleEffects) {
				for (ExplicitEffect eff : effects) {
					Pair<Integer, Integer> varValue = new Pair<Integer, Integer>(eff.variable, eff.value);
					String effName = problem.propositionNames.get(eff.variable).get(eff.value);

					/* Delete Effects */
					if (effName.contains("not") && effName.contains(propositionName))
						delCount++;

					/* Add Effects */
					if (varValue.equals(f))
						addCount++;
				}
			}
		}
		if (addCount > 0 && delCount > 0) {
			return true;
		} else
			return false;
	}

	@Override
	public String toString() {
		String partitionFacts = "";

		partitionFacts += "> Strictly Activating:\n";
		for (String sa : this.strictlyActivatingNames)
			partitionFacts += "\t" + sa + "\n";

		partitionFacts += "\n> Strictly Terminal:\n";
		for (String st : this.strictlyTerminalNames)
			partitionFacts += "\t" + st + "\n";

		partitionFacts += "\n> Unstable Activating:\n";
		for (String ua : this.unstableActivatingNames)
			partitionFacts += "\t" + ua + "\n";

		partitionFacts += "\n> Unstable Terminal:\n";
		for (String ut : this.unstableTerminalNames)
			partitionFacts += "\t" + ut + "\n";

		return partitionFacts;
	}
	
	public Set<Operator> getUnstableOperatorsForGoalFacts(){
		Set<Operator> unstableOperators = new HashSet<>();
		List<Pair<Integer, Integer>> goalFacts = ((ExplicitCondition) problem.getGoal()).getVariableValueAssignmentAsPairs();
		for(Pair<Integer, Integer> fact: goalFacts) {
			if(this.getUnstableActivating().contains(fact)) {
				unstableOperators.addAll(this.unstableActivatingFactToOperators.get(fact));
			}
		}
		return unstableOperators;
	}

	public Set<Pair<Integer, Integer>> getStrictlyActivating() {
		return strictlyActivating;
	}

	public Set<Pair<Integer, Integer>> getStrictlyTerminal() {
		return strictlyTerminal;
	}

	public Set<Pair<Integer, Integer>> getUnstableActivating() {
		return unstableActivating;
	}

	public Set<Pair<Integer, Integer>> getUnstableTerminal() {
		return unstableTerminal;
	}
}
