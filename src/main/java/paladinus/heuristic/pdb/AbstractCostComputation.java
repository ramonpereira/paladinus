package paladinus.heuristic.pdb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import paladinus.Global;
import paladinus.heuristic.graph.Connector;
import paladinus.heuristic.graph.Node;
import paladinus.problem.Problem;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.util.Pair;

/**
 *
 * This class implements the search in the abstract state space used to
 * determine abstract cost values. In a first step, the forward reachable
 * abstract state space is generated, whereas in the second step, this state
 * space is traversed in backward direction to label abstract states with
 * abstract cost values.
 *
 * @author Pascal Bercher
 */
public class AbstractCostComputation {
	/**
	 * The problem this abstract cost computation operates on
	 */
	protected final Problem problem;

	/**
	 * Treshold for value iteration to determine that the cost values converge.
	 */
	public static final double EPSILON = 0.0001;

	/**
	 * Discount factor to enforce convergence of value iteration when maximizing (if
	 * expected cost value is finite).
	 */
	public static final float DISCOUNT_FACTOR = 0.75f;

	/**
	 * Abstraction of a planning problem.
	 */
	private final Abstraction abstraction;

	/**
	 * Map from hash code of a state to node to avoid duplicates.
	 */
	private HashMap<Integer, Node> nodes;

	/**
	 * For debugging.
	 */
	private static int callCounter = 0;

	/**
	 * Set true for debug output information.
	 */
	public final static boolean DEBUG = false;

	/**
	 * Abstract cost computation for given abstraction.
	 *
	 * @param abstraction
	 */
	public AbstractCostComputation(Problem problem, Abstraction abstraction) {
		this.abstraction = abstraction;
		this.problem = problem;
		nodes = new HashMap<Integer, Node>((int) (PDB.numAbstractStates(this.problem, abstraction.pattern) / 0.75) + 1);
	}

	@SuppressWarnings("unused")
	private void strongBackwardEvaluation() {
		Queue<Object> queue = new LinkedList<Object>();
		Set<Node> seenNodes = new LinkedHashSet<Node>();

		for (Node node : nodes.values()) {
			if (node != null && node.getCostEstimate() == 0) {
				queue.offer(node);
			}
		}

		while (!queue.isEmpty()) {
			Object object = queue.poll();
			if (object instanceof Node) {
				Node node = (Node) object;
				seenNodes.add(node);
				for (Connector connector : node.getIncomingConnectors()) {
					assert connector.getUnsatisfiedPreconditionCount() > 0;
					connector.setUnsatisfiedPreconditionCount(connector.getUnsatisfiedPreconditionCount() - 1);
					if (connector.getUnsatisfiedPreconditionCount() == 0) {
						queue.offer(connector);
					}
				}
			} else if (object instanceof Connector) {
				Connector connector = (Connector) object;
				Node parent = connector.getParent();
				if (!queue.contains(parent) && !seenNodes.contains(parent)) {
					queue.offer(parent);
				}
				double maxChildCost = Double.NEGATIVE_INFINITY;
				for (Node succ : connector.getChildren()) {
					double childCost = succ.getCostEstimate();
					if (childCost > maxChildCost) {
						maxChildCost = childCost;
					}
				}

				if (maxChildCost + connector.getCost() < parent.getCostEstimate()) {
					parent.setCostEstimate(maxChildCost + connector.getCost());
				}
			} else {
				assert false;
			}
		}
	}

	@SuppressWarnings("unlikely-arg-type")
	private void backwardRestriction() {
		unsafeAndProvenLabelling();

		// delete backward-unreachable nodes
		Set<Connector> removedConnectors = new LinkedHashSet<Connector>();

		HashSet<Node> removeTheseNodes = new HashSet<Node>();
		for (Node node : nodes.values()) {
			if (!node.isProven()) {
				removedConnectors.addAll(node.getOutgoingConnectors());
				removedConnectors.addAll(node.getIncomingConnectors());
				removeTheseNodes.add(node);
			}
		}
		nodes.remove(removeTheseNodes);

		// delete connectors pointing to them
		for (Node node : nodes.values()) {
			node.getIncomingConnectors().removeAll(removedConnectors);
			node.getOutgoingConnectors().removeAll(removedConnectors);
		}
	}

