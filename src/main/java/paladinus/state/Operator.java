package paladinus.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import paladinus.explicit.ExplicitOperator;
import paladinus.util.Pair;

/**
 * @author Manuela Ortlieb
 *
 */
public abstract class Operator {

	/**
	 * Name of this operator.
	 */
	protected String name;

	/**
	 * Observation which is a set of variable-value pairs.
	 */
	public final Set<Pair<Integer, Integer>> observation;

	/**
	 * Denotes if this operator is abstracted.
	 */
	public boolean isAbstracted;

	/**
	 * Denotes if this operator senses variable-value pairs.
	 */
	public final boolean isSensing;

	/**
	 * Denotes if this operator has a non-empty effect, such that it changes the
	 * current state.
	 */
	public final boolean isCausative;

	/**
	 * Cost to apply this operator.
	 */
	protected double cost;

	/**
	 * Variables which are affected (either as part of an effect or as part of an
	 * observation) by this operator.
	 */
	protected Set<Integer> affectedVariables = new HashSet<Integer>();

	/**
	 * Operator.
	 *
	 * @param name         name of the operator
	 * @param precondition precondition of the operator
	 * @param effect       effect of the operator
	 * @param observations variable-value pairs which could be observed by the
	 *                     operator
	 */
	protected Operator(String name, Set<Pair<Integer, Integer>> observation, boolean isAbstracted, boolean isCausative,
			double cost) {
		this.name = name;
		// Note: If this operator has no observations, this map is empty.
		this.observation = Collections.unmodifiableSet(observation);
		this.isAbstracted = isAbstracted;
		isSensing = !observation.isEmpty();
		this.isCausative = isCausative;
		this.cost = cost;
	}

	/**
	 * Make the operator forget it is abstracted.
	 */
	public void forgetAbstraction() {
		isAbstracted = false;
	}

	/**
	 * Get the precondition of this operator.
	 *
	 * @return precondition of this operator
	 */
	public abstract Condition getPrecondition();

	/**
	 * String representation of this operator.
	 *
	 * @return name of the operator
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Checks whether this operator is enabled in a given state, i.e., if its
	 * preconditions are satisfied.
	 *
	 * @param state
	 * @return True iff this operator is enabled in the state
	 */
	public boolean isEnabledIn(State state) {
		return getPrecondition().isSatisfiedIn(state);
	}

	/**
	 * Abstract this operator to given pattern.
	 *
	 * @param pattern
	 * @return abstracted operator or null if resulting operator has no effects and
	 *         no observations
	 */
	public abstract Operator abstractToPattern(Set<Integer> pattern);

	/**
	 * Delete BDDs if they are used.
	 *
	 * Warning: This operator can not be used anymore after calling this method.
	 */
	public void free() {
	}

	/**
	 * Check that there are no duplicate names.
	 *
	 * @param ops operators to check
	 * @return true iff each name is unique
	 */
	public static boolean assertNoDuplicateInNames(Set<? extends Operator> ops) {
		Set<String> names = new HashSet<String>((int) (ops.size() / 0.75) + 1);
		for (Operator op : ops) {
			if (names.contains(op.name)) {
				System.err.println(op.name + " is a duplicate.");
				return false;
			}
			names.add(op.name);
		}
		return true;
	}

	/**
	 * Dumping method for debugging.
	 */
	public abstract void dump();

	/**
	 * Get explicit description of this operator.
	 *
	 * @return explicit operator.
	 */
	public abstract ExplicitOperator getExplicitOperator();

	/**
	 * Get all variables which are affected by this operator.
	 *
	 * @return Set of affected variables.
	 */
	public Set<Integer> getAffectedVariables() {
		return affectedVariables;
	}

	/**
	 * Get the operator's name.
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the operator's name.
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return a copy of this operator. FIXME Shallow or deep copy? (I guess at the
	 * moment it is a mix of both. To be save we should create a deep copy).
	 *
	 * @return
	 */
	public abstract Operator copy();

	/**
	 * Get cost of this operator.
	 *
	 * @return cost
	 */
	public double getCost() {
		return cost;
	}

	public static boolean applicableOperator(Operator op) {
		String[] opSplitted = op.getName().split(" ");
		List<String> listOccurrences = new ArrayList<>();
		for (int i = 0; i < opSplitted.length; i++) {
			if (i == 0)
				continue;
			listOccurrences.add(opSplitted[i]);
		}
		Set<String> occurrences = new HashSet<>(listOccurrences);
		if (listOccurrences.size() > 1 && occurrences.size() == 1)
			return false;
		return true;
	}

	public abstract void setCost(double cost);

}
