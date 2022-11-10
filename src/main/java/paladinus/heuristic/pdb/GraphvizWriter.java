package main.java.paladinus.heuristic.pdb;

import java.util.Collection;
import java.util.Collections;

import main.java.paladinus.heuristic.graph.Connector;
import main.java.paladinus.heuristic.graph.Node;

/**
 *
 * @author Robert Mattmueller
 *
 */

public class GraphvizWriter {

	public String printGraphAsDot(Collection<Node> nodes) {
		return printGraphAsDot(nodes, Collections.<Connector>emptySet());
	}

	public String printGraphAsDot(Collection<Node> nodes, Collection<Connector> connectorsToMark) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("digraph {\n");
		// Print nodes.
		for (Node node : nodes) {
			buffer.append(node.index);
			buffer.append(" [ peripheries=\"1\", shape=\"rectangle\", label=\"");
			buffer.append(node.toString());
			buffer.append("\", ");
			if (node.isGoalNode) {
				buffer.append("style=\"filled\", fillcolor=\"blue\", fontcolor=\"white\"");
			}
			// Dead end.
			else if (node.getOutgoingConnectors().size() == 0) {
				buffer.append("style=\"filled\", fillcolor=\"red\"");
			}
			// Self-loop dead end.
			else if (node.getOutgoingConnectors().size() == 1) {
				Connector c = node.getOutgoingConnectors().iterator().next();
				if (c.getChildren().size() == 1 && c.getChildren().iterator().next().equals(node)) {
					buffer.append("style=\"filled\", fillcolor=\"red\"");
				}
			}
			buffer.append("];\n");
		}

		// Print edges.
		for (Node node : nodes) {
			for (String name : node.getOutgoingConnectorsWithNames().keySet()) {
				Connector c = node.getOutgoingConnectorsWithNames().get(name);
				for (Node child : c.getChildren()) {
					buffer.append(c.getParent().index);
					buffer.append(" -> ");
					buffer.append(child.index);
					buffer.append(" [ label=\"");
					buffer.append(name.replace("_abs", ""));
					buffer.append("\",");
					if (connectorsToMark.contains(c)) {
						buffer.append("style=\"bold\", color=\"purple3\"");
					}
					buffer.append(" ];\n");
				}
			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}

}
