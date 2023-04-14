package paladinus.explicit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import paladinus.state.Condition;
import paladinus.state.Operator;
import paladinus.util.Pair;

/**
 * An operator consists of a precondition, which is a list of variable-value
 * pairs, a set of non-deterministic conditional effects, and a set of
 * variable-value pairs, which are observable.
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class ExplicitOperator extends Operator {

	/**
	 * An operator rule has as its precondition the precondition of the operator
	 * together with the effect precondition of <strong>one</strong> of the
	 * operator's effects.
	 *
	 * @author Robert Mattmueller
	 */
	public static class OperatorRule {

		public final Set<Pair<Integer, Integer>> body;

		public final Pair<Integer, Integer> head;

		/**
		 * Creates a new operator rule.
		 *
		 * @param body
		 * @param head
		 */
		public OperatorRule(Set<Pair<Integer, Integer>> body, Pair<Integer, Integer> head) {
			this.body = Collections.unmodifiableSet(body);
			this.head = head;
		}

		/**
		 * Test for equality.
		 *
		 * @return true iff. body and head are pairwise equal.
		 */
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof OperatorRule)) {
				return false;
			}
			OperatorRule r = (OperatorRule) o;
			return body.equals(r.body) && head.equals(r.head);
		}

		/**
		 * Get hash code of this operator rule.
		 *
		 * @return hash code
		 */
		@Override
		public int hashCode() {
			return body.hashCode() + head.hashCode();
		}

		@Override
		public String toString() {
			return head + " <-- " + body;
		}
	}

	/**
	 * Set true for debug information as output.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Precondition of this operator.
	 */
	public final ExplicitCondition precondition;

	/**
	 * Nondeterministic operator effects. Each entry in the set of sets gives one
	 * possible nondeterministic choice. Given such a choice, all effects contained
	 * in it will be applied (assuming that their effect conditions are satisfied).
	 */
	private Set<Set<ExplicitEffect>> nondeterministicEffect;

	/**
	 * Creates a new operator with given name, precondition, and nondeterministic
	 * effect.
	 *
	 * @param name                   name of the operator
	 * @param precondition           precondition of the operator
	 * @param nondeterministicEffect nondeterministic effect of the operator
	 */
	public ExplicitOperator(String name, ExplicitCondition precondition,
			Set<Set<ExplicitEffect>> nondeterministicEffect, Set<Pair<Integer, Integer>> observation,
			boolean isAbstracted, double cost) {
		super(name, observation, isAbstracted, nondeterministicEffect != null, cost);
		if (nondeterministicEffect != null) {
			assert !nondeterministicEffect.isEmpty() : "Operators with no effects are expected to be null.";
			assert !(nondeterministicEffect.size() == 1) || !nondeterministicEffect.iterator().next().isEmpty()
					: "Operators with only empty effects are expected to be null.";
		}
		this.precondition = precondition;
		if (nondeterministicEffect == null) {
			this.nondeterministicEffect = null;
		} else {
			setNonDeterministicChoices(nondeterministicEffect);
		}
		for (Pair<Integer, Integer> pair : observation) {
			affectedVariables.add(pair.first);
		}
		// affectedVariables = Collections.unmodifiableSet(affectedVariables); // TODO
		// Affected
		// variables should not change
	}

	/**
	 * Get nondeterministic effect of this operator.
	 *
	 * @return nondeterministic effect
	 */
	public Set<Set<ExplicitEffect>> getNondeterministicEffect() {
		return nondeterministicEffect;
	}

	/**
	 * Set nondeterministic effects of this operator. Should only be used by
	 * constructor and during preprocessing of the operator.
	 *
	 * @param nonDeterministicChoices nondeterministic effect of the operator
	 */
	public void setNonDeterministicChoices(Set<Set<ExplicitEffect>> nonDeterministicChoices) {
		// reset nondeterministic choices
		nondeterministicEffect = new HashSet<Set<ExplicitEffect>>();
		for (Set<ExplicitEffect> choice : nonDeterministicChoices) {
			// add each choice as unmodifiable set
			//nondeterministicEffect.add(Collections.unmodifiableSet(choice));
			nondeterministicEffect.add(choice);
			for (ExplicitEffect effect : choice) {
				affectedVariables.add(effect.variable);
			}
		}
		// add all choices as unmodifiable set
		//nondeterministicEffect = Collections.unmodifiableSet(nondeterministicEffect);
	}

	/**
	 * Computes and returns all operator rules corresponding to this operator.
	 *
	 * @return Corresponding operator rules
	 */
	public Collection<OperatorRule> getRules() {
		Set<OperatorRule> result = new LinkedHashSet<OperatorRule>();
		List<Pair<Integer, Integer>> baseBody = precondition.getVariableValueAssignmentAsPairs();
		for (Set<ExplicitEffect> effects : nondeterministicEffect) {
			for (ExplicitEffect effect : effects) {
				Set<Pair<Integer, Integer>> body = new LinkedHashSet<Pair<Integer, Integer>>();
				body.addAll(baseBody);
				body.addAll(effect.getConditionVariableValuePairs());
				Pair<Integer, Integer> head = new Pair<Integer, Integer>(effect.variable, effect.value);
				OperatorRule effectRule = new OperatorRule(body, head);
				result.add(effectRule);
			}
		}
		return result;
	}

	/**
	 * Get the precondition of this operator.
	 *
	 * @return precondition of this operator
	 */
	@Override
	public Condition getPrecondition() {
		return precondition;
	}

	/**
	 * Abstract this explicit operator to a given pattern by discarding all
	 * non-pattern variables.
	 *
	 * @param pattern set of variables remaining in the abstraction
	 * @return abstracted operator or null if resulting operator has no effects and
	 *         no observations
	 */
	@Override
	public ExplicitOperator abstractToPattern(Set<Integer> pattern) {
		if (DEBUG) {
			System.out.println();
			System.out.println("abstract following operator to pattern: " + pattern);
			dump();
		}

		Set<Set<ExplicitEffect>> abstractedNondeterministicEffect = null;
		Set<Pair<Integer, Integer>> abstractedObservation = new HashSet<Pair<Integer, Integer>>();
		if (nondeterministicEffect != null) {
			abstractedNondeterministicEffect = new HashSet<Set<ExplicitEffect>>();
			// Abstract effects
			for (Set<ExplicitEffect> choice : nondeterministicEffect) {
				Set<ExplicitEffect> absChoice = new HashSet<ExplicitEffect>();
				for (ExplicitEffect effect : choice) {
					if (pattern.contains(effect.variable)) { // keep it
						absChoice.add(effect);
					}
				}
				/*
				 * Note: Duplicates of single deterministic effects are eliminated here. That
				 * means that after abstraction of an operator the probability for a specific
				 * effect could be different from the probability without duplicate elimination.
				 * Example: Let <pre, {{a,b},{a,c},{d}}> be an operator and {a,d} be a pattern.
				 * The resulting abstracted operator (with this simplification) is <abs(pre),
				 * {{a},{d}}>. Every effect has a probability of 1/2 since assuming equal
				 * distribution. Whereas an abstraction to <abs(pre), {{a},{a},{d}}> leads to
				 * probability of 1/3 for every single effect and particularly the specific
				 * effect "a" has a probability of 2/3 (which differs from 1/2). TODO
				 * Investigate and evaluate this decision.
				 */
				abstractedNondeterministicEffect.add(absChoice);
			}
			assert (!abstractedNondeterministicEffect.isEmpty());
			// If all deterministic effects are empty reject this operator.
			if (abstractedNondeterministicEffect.size() == 1
					&& abstractedNondeterministicEffect.iterator().next().isEmpty()) {
				abstractedNondeterministicEffect = null;
			}
		}
		if (observation.size() > 0) {
			// Abstract observations.
			for (Pair<Integer, Integer> varVal : observation) {
				if (pattern.contains(varVal.first)) {
					abstractedObservation.add(varVal);
				}
			}
		}
		// If resulting operator has no effect and no observations, reject it!
		if (abstractedNondeterministicEffect == null && abstractedObservation.isEmpty()) {
			if (DEBUG) {
				System.out.println("Abstracted op is rejected because effect and observations are empty.");
			}
			return null;
		}

		// Abstract condition
		ExplicitCondition abstractedCondition = precondition.abstractToPattern(pattern);

		// Abstract Operator
		if (DEBUG) {
			System.out.println("Resulting abstracted operator:");
			System.out.println("name: " + name + "_abs");
			System.out.println("precondition: " + abstractedCondition);
			System.out.println("effects: " + abstractedNondeterministicEffect);
			System.out.println("observations: " + abstractedObservation);
		}
		return new ExplicitOperator(name + "_abs", abstractedCondition, abstractedNondeterministicEffect,
				abstractedObservation, true, cost);
	}

	/**
	 * Two explicit operators are equal if their precondition and their effect are
	 * equal. Names are ignored.
	 *
	 * @param o object to compare
	 * @return true iff both operators are equivalent
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExplicitOperator)) {
			return false;
		}
		ExplicitOperator other = (ExplicitOperator) o;
		if (!observation.equals(other.observation)) {
			return false;
		}
		if (getPrecondition().equals(other.getPrecondition())) {
			return ((nondeterministicEffect == null && other.nondeterministicEffect == null)
					|| nondeterministicEffect.equals(other.nondeterministicEffect));
		}
		return false;
	}

	/**
	 * Get hash code of this explicit operator.
	 *
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		if (nondeterministicEffect != null) {
			return precondition.hashCode() + nondeterministicEffect.hashCode();
		} else {
			return precondition.hashCode();
		}
	}

	/**
	 * Dump this operator.
	 *
	 * @param stream
	 */
	@Override
	public void dump() {
		System.out.print(name);
		System.out.println();
		System.out.print(precondition);
		System.out.print(" => ");
		int i = 0;
		if (isCausative) {
			for (Set<ExplicitEffect> choice : nondeterministicEffect) {
				int j = 0;
				for (ExplicitEffect eff : choice) {
					System.out.print(eff);
					if (++j < choice.size()) {
						System.out.print(" & ");
					}
				}
				if (++i < nondeterministicEffect.size()) {
					System.out.print("  ||  ");
				}
			}
		}
		System.out.println();
		if (isSensing) {
			System.out.println("obs " + observation);
		}
		System.out.println("cost: " + cost);
	}

	@Override
	public ExplicitOperator getExplicitOperator() {
		return this;
	}

	@Override
	public Operator copy() {
		return new ExplicitOperator(name, precondition, nondeterministicEffect, observation, isAbstracted, cost);
	}

	/**
	 * Set cost of this operator.
	 *
	 * @param cost
	 */
	@Override
	public void setCost(double cost) {
		this.cost = cost;
	}
}
