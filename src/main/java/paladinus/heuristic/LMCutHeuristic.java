package paladinus.heuristic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import paladinus.explicit.ExplicitCondition;
import paladinus.explicit.ExplicitOperator;
import paladinus.explicit.ExplicitState;
import paladinus.heuristic.FFHeuristic.RPGStrategy;
import paladinus.problem.Problem;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.util.Pair;

/**
 * LM-Cut Heuristic
 *
 * @author Josua Scherzinger
 */
public class LMCutHeuristic extends Heuristic {

	/**
	 * Heuristic class to calculate the h-max values.
	 */
	private FFHeuristic hMaxHeuristic;

	/**
	 * Map used to evaluate justification graph backwards.
	 */
	private Map<FFProposition, Set<Pair<FFRule, FFProposition>>> graphInverted;

	/**
	 * Map used to evaluate justification graph forwards.
	 */
	private Map<FFProposition, Set<Pair<FFRule, FFProposition>>> graph;

	/**
	 * Current heuristic value.
	 */
	private double lmCutValue;

	/**
	 * Stores the original operator costs.
	 */
	private Map<Operator, Double> originalCosts;

	/**
	 * Represents the "s" node in the justification graph.
	 */
	private FFProposition initProp = new FFProposition(-1, 1);

	/**
	 * Iteration number for debugging.
	 */
	private int iteration = 0;

	/**
	 * Set DEBUG true for more outputs.
	 */
	public static boolean DEBUG = false;

	/**
	 * Export dot file of the justification graph for debugging.
	 */
	public static boolean EXPORT_DOT = false;

	public LMCutHeuristic(Problem problem) {
		this(problem, problem.explicitGoal);
	}

	/**
	 * Create a new LM-cut heuristic.
	 */
	public LMCutHeuristic(Problem problem, ExplicitCondition goal) {
		super(problem, true); // TODO Support axioms correctly.
		// for (OperatorRule rule : problem.axioms) {
		// System.out.println("rule " + rule);
		// }
		// problem.getExplicitInitialStates().get(0).dump();
		originalCosts = new HashMap<Operator, Double>(problem.getOperators().size());
		for (Operator op : problem.getOperators()) {
			if (op.isCausative) {
				originalCosts.put(op, op.getCost());
			}
		}
		hMaxHeuristic = new FFHeuristic(problem, RPGStrategy.MAX, goal);
	}

	/**
	 * Compute heuristic value for given state.
	 *
	 * @param state state to be evaluated by heuristic
	 * @return heuristic value for given state
	 */
	@Override
	public double getHeuristic(State state) {
		lmCutValue = 0;
		double result = -1;

		if (state instanceof ExplicitState) {
			ExplicitState explicitState = (ExplicitState) state;
			while (result == -1) {
				double hmax = hMaxHeuristic.getHeuristic(state);
				if (DEBUG) {
					System.out.println("h max value is " + hmax);
				}
				assert hmax >= 0;

				// Stop if h-max value of the goal proposition reaches 0 or infinity.
				if (hmax == INFINITE_HEURISTIC) {
					assert hMaxHeuristic.goalProp.reachCost == FFProposition.INFINITE_REACH_COST;
					result = hmax;
				} else if (hmax == 0) {
					assert hMaxHeuristic.goalProp.reachCost == 0;
					result = lmCutValue;
					assert result >= 0;
					if (DEBUG) {
						System.out.println("result is " + result);
					}
				} else {
					assert hMaxHeuristic.goalProp.reachCost > 0
							: "Reach cost of the goal is " + hMaxHeuristic.goalProp.reachCost;

					// Initialize justification graph by adding a "s" node and edges
					// to the true propositions of the current state.
					initializeJustificationGraph(explicitState);

					// Select the propositions with the highest h-max value.
					choosePreconditions();

					// Search for cut. If no cut is found, raise an error.
					computeBackwardSearch();
					boolean okay = computeForwardCutSearch((ExplicitState) state);
					if (!okay)
						return hmax;

					if (EXPORT_DOT) {
						dumpGraph("lmcut-graph_" + iteration++, state.toString());
					}
					resetJustificationGraph();
				}
			}
			restoreCosts();
			return result;
		}
		return result;
	}

