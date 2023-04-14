package paladinus.explicit;

import java.util.List;
import java.util.Map;

import paladinus.util.Pair;

/**
 * An effect consists of a condition <tt>cond</tt>, which is a list of
 * variable-value pairs, an affected variable <tt>var</tt>, and a new value
 * <tt>val</tt> for <tt>var</tt>. Thus, it is basically a horn clause. Effects
 * are normalized, i.e., there are no nested conditions.
 *
 * @author Robert Mattmueller
 */
public class ExplicitEffect {

	/**
	 * Effect condition.
	 */
	public final ExplicitCondition condition;

	/**
	 * Affected variable.
	 */
	public final int variable;

	/**
	 * New value.
	 */
	public final int value;

	/**
	 * Creates an effect with given condition, affected variable and new value.
	 *
	 * @param condition Condition
	 * @param variable  Affected variable
	 * @param value     New value
	 */
	public ExplicitEffect(ExplicitCondition condition, int variable, int value) {
		this.condition = condition;
		this.variable = variable;
		this.value = value;
	}

	/**
	 * Get condition variable-value assignment as integer pairs.
	 *
	 * @return Condition variable-value assignment as list of pairs
	 */
	public List<Pair<Integer, Integer>> getConditionVariableValuePairs() {
		return condition.getVariableValueAssignmentAsPairs();
	}

	/**
	 * Checks whether this effect is enabled in a given variable assignment, i.e.,
	 * if its condition is satisfied.
	 *
	 * @param variableValueAssignment variable assignment
	 * @return true iff this effect is enabled in given assignment
	 */
	public boolean isEnabledIn(Map<Integer, Integer> variableValueAssignment) {
		return condition.isSatisfiedIn(variableValueAssignment);
	}

	/**
	 * Test equality of this effect and given object.
	 *
	 * @param obj other object
	 * @result true iff given object and this object are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExplicitEffect)) {
			return false;
		}
		ExplicitEffect other = (ExplicitEffect) obj;
		return other.variable == variable && other.value == value && other.condition.equals(condition);
	}

	/**
	 * Get hash code of this effect.
	 *
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return 2 * variable + 3 * value + condition.hashCode();
	}

	/**
	 * Get string representation of this effect.
	 *
	 * @return string representation of this effect
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		if (!condition.isTrue()) {
			buffer.append(condition.toString());
			buffer.append("-> ");
		}
		buffer.append(variable + ":" + value + ")");
		return buffer.toString();
	}
}
