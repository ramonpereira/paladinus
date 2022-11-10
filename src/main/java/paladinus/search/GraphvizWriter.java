package main.java.paladinus.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.problem.Problem;

/**
 * Generate an output of the whole search graph in GraphViz (dot) format.
 *
 * @author Robert Mattmueller
 * @author Ramon Fraga Pereira
 */
public class GraphvizWriter {

	/**
	 * Search manager
	 */
	HeuristicSearch search;

	/**
	 * The problem this GraphvizWriter operates on
	 */
	Problem problem;

	/**
	 * Create a new Graphviz writer for a given search manager instance.
	 *
	 * @param aostar The search manager instance
	 */
	public GraphvizWriter(HeuristicSearch search) {
		this.search = search;
		problem = search.getProblem();
	}

	/**
	 * Create a description of the current search graph in GraphViz (dot) format.
	 * Nodes are identified and printed by giving the underlying variable
	 * assignments and an indicator whether the node is currently marked as proven.
	 * For each SearchConnector and each child variable, an arc from the parent to the
	 * child is drawn, labelled with the protagonist operator to which the SearchConnector
	 * corresponds.
	 *
	 * @param complete True if the complete graph should be drawn, and false if only
	 *                 marked SearchConnectors should be followed
	 * @return A string containing the complete GraphViz (dot) description
	 */
	public String createOutputStateSpace(boolean complete) {
		List<SearchNode> seenNodes = new LinkedList<SearchNode>();
		List<SearchConnector> seenSearchConnectors = new LinkedList<SearchConnector>();
		Queue<SearchNode> queue = new LinkedList<SearchNode>();
		queue.offer(search.stateNodeMap.get(problem.getSingleInitialState().uniqueID));

		while (!queue.isEmpty()) {
			SearchNode node = queue.poll();
			seenNodes.add(node);

			Collection<SearchConnector> SearchConnectors = null;
			if (complete) {
				SearchConnectors = node.getOutgoingConnectors();
			} else {
				SearchConnectors = new ArrayList<SearchConnector>();
				if (node.getMarkedConnector() != null) {
					SearchConnectors.add(node.getMarkedConnector());
				}
			}

			for (SearchConnector SearchConnector : SearchConnectors) {
				seenSearchConnectors.add(SearchConnector);
				for (SearchNode next : SearchConnector.children) {
					if (!seenNodes.contains(next) && !queue.contains(next)) {
						queue.offer(next);
					}
				}
			}
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append("digraph {\n");

		for (SearchNode node : seenNodes) {
			buffer.append(node.index);
			buffer.append(" [ peripheries=\"1\", shape=\"rectangle\", ");
			if (node.isGoalNode()) {
				buffer.append("fontcolor=\"white\", style=\"filled\", fillcolor=\"blue\", ");
			} else {
				if (!node.isProven()) {
					if (node.isDeadEndNode()) {
						buffer.append("style=\"filled\", fillcolor=\"red\", ");
					} else if ((node.isDisproven() && node.isExpanded())) {
						buffer.append("style=\"filled,rounded\", fillcolor=\"red\", ");
					} else if (!node.isExpanded()) {
						buffer.append("style=\"filled\", fillcolor=\"yellow\", ");
					} else {
						buffer.append("style=\"rounded\", ");
					}
				} else {
					if (!node.isExpanded()) {
						buffer.append("style=\"filled\", fillcolor=\"green\", ");
					} else {
						buffer.append("style=\"filled,rounded\", fillcolor=\"green\", ");
					}
				}
			}
			buffer.append("label=\"");
			buffer.append("index: " + node.index + "\\n");
			buffer.append("depth: " + node.getDepth() + "\\n");
			buffer.append("h-Value: " + node.heuristic + "\\n");
			buffer.append("branching factor: " + node.getBranchingFactor() + "\\n");
			buffer.append("novelty: " + node.getBinaryNovelty() + "\\n");
			buffer.append("quantified novel: " + node.getQuantifiedNovel() + "\\n");
			if (problem.isFullObservable) {
				for (int i = 0; i < ((ExplicitState) node.state).size - 1; i++) {
					String tmp = problem.propositionNames.get(i)
							.get(((ExplicitState) node.state).variableValueAssignment.get(i));
					if (!tmp.startsWith("(not")) {
						buffer.append(tmp);
						buffer.append("\\n");
					}
				}
				buffer.append(problem.propositionNames.get(((ExplicitState) node.state).size - 1)
						.get(((ExplicitState) node.state).variableValueAssignment
								.get(((ExplicitState) node.state).size - 1)));
			}
			buffer.append("\" ]\n");
		}
		for (SearchConnector SearchConnector : seenSearchConnectors) {
			for (SearchNode next : SearchConnector.children) {
				buffer.append(SearchConnector.parent.index);
				buffer.append(" -> ");
				buffer.append(next.index);
				buffer.append(" [ label=\"");
				buffer.append(SearchConnector.operator.getName() + "\\n");
				buffer.append(SearchConnector.getEstimatedCost() + "\\n");
				buffer.append("\"");
				if (complete && SearchConnector.equals(SearchConnector.parent.getMarkedConnector()) && SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"red:blue\" ");
				} else if (complete && SearchConnector.equals(SearchConnector.parent.getMarkedConnector()) && !SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"red\" ");
				} else if (SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"blue\" ");
				}
				buffer.append(" ]\n");
			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}

