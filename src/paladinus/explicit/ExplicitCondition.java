package paladinus.explicit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import paladinus.heuristic.pdb.Abstraction;
import paladinus.state.Condition;
import paladinus.state.State;
import paladinus.util.Pair;

/**
 * A condition is a set (conjunction) of variable-value pairs.
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class ExplicitCondition implements Condition {

	/**
	 * Mapping from variables to values.
	 */
	public final Map<Integer, Integer> variableValueMap;

	/**
	 * Number of variables to which this condition refers.
	 */
	public final int size;

	/**
	 * Set true to get more information as output.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Create a condition which is an assignment of variables to values.
	 *
	 * @param variableValueMap mapping from variables to values
	 */
	public ExplicitCondition(Map<Integer, Integer> variableValueMap) {
		this.variableValueMap = Collections.unmodifiableMap(variableValueMap);
		size = variableValueMap.size();
	}

	/**
	 * Get variable-value assignment of this condition as integer pairs.
	 *
	 * @return Variable-value assignment as list of pairs
	 */
	public List<Pair<Integer, Integer>> getVariableValueAssignmentAsPairs() {
		ArrayList<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
		for (int var : variableValueMap.keySet()) {
			result.add(new Pair<Integer, Integer>(var, variableValueMap.get(var)));
		}
		return result;
	}

	/**
	 * Test if this condition is tautologically true.
	 *
	 * @return true iff this condition is tautologically true
	 */
	public boolean isTrue() {
		return size == 0;
	}

	/**
	 * Tests if this condition is satisfied by the given state.
	 *
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 */
	@Override
	public boolean isSatisfiedIn(State state) {
		return isSatisfiedIn(((ExplicitState) state).variableValueAssignment);
	}

	/**
	 * Tests if this condition is satisfied by the given assignment.
	 *
	 * @param variableValueAssingment variable assignment
	 * @return true iff this condition is satisfied in the given assignment
	 */
	public boolean isSatisfiedIn(Map<Integer, Integer> variableValueAssignment) {
		if (DEBUG) {
			System.out.println("is condition " + this + " satisfied in " + variableValueAssignment);
		}
		for (int var : variableValueMap.keySet()) {
			if (!variableValueAssignment.containsKey(var)) {
				return false;
			}
			if (variableValueAssignment.get(var).intValue() != variableValueMap.get(var).intValue()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Abstraction of this condition to given set of variables. Returns the
	 * abstracted condition.
	 *
	 * @param pattern set of variables
	 * @return abstracted explicit condition induced by given pattern
	 */
	public ExplicitCondition abstractToPattern(Set<Integer> pattern) {
		return new ExplicitCondition(Abstraction.abstractVariableValueMap(variableValueMap, pattern));
	}

	/**
	 * Test if given object and this condition are equal.
	 *
	 * @param obj other object
	 * @return true iff given object and this condition are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExplicitCondition)) {
			return false;
		}
		return variableValueMap.equals(((ExplicitCondition) obj).variableValueMap);
	}

	/**
	 * Get hash code of this condition.
	 *
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return variableValueMap.hashCode();
	}

	/**
	 * Get string representation of this condition.
	 *
	 * @return string representation of this condition
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int var : variableValueMap.keySet()) {
			buffer.append(var);
			buffer.append(":");
			buffer.append(variableValueMap.get(var));
			buffer.append(" ");
		}
		return buffer.toString();
	}

	/**
	 * Dump this explicit condition. Use for debugging.
	 */
	@Override
	public void dump() {
		System.out.println(this);
	}
}
