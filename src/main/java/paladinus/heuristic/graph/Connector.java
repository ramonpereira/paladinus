package paladinus.heuristic.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import paladinus.state.Operator;

/**
 *
 * @author Robert Mattmueller
 *
 */

public class Connector {

	Node parent;
	Set<Node> children;

	/**
	 * Operator which corresponds to this connector.
	 */
	Operator operator;

	int unsatisfiedPreconditionCount;
	double cost;

	public boolean isSafe = false;
	public String label;

	public Connector(Node parent, Set<Node> children, Operator op) {
		this.parent = parent;
		this.children = children;
		operator = op;
		String name = op.getName();
		assert (!parent.outgoingConnectors.containsKey(name));
		parent.outgoingConnectors.put(name, this);
		for (Node child : children) {
			if (!child.incomingConnectors.containsKey(name)) {
				child.incomingConnectors.put(name, new HashSet<Connector>());
			}
			child.incomingConnectors.get(name).add(this);
		}
		unsatisfiedPreconditionCount = children.size();
		cost = op.getCost();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Connector)) {
			return false;
		}
		Connector c = (Connector) o;
		return parent.equals(c.parent) && children.equals(c.children);
	}

	public Set<Node> getChildren() {
		return children;
	}

	public String getName() {
		return operator.getName();
	}

	public Node getParent() {
		return parent;
	}

	public int getUnsatisfiedPreconditionCount() {
		return unsatisfiedPreconditionCount;
	}

	@Override
	public int hashCode() {
		return parent.hashCode() + children.hashCode();
	}

	public void setChildren(Set<Node> children) {
		this.children = children;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public void setUnsatisfiedPreconditionCount(int unsatisfiedPreconditionCount) {
		this.unsatisfiedPreconditionCount = unsatisfiedPreconditionCount;
	}

	public double getCost() {
		return cost;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(parent.toString());
		result.append("=>{");
		for (Node node : children) {
			result.append(node.toString());
			result.append(":");
		}
		result.append("}");
		return result.toString();
	}

	public static boolean consistencyTestForConnectors(Collection<Node> nodes) {
		// Each outgoing connector has to be an incoming connector of the corresponding
		// node.
		for (Node parent : nodes) {
			for (Connector out : parent.getOutgoingConnectors()) {
				for (Node child : out.children) {
					// Check incoming connectors of this child.
					if (!child.getIncomingConnectors().contains(out)) {
						return false;
					}
				}
			}
		}
		// Each incoming connector has to be an outgoing connector of the corresponding
		// node.
		for (Node child : nodes) {
			for (Connector in : child.getIncomingConnectors()) {
				Node parent = in.parent;
				if (!parent.getOutgoingConnectors().contains(in)) {
					return false;
				}
			}
		}
		return true;
	}
}