	private Set<Connector> computeBackwardReachableConnectors() {
		Set<Connector> connectorsAtCurrentDistance = new LinkedHashSet<Connector>();
		Set<Connector> backwardReachableConnectors = new LinkedHashSet<Connector>();

		Set<Node> goalNodes = new LinkedHashSet<Node>(); // store goal node during forward construction
		for (Node node : nodes.values()) {
			if (node.isGoalNode) {
				goalNodes.add(node);
				for (Connector c : node.getIncomingConnectors()) {
					if (!c.getParent().isGoalNode && c.isSafe) {
						connectorsAtCurrentDistance.add(c);
						backwardReachableConnectors.add(c);
					}
				}
			}

		}

		while (!connectorsAtCurrentDistance.isEmpty()) {
			Set<Connector> newlyFoundConnectors = new LinkedHashSet<Connector>();

			for (Connector c : connectorsAtCurrentDistance) {
				for (Connector d : c.getParent().getIncomingConnectors()) {
					if (!backwardReachableConnectors.contains(d) && !d.getParent().isGoalNode && d.isSafe) {
						newlyFoundConnectors.add(d);
						backwardReachableConnectors.add(d);
					}
				}
			}

			connectorsAtCurrentDistance = newlyFoundConnectors;
		}

		for (Node node : nodes.values()) {
			for (Connector c : node.getOutgoingConnectors()) {
				c.isSafe = backwardReachableConnectors.contains(c);
			}
		}

		return backwardReachableConnectors;
	}

	private int deleteUnprovenConnectors(Set<Connector> backwardReachableConnectors) {
		Set<Node> goalNodes = new LinkedHashSet<Node>();
		for (Node node : nodes.values()) {
			if (node.isGoalNode) {
				goalNodes.add(node);
			}
		}

		Queue<Connector> deleteQueue = new LinkedList<Connector>();
		Map<Node, Integer> counter = new LinkedHashMap<Node, Integer>();

		for (Connector c : backwardReachableConnectors) {
			if (counter.containsKey(c.getParent())) {
				counter.put(c.getParent(), counter.get(c.getParent()) + 1);
			} else {
				counter.put(c.getParent(), 1);
			}
		}

		for (Connector c : backwardReachableConnectors) {
			for (Node child : c.getChildren()) {
				if (!child.isGoalNode && !counter.containsKey(child) && !deleteQueue.contains(c)) {
					deleteQueue.add(c);
					break;
				}
			}
		}

		while (!deleteQueue.isEmpty()) {
			Connector c = deleteQueue.poll();
			c.isSafe = false;

			Node parent = c.getParent();
			if (counter.containsKey(parent)) {
				assert counter.get(parent) > 0;
				counter.put(parent, counter.get(parent) - 1);
				if (counter.get(parent) == 0) {
					for (Connector d : parent.getIncomingConnectors()) {
						if (d.isSafe && !deleteQueue.contains(d)) {
							deleteQueue.add(d);
						}
					}
				}
			} else {
				assert false;
			}
		}

		int num_safe = 0;

		for (Node node : nodes.values()) {
			for (Connector c : node.getOutgoingConnectors()) {
				if (c.isSafe) {
					num_safe++;
				}
			}
		}

		return num_safe;
	}