	public static String createOutputTrace(Problem problem, SearchNode lastNode) {
		List<SearchNode> seenNodes = new LinkedList<SearchNode>();
		List<SearchConnector> seenSearchConnectors = new LinkedList<SearchConnector>();
		Queue<SearchNode> queue = new LinkedList<SearchNode>();
		queue.offer(lastNode);

		while (!queue.isEmpty()) {
			SearchNode node = queue.poll();
			seenNodes.add(node);

			// List<SearchConnector> SearchConnectors = new Linked;
			// if (complete) {
			// SearchConnectors = node.outgoingSearchConnectors;
			// }
			// else {
			// SearchConnectors = new ArrayList<SearchConnector>();
			// if (node.markedSearchConnector != null) {
			// SearchConnectors.add(node.markedSearchConnector);
			// }
			// }

			for (SearchConnector SearchConnector : node.getIncomingConnectors()) {
				seenSearchConnectors.add(SearchConnector);
				if (!seenNodes.contains(SearchConnector.parent) && !queue.contains(SearchConnector.parent)) {
					queue.offer(SearchConnector.parent);
				}
			}
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append("digraph {\n");

		Collections.reverse(seenNodes);
		for (SearchNode node : seenNodes) {
			buffer.append(node.index);
			buffer.append(" [ peripheries=\"1\", shape=\"rectangle\", ");
			if (node.isGoalNode()) {
				buffer.append("fontcolor=\"white\", style=\"filled\", fillcolor=\"blue\", ");
			} else {
				if (!node.isProven()) {
					if (node.isDeadEndNode()) {
						buffer.append("style=\"filled\", fillcolor=\"red\", ");
					} else if ((node.isDisproven() && node.isExpanded())) {
						buffer.append("style=\"filled,rounded\", fillcolor=\"red\", ");
					} else if (!node.isExpanded()) {
						buffer.append("style=\"filled\", fillcolor=\"yellow\", ");
					} else {
						buffer.append("style=\"rounded\", ");
					}
				} else {
					if (!node.isExpanded()) {
						buffer.append("style=\"filled\", fillcolor=\"green\", ");
					} else {
						buffer.append("style=\"filled,rounded\", fillcolor=\"green\", ");
					}
				}
			}
			buffer.append("label=\"");
			buffer.append("index: " + node.index + "\\n");
			buffer.append("h-Valeu: " + node.heuristic + "\\n");
			if (problem.isFullObservable) {
				for (int i = 0; i < ((ExplicitState) node.state).size - 1; i++) {
					String tmp = problem.propositionNames.get(i)
							.get(((ExplicitState) node.state).variableValueAssignment.get(i));
					if (!tmp.startsWith("(not")) {
						buffer.append(tmp);
						buffer.append("\\n");
					}
				}
				buffer.append(problem.propositionNames.get(((ExplicitState) node.state).size - 1)
						.get(((ExplicitState) node.state).variableValueAssignment
								.get(((ExplicitState) node.state).size - 1)));
			}
			buffer.append("\" ]\n");
		}
		for (SearchConnector SearchConnector : seenSearchConnectors) {
			for (SearchNode next : SearchConnector.children) {
				buffer.append(SearchConnector.parent.index);
				buffer.append(" -> ");
				buffer.append(next.index);
				buffer.append(" [ label=\"");
				buffer.append(SearchConnector.operator.getName());
				buffer.append("\"");
				if (SearchConnector.equals(SearchConnector.parent.getMarkedConnector()) && SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"red:blue\" ");
				} else if (SearchConnector.equals(SearchConnector.parent.getMarkedConnector()) && !SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"red\" ");
				} else if (SearchConnector.isSafe) {
					buffer.append(", style=\"bold\", color=\"blue\" ");
				}
				buffer.append(" ]\n");
			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}

	public static void dumpGraph(String filename, String graph) {
		File f = new File(filename + ".dot");
		try {
			FileWriter writer = new FileWriter(f);
			writer.write(graph);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
