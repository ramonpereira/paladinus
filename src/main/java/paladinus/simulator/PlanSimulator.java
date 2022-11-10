package main.java.paladinus.simulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import main.java.paladinus.PaladinusPlanner;
import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.search.policy.Policy;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;
import main.java.paladinus.symbolic.BeliefState;

/**
 *
 * @author Robert Mattmueller
 *
 */
class Connector {

	Node parent;
	Set<Node> children;
	String name;
	int unsatisfiedPreconditionCount;
	double cost;
	boolean isSafe = false;

	public Connector(Node parent, Set<Node> children, String name, double cost) {
		this.parent = parent;
		this.children = children;
		parent.outgoingConnectors.add(this);
		for (Node child : children) {
			child.incomingConnectors.add(this);
		}
		this.name = name;
		this.cost = cost;
		unsatisfiedPreconditionCount = children.size();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Connector)) {
			return false;
		}
		Connector c = (Connector) o;
		return parent.equals(c.parent) && children.equals(c.children);
	}

	@Override
	public int hashCode() {
		return parent.hashCode() + children.hashCode();
	}

}

class Node {

	static final double UNINITIALIZED_COST_ESTIMATE = Double.POSITIVE_INFINITY;

	static int nextIndex = 0;

	State state;
	int index;
	double costEstimate;
	Set<Connector> incomingConnectors;
	Set<Connector> outgoingConnectors;
	boolean isProven = false;

	public Node(State state) {
		this.state = state;
		index = nextIndex++;
		costEstimate = Node.UNINITIALIZED_COST_ESTIMATE;
		if (state.isGoalState()) {
			costEstimate = 0.0;
		}
		incomingConnectors = new LinkedHashSet<Connector>();
		outgoingConnectors = new LinkedHashSet<Connector>();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Node)) {
			return false;
		}
		return index == ((Node) o).index;
	}

	@Override
	public int hashCode() {
		return index;
	}

	/**
	 * Resets the index to 0. Necessary for running the planner more than one time
	 * in one program.
	 */
	static void resetIndex() {
		nextIndex = 0;
	}
}

public class PlanSimulator {
	protected final Problem problem;

	public static final double EPSILON = 0.0001;
	
	public static List<Integer> goalNodes = new ArrayList<>();

	public PlanSimulator(Problem problem) {
		this.problem = problem;
	}

	private static Map<State, Node> buildStateSpace(Problem problem, Policy policy) {
		// build state space
		Map<State, Node> seenStates = new LinkedHashMap<State, Node>();
		Queue<Node> queue = new LinkedList<Node>();
		Node initial = new Node(problem.getSingleInitialState());
		seenStates.put(problem.getSingleInitialState(), initial);
		queue.add(initial);

		while (!queue.isEmpty()) {
			Node node = queue.poll();
			Set<Node> children = new LinkedHashSet<Node>();

			assert node != null;
			assert node.state != null;
			Operator op = policy.getOperator(node.state);
			Set<State> successorStates = node.state.apply(op);
			for (State successor : successorStates) {
				Node newNode = null;
				if (seenStates.containsKey(successor)) {
					newNode = seenStates.get(successor);
				} else {
					newNode = new Node(successor);
					seenStates.put(successor, newNode);
					if (!successor.isGoalState()) {
						queue.offer(newNode);
					}
				}
				children.add(newNode);
			}
			new Connector(node, children, op.getName(), op.getCost());
		}

		Node.resetIndex();
		return seenStates;
	}

	public double performValueIteration(Policy policy) {
		Map<State, Node> seenStates = buildStateSpace(problem, policy);
		Collection<Node> nodes = seenStates.values();

		// initialize cost estimates
		for (Node node : nodes) {
			if (node != null) {
				node.costEstimate = 0.0;
			}
		}

		// auxiliary data structures
		double[] oldCostEstimate = new double[nodes.size()];

		// main loop
		boolean converged = false;
		do {
			// update
			for (Node node : nodes) {
				if (node != null) {
					oldCostEstimate[node.index] = node.costEstimate;
					if (!node.state.isGoalState()) {
						node.costEstimate = Node.UNINITIALIZED_COST_ESTIMATE;
					}
				}
			}
			for (Node node : nodes) {
				if (node != null && !node.state.isGoalState()) {
					for (Connector connector : node.outgoingConnectors) {
						double connectorValueMax = Double.NEGATIVE_INFINITY;
						double connectorValueSum = 0.0;
						for (Node child : connector.children) {
							double childEstimate = oldCostEstimate[child.index];
							if (childEstimate > connectorValueMax) {
								connectorValueMax = childEstimate;
							}
							connectorValueSum += childEstimate;
						}
						double connectorValueAvg = connectorValueSum / connector.children.size();
						double connectorValue = 0.0;

						boolean useMax = false;
						if (useMax) {
							connectorValue = connectorValueMax;
						} else {
							connectorValue = connectorValueAvg;
						}

						if (connector.cost + connectorValue < node.costEstimate) {
							node.costEstimate = connector.cost + connectorValue;
						}
					}
				}
			}

			// convergence test
			converged = true;
			for (Node node : nodes) {
				if (node != null && Math.abs(node.costEstimate - oldCostEstimate[node.index]) > EPSILON) {
					converged = false;
					break;
				}
			}
		} while (!converged);

		return seenStates.get(problem.getSingleInitialState()).costEstimate;
	}

	public static void savePlanAsDot(Problem problem, Policy policy, String filename) {
		if (filename.trim().equals("")) {
			filename = PaladinusPlanner.getNameOfProblemInstance() + ".dot";
		}
		Collection<Node> nodes = buildStateSpace(problem, policy).values();
		LinkedList<Connector> seenConnectors = new LinkedList<Connector>();
		Node initialNode = null;
		LinkedList<Node> goals = new LinkedList<Node>();
		StringBuffer buffer = new StringBuffer();
		buffer.append("digraph G {\n");
		for (Node node : nodes) {
			String label = "";
			if (problem.isFullObservable) {
				label = ((ExplicitState) node.state).toStringWithPropositionNames();
			} else {
				label = ((int) ((BeliefState) node.state).getNumberOfWorldStates()) + "";
			}
			buffer.append(String.format("%d [label=\"%s\"];\n", node.index, label));
			if (node.state.isGoalState()) {
				goals.add(node);
			}
			if (node.state.equals(problem.getSingleInitialState())) {
				initialNode = node;
			}
			seenConnectors.addAll(node.outgoingConnectors);
		}

		for (Connector connector : seenConnectors) {
			// random color
			String[] letters = "0123456789ABCDEF".split("");
			String color = "#";
			for (int i = 0; i < 6; i++) {
				color += letters[(int) Math.round(Math.random() * 15)];
			}

			for (Node next : connector.children) {
				buffer.append(String.format("%d -> %d [label=\"%s\", color=\"%s\", fontcolor=\"%s\"];\n",
						connector.parent.index, next.index, connector.name, color, color));
			}
		}

		for (Node goal : goals) {
			PlanSimulator.goalNodes.add(goal.index);
			buffer.append(String.format("subgraph cluster_goal_%d {\n" + "style=filled;\n" + "color=palegreen;\n"
					+ "%s;\n" + "label=\"GOAL\";\n" + "}\n", goal.index, goal.index));
		}

		buffer.append(String.format("subgraph cluster_init {\n" + "style=filled;\n" + "color=lightblue;\n" + "%s;\n"
				+ "label=\"INIT\";\n" + "}\n", initialNode.index));

		buffer.append("}\n");

		File plan = new File(filename);
		try {
			FileWriter writer = new FileWriter(plan);
			writer.write(buffer.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
