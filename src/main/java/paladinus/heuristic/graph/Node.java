package paladinus.heuristic.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import paladinus.heuristic.Heuristic;
import paladinus.state.State;

/**
 *
 * @author Robert Mattmueller
 *
 */

public class Node {

	public static final double UNINITIALIZED_COST_ESTIMATE = Double.POSITIVE_INFINITY;

	State state;
	double gValue;
	double fValue;
	double hValue;
	Node parent; // predecessor on shortest path from init
	Node child; // successor on shortest path to goal:
	double costEstimate;
	private int sequenceNumber;
	public HashMap<String, HashSet<Connector>> incomingConnectors; // operator name to connectors
	HashMap<String, Connector> outgoingConnectors;
	boolean proven = false;
	public boolean isGoalNode;
	boolean alreadyBucketed = false;
	public int index;

	public Node(State state) {
		this.state = state;
		costEstimate = Node.UNINITIALIZED_COST_ESTIMATE;
		if (state.isGoalState()) {
			costEstimate = 0.0;
			isGoalNode = true;
		}
		index = state.hashCode;
		gValue = Heuristic.INFINITE_HEURISTIC;
		hValue = Heuristic.INFINITE_HEURISTIC;
		parent = null;
		child = null;
		incomingConnectors = new HashMap<String, HashSet<Connector>>();
		outgoingConnectors = new HashMap<String, Connector>();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Node)) {
			return false;
		}
		return index == ((Node) o).index;
	}

	public double getCostEstimate() {
		return costEstimate;
	}

	public Collection<Connector> getIncomingConnectors() {
		HashSet<Connector> connectors = new HashSet<Connector>();
		for (HashSet<Connector> c : incomingConnectors.values()) {
			connectors.addAll(c);
		}
		return connectors;
	}

	public Collection<Connector> getOutgoingConnectors() {
		return outgoingConnectors.values();
	}

	public Map<String, Connector> getOutgoingConnectorsWithNames() {
		return outgoingConnectors;
	}

	@Override
	public int hashCode() {
		return index;
	}

	public boolean isProven() {
		return proven;
	}

	public void setCostEstimate(double costEstimate) {
		this.costEstimate = costEstimate;
	}

	public void setProven(boolean isProven) {
		proven = isProven;
	}

	@Override
	public String toString() {
		return state.toString();
	}

	public double getgValue() {
		return gValue;
	}

	public void setgValue(double gValue) {
		this.gValue = gValue;
	}

	public double getfValue() {
		return fValue;
	}

	public void setfValue(double fValue) {
		this.fValue = fValue;
	}

	public double gethValue() {
		return hValue;
	}

	public void sethValue(int hValue) {
		this.hValue = hValue;
	}

	public State getState() {
		return state;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void free() {
		state.free();
	}
}
