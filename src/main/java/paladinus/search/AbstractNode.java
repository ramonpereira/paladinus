package main.java.paladinus.search;

import java.math.BigInteger;

import main.java.paladinus.Global;
import main.java.paladinus.state.State;

/**
 * Node in the search space.
 *
 * @author Robert Mattmueller
 */
public abstract class AbstractNode {

	/**
	 * Heuristic value indicating proven nodes
	 */
	public static final double PROVEN = 0.0;

	/**
	 * Heuristic value indicating disproven nodes
	 */
	public static final double DISPROVEN = Double.POSITIVE_INFINITY;

	/**
	 * Next free index to be used for node numbering
	 */
	private static int nextFreeIndex = 0;

	/**
	 * Get number of created nodes.
	 *
	 * @return number of created nodes
	 */
	public static int getNumberOfNodes() {
		return nextFreeIndex;
	}

	/**
	 * Resets the next free index counter which is used to number nodes.
	 */
	public static void resetIndex() {
		nextFreeIndex = 0;
	}

	/**
	 * Unique index
	 */
	public final int index;

	/**
	 * State represented by this node.
	 */
	public final State state;

	/**
	 * Flag indicating that this node has been proven, i.e., the protagonist has a
	 * winning strategy for this node.
	 */
	private boolean isProven = false;

	/**
	 * Flag indicating that this node has been disproven, i.e., the antagonist has a
	 * winning strategy for this node.
	 */
	private boolean isDisproven = false;

	/**
	 * Flag indicating that this node has already been expanded.
	 */
	private boolean isExpanded = false;

	/**
	 * Random number to sort nodes by random.
	 */
	public int random = Global.generator.nextInt() & Integer.MAX_VALUE;

	/**
	 * Indicates if this is a goal node.
	 */
	private boolean isGoalNode = false;
	
	private boolean isDeadEndNode = false;

	/**
	 * Unique id of the represented state.
	 */
	final BigInteger stateID;

	/**
	 * Creates a new node for a given state.
	 *
	 * @param state State to be represented
	 */
	public AbstractNode(State state) {
		this.state = state;
		stateID = state.uniqueID;
		index = nextFreeIndex++;
		if (state.isGoalState()) {
			setGoalNode(true);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractNode)) {
			return false;
		}
		AbstractNode other = (AbstractNode) o;
		return stateID.equals(other.stateID);
	}

	@Override
	public int hashCode() {
		return stateID.intValue();
	}

	@Override
	public String toString() {
		return state.toString();
	}

	public void setExpanded() {
		assert isExpanded == false : "It does not make sense to expand a node a second time.";
		isExpanded = true;
		// state.beliefStateBDD.free();
		// state = null;
	}

	public boolean isExpanded() {
		return isExpanded;
	}

	public int getIndex() {
		return index;
	}

	public boolean isProven() {
		return isProven;
	}

	public boolean isDisproven() {
		return isDisproven;
	}

	public void setProven() {
		assert !isDisproven;
		isProven = true;
	}

	public void unsetProven() {
		assert isProven;
		isProven = false;
	}

	public void setDisproven() {
		assert !isProven;
		isDisproven = true;
	}

	public boolean isDecided() {
		return isProven || isDisproven;
	}

	public boolean isGoalNode() {
		return isGoalNode;
	}

	public void setGoalNode(boolean isGoalNode) {
		this.isGoalNode = isGoalNode;
	}
	
	public void setDeadEndNode(boolean isDeadEndNode) {
		this.isDeadEndNode = isDeadEndNode;
	}
	
	public boolean isDeadEndNode() {
		return isDeadEndNode;
	}
}