	/**
	 * Initialize justification graph by adding edges from the initial node to the
	 * true proposition of the given state.
	 *
	 * @param state current state to evaluate
	 */
	private void initializeJustificationGraph(ExplicitState state) {
		if (graph == null) {
			assert graphInverted == null;

			// Create map with all propositions as keys
			graph = new HashMap<FFProposition, Set<Pair<FFRule, FFProposition>>>();
			graphInverted = new HashMap<FFProposition, Set<Pair<FFRule, FFProposition>>>();

			for (int var = 0; var < problem.numStateVars; var++) {
				for (int val = 0; val < problem.domainSizes.get(var); val++) {
					FFProposition prop = hMaxHeuristic.getProposition(var, val);
					graph.put(prop, new HashSet<Pair<FFRule, FFProposition>>());
					graphInverted.put(prop, new HashSet<Pair<FFRule, FFProposition>>());
				}
			}
			graph.put(hMaxHeuristic.goalProp, new HashSet<Pair<FFRule, FFProposition>>());
			graphInverted.put(hMaxHeuristic.goalProp, new HashSet<Pair<FFRule, FFProposition>>());

			// add initial proposition
			graph.put(initProp, new HashSet<Pair<FFRule, FFProposition>>());
			graphInverted.put(initProp, new HashSet<Pair<FFRule, FFProposition>>());
		}

		// Add edges from "s" to propositions which are initially true.
		HashSet<Pair<FFRule, FFProposition>> edges = new HashSet<Pair<FFRule, FFProposition>>();
		ExplicitOperator op = new ExplicitOperator("", null, null, Collections.<Pair<Integer, Integer>>emptySet(),
				false, 0);
		for (Entry<Integer, Integer> varVal : state.variableValueAssignment.entrySet()) {
			FFProposition prop = hMaxHeuristic.getProposition(varVal.getKey(), varVal.getValue());
			FFRule rule = new FFRule(new HashSet<FFProposition>(Arrays.asList(initProp)), prop, op);
			edges.add(new Pair<FFRule, FFProposition>(rule, prop));

			HashSet<Pair<FFRule, FFProposition>> edgesBackward = new HashSet<Pair<FFRule, FFProposition>>();
			edgesBackward.add(new Pair<FFRule, FFProposition>(rule, initProp));
			graphInverted.put(prop, new HashSet<Pair<FFRule, FFProposition>>());
		}
		graph.put(initProp, edges);
	}

	/**
	 * Select precondition of every rule that generates the highest h-max value and
	 * create the corresponding edge in the justification graph
	 *
	 * @param state state to be evaluated by heuristic
	 */
	private void choosePreconditions() {
		if (DEBUG) {
			System.out.println("Choose preconditions.");
		}
		for (FFRule rule : hMaxHeuristic.rules) {
			double max = Double.NEGATIVE_INFINITY;
			FFProposition maxProp = null;

			// Check which proposition of the precondition generates the highest h-max
			// value.
			for (FFProposition proposition : rule.body) {
				assert proposition.reachCost >= -1;

				// Remember best option.
				if (proposition.reachCost >= max) {
					max = proposition.reachCost;
					maxProp = proposition;
				}
			}

//      List<FFProposition> maxProps = new ArrayList<FFProposition>();
//      for (FFProposition proposition : rule.body) {
//    	  if (proposition.reachCost == max)
//    		  maxProps.add(proposition);
//      }
//      Random r = new Random();
//      int i = r.nextInt(maxProps.size());
//      //System.out.println(maxProps);
//      maxProp = maxProps.get(i);
//      //System.out.println();

			if (maxProp == null) { // No precondition.
				maxProp = initProp;
			}
			if (DEBUG) {
				System.out.println("For rule " + rule + " maxProp is " + maxProp + " with cost " + max);
			}

			// Create an edge in the justification graph and
			// store edges in both directions for simpler s-t-cut computation
			Pair<FFRule, FFProposition> edgeInverted = new Pair<FFRule, FFProposition>(rule, maxProp);
			Pair<FFRule, FFProposition> edge = new Pair<FFRule, FFProposition>(rule, rule.head);
			assert edge != null;
			assert edgeInverted != null;
			graphInverted.get(rule.head).add(edgeInverted);
			graph.get(maxProp).add(edge);
		}
	}

	/**
	 * Do a backward search in the justification graph to mark all propositions that
	 * can be reached backwards by the goalProp using only 0-cost operators.
	 */
	private void computeBackwardSearch() {
		Stack<FFProposition> marked = new Stack<FFProposition>();
		hMaxHeuristic.goalProp.markedBackwards = true;
		marked.push(hMaxHeuristic.goalProp);

		// Mark all propositions that can be reached backwards from the goalProp with
		// 0-cost operators.
		while (!marked.empty()) {
			FFProposition current = marked.pop();
			for (Pair<FFRule, FFProposition> edge : graphInverted.get(current)) {
				if (edge.first.operator.getCost() == 0 && !edge.second.markedBackwards) {
					edge.second.markedBackwards = true;
					marked.push(edge.second);
					if (DEBUG) {
						System.out.println("marked backwards " + edge.second);
					}
				}
			}
		}
	}