	/**
	 * Compute all forward reachable states, beginning with initial state and
	 * applying all applicable operators until a fix point is reached.
	 */
	private void forwardConstruction() {
		Queue<Node> queue = new LinkedList<Node>();
		if (DEBUG) {
			abstraction.dump();
		}
		for (State init : abstraction.getInitialState()) {
			lookupAndInsert(init, queue);
		}
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			if (DEBUG) {
				System.out.println("Process node " + node + " with "
						+ node.getState().getApplicableOps(abstraction.operators).size() + " applicable ops.");
			}
			for (Operator op : node.getState().getApplicableOps(abstraction.operators)) {
				Set<Node> children = new LinkedHashSet<Node>();
				Set<State> newStates = node.getState().apply(op);
				for (State state : newStates) {
					Node newNode = lookupAndInsert(state, queue);
					children.add(newNode);
				}
				new Connector(node, children, op);
			}
		}
		if (DEBUG) {
			System.out.println("Finished forward construction.");
		}
	}

	/**
	 * Generate Graphviz visualization.
	 */
	private void generateVisualizations() {

		System.out.println("VISUALIZING");

		String graphvizOutput = toStringGraphviz();
		String graphvizFilename = graphvizFilename();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(graphvizFilename));
			out.write(graphvizOutput);
			out.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			System.err.println("Current working directory:");
			System.err.println(System.getProperty("user.dir"));
		}
		try {
			Runtime.getRuntime().exec("dot -Tpng -o " + graphvizFilename + ".png " + graphvizFilename);
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	private String graphvizFilename() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("./out/abstraction");
		// TODO Repair graphviz output.
		/*
		 * for (int var : this.abstractionManager.pattern) { buffer.append("_");
		 * buffer.append(var); }
		 */
		buffer.append(".dot");
		return buffer.toString();
	}

	/**
	 * Lookup and insert given state in given queue.
	 *
	 * @param state
	 * @param queue
	 * @return node which corresponds to given state.
	 */
	Node lookupAndInsert(State state, Queue<Node> queue) {
		if (!nodes.containsKey(state.hashCode)) {
			Node n = new Node(state);
			nodes.put(state.hashCode, n);
			queue.offer(n);
		} else {
			assert nodes.get(state.hashCode).getState().equals(state);
		}
		return nodes.get(state.hashCode);
	}

	/**
	 * Perform value iteration.
	 */
	private void performValueIteration() {
		// All nodes that are left are backward-reachable
		// and hence should get an estimate of less than infinity:
		for (Node node : nodes.values()) {
			if (node != null) {
				node.setCostEstimate(0.0);
			}
		}

		// Auxiliary data structures.
		Map<Node, Double> oldCostEstimate = new HashMap<Node, Double>((int) (nodes.size() / 0.75) + 1);

		// Main loop.
		boolean converged = false;
		do {
			// Update.
			for (Node node : nodes.values()) {
				if (node != null) {
					oldCostEstimate.put(node, node.getCostEstimate());
					if (DEBUG) {
						System.out.println("Node " + node + " old cost estimate is " + oldCostEstimate.get(node));
					}
					if (!node.isGoalNode) {
						node.setCostEstimate(Node.UNINITIALIZED_COST_ESTIMATE);
					}
				} else {
					assert false;
				}
			}
			if (DEBUG) {
				System.out.println("Value iteration.");
			}
			for (Node node : nodes.values()) {
				if (DEBUG) {
					System.out.println("Process node " + node);
				}
				if (!node.isGoalNode) {
					if (DEBUG) {
						System.out.println("Node is no goal node and has " + node.getOutgoingConnectors().size()
								+ " outgoing connectors.");
					}
					for (Connector connector : node.getOutgoingConnectors()) {
						double connectorValueMax = Double.NEGATIVE_INFINITY;
						double connectorValueSum = 0.0;
						// double sumOfChildrenCardinality = 0.0;
						for (Node child : connector.getChildren()) {
							if (DEBUG) {
								System.out.println("child " + child);
							}
							double childEstimate = oldCostEstimate.get(child);
							if (childEstimate > connectorValueMax) {
								connectorValueMax = childEstimate;
							}
							// if (MyNDPlanner.weighBeliefStatesByCardinality && node.getState() instanceof
							// BeliefState) {
							// double numWorldStates = ((BeliefState)
							// child.getState()).getNumberOfWorldStates();
							// connectorValueSum += numWorldStates * childEstimate;
							// sumOfChildrenCardinality += numWorldStates;
							// } else {
							assert (connectorValueSum <= connectorValueSum + childEstimate);
							connectorValueSum += childEstimate;
							// }
						}

						double connectorValueAvg;
						// if (MyNDPlanner.weighBeliefStatesByCardinality && node.getState() instanceof
						// BeliefState) {
						// connectorValueAvg = connectorValueSum / sumOfChildrenCardinality;
						// } else {
						connectorValueAvg = connectorValueSum / connector.getChildren().size();
						// }

						boolean useMax = false; // Experiments show that it seems to be preferable to average
						// about child nodes.
						if (useMax) {
							if (connector.getCost() + connectorValueMax * DISCOUNT_FACTOR < node.getCostEstimate()) {
								node.setCostEstimate(connector.getCost() + connectorValueMax * DISCOUNT_FACTOR);
							}
						} else {
							if (connector.getCost() + connectorValueAvg < node.getCostEstimate()) {
								node.setCostEstimate(connector.getCost() + connectorValueAvg);
							}
						}
					}
				}
			}

			// convergence test
			converged = true;
			for (Node node : nodes.values()) {
				if (DEBUG) {
					System.out.println(node);
					System.out.println("oldestimate " + oldCostEstimate.get(node));
				}
				if (node != null && Math
						.abs(node.getCostEstimate() - oldCostEstimate.get(node)) > AbstractCostComputation.EPSILON) {
					converged = false;
					break;
				}
			}

		} while (!converged);
	}

	public Collection<Node> run() {
		forwardConstruction();
		boolean testConnectors = false; // For debugging.
		if (testConnectors) {
			assert Connector.consistencyTestForConnectors(nodes.values());
			printGraph("forwardgraph_" + callCounter + "_" + abstraction.pattern);
		}
		switch (Global.options.getSearchAlgorithm()) {
		default:
			// Strong cyclic planning.
			backwardRestriction();
			performValueIteration();
		}
		boolean visualize = false; // For debugging.
		if (visualize) {
			generateVisualizations();
		}
		assert (nodes.values().size() == new HashSet<Node>(nodes.values()).size()); // no duplicates
		// Delete states (BDDs) of nodes.
		if (!problem.isFullObservable && !Global.options.assumeFullObservabilityForPDBs()) {
			for (Node node : nodes.values()) {
				node.free();
			}
		}
		// TODO Delete abstracted operators (BDDs)?
		callCounter++;
		return nodes.values();
	}

	private String toStringGraphviz() {
		String goalStateStyle = " [style=filled, fillcolor=blue]";
		String provenStateStyle = " [style=filled, fillcolor=aquamarine]";
		StringBuffer buffer = new StringBuffer();
		buffer.append("digraph abstraction {\n");
		for (Node node : nodes.values()) {
			if (node != null) {
				String styleString = "";
				if (node.getState().isGoalState()) {
					styleString = goalStateStyle;
				} else if (node.isProven()) {
					styleString = provenStateStyle;
				}

				String nodeString = node.toString();
				nodeString = nodeString.replace(" ", "").replace(",", "").replace("<", "").replace(">", "").replace(":",
						"");
				nodeString = nodeString.substring(1, nodeString.length() - 1);

				buffer.append(
						"    node_" + nodeString + " [label=\"" + node.getCostEstimate() + "\"]" + styleString + ";\n");
			}
		}

		Map<Pair<Node, Node>, List<String>> edges = new LinkedHashMap<Pair<Node, Node>, List<String>>();
		for (Node node : nodes.values()) {
			if (node != null) {
				for (Connector conn : node.getOutgoingConnectors()) {
					for (Node child : conn.getChildren()) {
						if (!edges.containsKey(new Pair<Node, Node>(node, child))) {
							edges.put(new Pair<Node, Node>(node, child), new ArrayList<String>());
						}
						List<String> l = edges.get(new Pair<Node, Node>(node, child));
						if (!l.contains(conn.getName())) {
							l.add(conn.getName());
						}
					}
				}
			}
		}

		for (Map.Entry<Pair<Node, Node>, List<String>> edge : edges.entrySet()) {
			Pair<Node, Node> key = edge.getKey();
			List<String> value = edge.getValue();
			String from = key.first.toString();
			String to = key.second.toString();
			StringBuffer label = new StringBuffer();
			for (int i = 0; i < value.size(); i++) {
				label.append(value.get(i));
				if (i < value.size() - 1) {
					label.append(", ");
				}
			}

			String fromString = from.toString();
			fromString = fromString.replace(" ", "").replace(",", "").replace("<", "").replace(">", "").replace(":",
					"");
			fromString = fromString.substring(1, fromString.length() - 1);

			String toString = to.toString();
			toString = toString.replace(" ", "").replace(",", "").replace("<", "").replace(">", "").replace(":", "");
			toString = toString.substring(1, toString.length() - 1);

			buffer.append(
					"    node_" + fromString + " -> node_" + toString + " [label=\"" + label.toString() + "\"];\n");
		}
		buffer.append("}\n");
		return buffer.toString();
	}

	private void unsafeAndProvenLabelling() {
		int num_safe = 0;
		for (Node node : nodes.values()) {
			for (Connector c : node.getOutgoingConnectors()) {
				c.isSafe = true;
				num_safe++;
			}
		}
		if (DEBUG) {
			System.out.println("Number of connectors: " + num_safe);
		}
		int old_num_safe;

		int i = 0;
		do {
			old_num_safe = num_safe;

			// backward reachable connectors
			Set<Connector> backwardReachable = computeBackwardReachableConnectors();
			if (DEBUG) {
				printGraphAndMarkConnectors("backward_reachable_" + callCounter + "_" + i + "_" + abstraction.pattern,
						backwardReachable);
				System.out.println("number of backward reachable connectors: " + backwardReachable.size());
			}

			// unprovable connectors
			num_safe = deleteUnprovenConnectors(backwardReachable);
			if (DEBUG) {
				System.out.println("backward_reachable_" + callCounter + "_" + i + "_" + abstraction.pattern);
				System.out.println("num_safe: " + num_safe);
			}
			i++;
		} while (num_safe != old_num_safe);

		for (Node node : nodes.values()) {
			if (node != null) {
				for (Connector c : node.getOutgoingConnectors()) {
					if (c.isSafe) {
						node.setProven(true);
					}
				}
				if (node.isGoalNode) {
					node.setProven(true);
				}
			}
		}
	}

	public void printGraph(String filename) {
		printGraphAndMarkConnectors(filename, Collections.<Connector>emptySet());
	}

	public void printGraphAndMarkConnectors(String filename, Collection<Connector> connectors) {
		File graph = new File(filename + ".dot");
		try {
			FileWriter writer = new FileWriter(graph);
			writer.write(new GraphvizWriter().printGraphAsDot(nodes.values(), connectors));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
