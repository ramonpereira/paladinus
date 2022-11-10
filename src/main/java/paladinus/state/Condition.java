package main.java.paladinus.state;

public interface Condition {

	/**
	 * Tests if this condition is satisfied by the given state.
	 *
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 */
	boolean isSatisfiedIn(State state);

	/**
	 * Dump this condition. Only for debugging.
	 */
	void dump();

}