	/**
	 * Compute a forward search until you reach the cut. Decrease operator costs on
	 * the cut and increase heuristic value.
	 *
	 * @param state current state
	 * @return true iff cut was found
	 */
	private boolean computeForwardCutSearch(ExplicitState state) {
		Stack<FFProposition> marked = new Stack<FFProposition>();
		Set<ExplicitOperator> cut = new HashSet<ExplicitOperator>();

		assert initProp.markedBackwards == false;
		initProp.markedForwards = true;
		marked.push(initProp);

		// Search until backwards marked nodes are reached that indicate a cut.
		while (!marked.empty()) {
			FFProposition current = marked.pop();
			for (Pair<FFRule, FFProposition> edge : graph.get(current)) {
				if (!edge.second.markedForwards) {
					if (edge.second.markedBackwards) {
						if (edge.first.operator == null || edge.first.operator.getName().equals(""))
							continue;

						cut.add(edge.first.operator);
						if (DEBUG) {
							System.out.println("cut " + edge.first.operator);
						}
					} else {
						edge.second.markedForwards = true;
						if (DEBUG) {
							System.out.println("marked forward " + edge.second);
						}
						marked.push(edge.second);
					}
				}
			}
		}

		// Raise an error if no cut was found.
		/*
		 * if (cut.isEmpty()) { new
		 * Exception("Impossible. No cut found, even though h-max value is not infinity."
		 * ).printStackTrace(); Global.ExitCode.EXIT_CRITICAL_ERROR.exit(); }
		 */

		if (cut.isEmpty()) {
			lmCutValue += 1;
			return false;
		}

		// Set costs of operators on the cut to 0.
		double minCost = Double.POSITIVE_INFINITY;
		for (ExplicitOperator operator : cut) {
			if (operator.getCost() < minCost) {
				minCost = operator.getCost();
			}
			operator.setCost(0);
		}
		// Increase heuristic value.
		assert minCost >= 0;
		assert minCost < Double.POSITIVE_INFINITY;
		lmCutValue += minCost;
		return true;
	}

	/**
	 * Remove all edges and reset markings.
	 */
	private void resetJustificationGraph() {
		assert graph != null;
		for (FFProposition prop : graph.keySet()) {
			prop.markedBackwards = false;
			prop.markedForwards = false;
			graph.get(prop).clear(); // delete edges
			graphInverted.get(prop).clear(); // delete edges
		}
	}

	/**
	 * Revert cots of all operators to their original value.
	 */
	private void restoreCosts() {
		for (Operator op : originalCosts.keySet()) {
			op.setCost(originalCosts.get(op));
		}
	}

	/**
	 * Dump justification graph.
	 *
	 * @param filename
	 * @param state
	 */
	public void dumpGraph(String filename, String state) {
		File graph = new File(filename + ".dot");
		try {
			FileWriter writer = new FileWriter(graph);
			writer.write(createOutput(state));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a Graphviz representation of the justification graph.
	 *
	 * @param state
	 * @return
	 */
	private String createOutput(String state) {
		Map<FFProposition, Integer> propToIdx = new HashMap<FFProposition, Integer>();
		StringBuffer buffer = new StringBuffer();
		buffer.append("digraph {\n");
		buffer.append("label=\"" + state + "\";\n");
		buffer.append("labelloc=top;\n");
		buffer.append("labeljust=left;\n");
		int c = 0;
		for (FFProposition prop : graph.keySet()) {
			Integer v = propToIdx.put(prop, c);
			assert v == null;
			buffer.append(c++);
			buffer.append(" [ peripheries=\"1\", shape=\"circle\", ");
			if (prop == hMaxHeuristic.goalProp) {
				buffer.append("fontcolor=\"white\", style=\"filled\", fillcolor=\"blue\", ");
			} else if (prop == initProp) {
				buffer.append("fontcolor=\"white\", style=\"filled\", fillcolor=\"black\", ");
			} else if (prop.markedBackwards) {
				assert !prop.markedForwards;
				buffer.append("fontcolor=\"black\", style=\"filled\", fillcolor=\"deepskyblue\", ");
			} else if (prop.markedForwards) {
				buffer.append("fontcolor=\"black\", style=\"filled\", fillcolor=\"orange\", ");
			} else {
				buffer.append("fontcolor=\"white\", style=\"filled\", fillcolor=\"red\", ");
			}
			buffer.append("label=\"");
			if (prop == hMaxHeuristic.goalProp) {
				buffer.append("T");
			} else if (prop == initProp) {
				buffer.append("S");
			} else {
				buffer.append(prop);
			}
			buffer.append("\\n");
			buffer.append("\" ]\n");
		}
		HashSet<FFRule> seenRules = new HashSet<FFRule>();
		for (FFProposition current : graph.keySet()) {
			for (Pair<FFRule, FFProposition> edge : graph.get(current)) {
				assert !seenRules.contains(edge.first);
				buffer.append(propToIdx.get(current));
				buffer.append(" -> ");
				buffer.append(propToIdx.get(edge.second));
				buffer.append(" [ label=\"");
				buffer.append(edge.first.operator);
				buffer.append("\"");
				buffer.append(" ]\n");
				seenRules.add(edge.first);

			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}
}
