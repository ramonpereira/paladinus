package main.java.paladinus.problem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.java.paladinus.explicit.ExplicitCondition;
import main.java.paladinus.explicit.ExplicitOperator;
import main.java.paladinus.state.Operator;
import main.java.paladinus.util.Pair;

public class OperatorAnalyzer {

	/**
	 * A graph containing nodes and connectors (hyperarcs).
	 *
	 * @author Manuela Ortlieb
	 *
	 */
	public class DependencyGraph {

		/**
		 * Map from state variables to the node which represents it.
		 */
		private Map<Integer, Node> nodes = new HashMap<Integer, Node>();

		private Set<Connector> connectors = new HashSet<Connector>();

		private Set<Integer> labels = new HashSet<Integer>();

		public void addNode(int stateVar) {
			if (!nodes.containsKey(stateVar)) {
				nodes.put(stateVar, new Node(stateVar));
			}
		}

		public Node getNode(int stateVar) {
			if (nodes.containsKey(stateVar)) {
				return nodes.get(stateVar);
			}
			return null;
		}

		public boolean containsNode(int stateVar) {
			return nodes.containsKey(stateVar);
		}

		public boolean containsLabel(int label) {
			return labels.contains(label);
		}

		public void addEdge(int from, int label, int to) {
			assert nodes.containsKey(from);
			assert nodes.containsKey(to);
			labels.add(label);
			boolean foundLabel = false;
			for (Connector con : nodes.get(from).outgoingConnectors) {
				if (con.label == label) {
					con.addChild(nodes.get(to));
					foundLabel = true;
				}
			}
			if (!foundLabel) {
				// Create a new connector.
				connectors.add(new Connector(label, getNode(from), new HashSet<Node>(Arrays.asList(getNode(to)))));
			}
		}

		@Override
		public String toString() {
			String s = "Nodes: ";
			s += nodes.keySet().toString();
			s += "\nConnectors: ";
			s += connectors.toString();
			return s;
		}
	}

	/**
	 * A connector is a directed edge from a node to a set of nodes.
	 *
	 * @author Manuela Ortlieb
	 *
	 */
	public class Connector {

		/**
		 * Description label for the connector.
		 */
		public final int label;

		/**
		 * Parent node;
		 */
		public final Node parent;

		/**
		 * Children nodes.
		 */
		private Set<Node> children;

		public Connector(int label, Node parent, Set<Node> children) {
			this.label = label;
			this.parent = parent;
			this.children = children;
			parent.outgoingConnectors.add(this);
			for (Node child : children) {
				child.incomingConnectors.add(this);
			}
		}

		public void addChild(Node child) {
			children.add(child);
			child.incomingConnectors.add(this);
		}

		@Override
		public int hashCode() {
			return parent.hashCode() * 7 + label;
		}

		@Override
		public boolean equals(Object obj) {
			// System.err.println();
			System.err.println("equals: " + this + " / " + obj);
			if (!(obj instanceof Connector)) {
				return false;
			}
			Connector other = (Connector) obj;
			if (label != other.label) {
				// System.err.println("label != label");
				return false;
			}
			if (!parent.equals(other.parent)) {
				// System.err.println("parent != parent");
				return false;
			}
			// if (!children.equals(other.children)) {
			// System.err.println("children != children");
			// }
			return children.equals(other.children);
		}

		@Override
		public String toString() {
			return parent + " ==" + label + "==> " + children.toString();
		}

		public Set<Node> getChildren() {
			return children;
		}
	}

	public class Node {

		/**
		 * State variable which is represented by this node.
		 */
		public final int stateVariable;

		public Set<Connector> incomingConnectors = new HashSet<Connector>();

		public Set<Connector> outgoingConnectors = new HashSet<Connector>();

		public Node(int stateVariable) {
			this.stateVariable = stateVariable;
		}

		@Override
		public int hashCode() {
			return stateVariable;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Node)) {
				return false;
			}
			Node other = (Node) obj;
			return other.stateVariable == stateVariable;
		}

		@Override
		public String toString() {
			return stateVariable + "";
		}

	}

	public static DependencyGraph analyze(Set<Operator> ops) {
		boolean DEBUG = false;
		if (DEBUG) {
			System.out.println("Analyze...");
		}
		DependencyGraph graph = (new OperatorAnalyzer()).new DependencyGraph();
		Set<ExplicitOperator> closed = new HashSet<ExplicitOperator>();
		for (Operator op : ops) {
			if (!op.isSensing) {
				continue;
			}
			ExplicitOperator explicitOp = op.getExplicitOperator();
			if (DEBUG) {
				System.out.println("sensing op " + explicitOp + " (precondition " + explicitOp.precondition
						+ " / observation " + explicitOp.observation + ")");
			}

			// Add a node to the graph for each variable which is observed
			for (Pair<Integer, Integer> fact : explicitOp.observation) {
				graph.addNode(fact.first);
				if (DEBUG) {
					System.out.println("Node is added for variable " + fact.first);
				}
			}

			// Compare this operator with the operators of the closed list.
			for (ExplicitOperator other : closed) {
				ExplicitCondition cond1 = (ExplicitCondition) explicitOp.getPrecondition();
				ExplicitCondition cond2 = (ExplicitCondition) other.getPrecondition();

				// Test if one operator's precondition is satisfied by the other one.
				if (cond1.isSatisfiedIn(cond2.variableValueMap)) {
					if (DEBUG) {
						System.out.println(explicitOp.observation + " " + cond1 + " is satisfied by " + cond2 + " "
								+ other.observation);
					}
					// Add an edge between the two observed variables labeled with the state
					// variable
					for (Pair<Integer, Integer> fact : explicitOp.observation) {
						for (Pair<Integer, Integer> fact2 : other.observation) {
							for (Integer var : cond2.variableValueMap.keySet()) {
								if (cond1.isTrue() || cond1.variableValueMap.keySet().contains(var)) {
									graph.addEdge(fact2.first, var, fact.first);
								}
							}
						}
					}
				}
				if (cond2.isSatisfiedIn(cond1.variableValueMap)) {
					if (DEBUG) {
						System.out.println(other.observation + " " + cond1 + " is satisfied by " + cond2 + " "
								+ explicitOp.observation);
					}

					// Add an edge between the two observed variables labeled with the state
					// variable
					for (Pair<Integer, Integer> fact : other.observation) {
						for (Pair<Integer, Integer> fact2 : explicitOp.observation) {
							for (Integer var : cond1.variableValueMap.keySet()) {
								graph.addEdge(fact2.first, var, fact.first);
							}
						}
					}
				}
			}
			closed.add(op.getExplicitOperator());
		}
		if (DEBUG) {
			System.err.println(graph);
		}
		return graph;
	}
}
