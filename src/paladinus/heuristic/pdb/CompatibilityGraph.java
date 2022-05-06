package paladinus.heuristic.pdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import paladinus.problem.Problem;
import paladinus.state.Operator;

/**
 * @author Manuela Ortlieb
 */
public class CompatibilityGraph {

	private Problem problem;

	private HashSet<Integer> vertices;

	/**
	 * Index of pattern to set of indices of adjacent patterns.
	 */
	private HashMap<Integer, HashSet<Integer>> neighbors;

	private ArrayList<Set<Integer>> patternCollection;

	private boolean[][] additiveVariablePairs;

	private HashSet<HashSet<Integer>> maxCliques;

	private HashSet<Integer> Q;

	/**
	 * The Constructor of the Compatibility Graph.
	 *
	 * @param patternCollection
	 */
	public CompatibilityGraph(Problem problem, Set<Set<Integer>> patternCollection) {
		this.problem = problem;
		int numberOfPatterns = patternCollection.size();
		int capacity = (int) (numberOfPatterns / 0.75) + 1;
		vertices = new HashSet<Integer>(capacity); // One vertex for each pattern.
		neighbors = new HashMap<Integer, HashSet<Integer>>(capacity);
		this.patternCollection = new ArrayList<Set<Integer>>(patternCollection); // We need an index for
		// each pattern.
		maxCliques = new HashSet<HashSet<Integer>>(30);
		computeAdditiveVars();
		buildCGraph();
	}

	/**
	 * Extend the graph permanently by a new pattern. Maximal cliques have to be
	 * recomputed after extension.
	 *
	 * @param newPattern
	 */
	public void extendCompatibilityGraph(Set<Integer> newPattern) {
		patternCollection.add(newPattern);
		maxCliques = new HashSet<HashSet<Integer>>(30);
		// add new pattern/vertex to the graph
		int newVertex = patternCollection.size() - 1;
		boolean notContained = vertices.add(newVertex);
		assert (notContained);
		neighbors.put(newVertex, new HashSet<Integer>());
		for (int i = 0; i < vertices.size() - 1; i++) {
			if (arePatternsAdditive(patternCollection.get(i), patternCollection.get(newVertex))) {
				neighbors.get(i).add(newVertex);
				neighbors.get(newVertex).add(i);
			}
		}
		computeMaximalCliques();
	}

	/**
	 * Precomputation of additive variables.
	 */
	public void computeAdditiveVars() {
		assert additiveVariablePairs == null;
		additiveVariablePairs = new boolean[problem.numStateVars][problem.numStateVars];
		for (boolean[] row : additiveVariablePairs) {
			Arrays.fill(row, true);
		}
		for (Operator op : problem.getOperators()) { // TODO Check this! Only causative actions??
			Integer[] effectVars = op.getAffectedVariables().toArray(new Integer[op.getAffectedVariables().size()]);
			for (int j = 0; j < effectVars.length; j++) {
				for (int k = 0; k < effectVars.length; k++) {
					additiveVariablePairs[effectVars[j]][effectVars[k]] = false;
				}
			}
		}
	}

	public Set<Set<Set<Integer>>> getMaxAdditiveSubsets(Set<Integer> newPattern) {
		// newPattern is not part of the compatibility graph
		assert (!patternCollection.contains(newPattern));
		Set<Set<Set<Integer>>> maxAdditiveSubsets = new HashSet<Set<Set<Integer>>>();
		assert (!maxCliques.isEmpty());
		for (HashSet<Integer> clique : maxCliques) {
			HashSet<Set<Integer>> subset = new HashSet<Set<Integer>>();
			// take all patterns which are additive to the new pattern
			for (Integer index : clique) {
				if (arePatternsAdditive(patternCollection.get(index), newPattern)) {
					subset.add(patternCollection.get(index));
				}
			}
			if (!subset.isEmpty()) {
				maxAdditiveSubsets.add(subset);
			}
		}
		if (maxAdditiveSubsets.isEmpty()) {
			maxAdditiveSubsets.add(new HashSet<Set<Integer>>()); // empty set
		}
		return maxAdditiveSubsets;
	}

	/**
	 * Builds the compatibility graph using a pattern collection
	 */
	private void buildCGraph() {
		// add all patterns as vertices to the compatibility graph
		for (int i = 0; i < patternCollection.size(); i++) {
			vertices.add(i);
			neighbors.put(i, new HashSet<Integer>());
		}
		// add edges between additive vertices (patterns)
		for (int i = 0; i < patternCollection.size(); i++) {
			for (int j = i + 1; j < patternCollection.size(); j++) {
				if (arePatternsAdditive(patternCollection.get(i), patternCollection.get(j))) {
					neighbors.get(i).add(j);
					neighbors.get(j).add(i);
				}
			}
		}
		computeMaximalCliques();
	}

	/**
	 * Computes if two patterns are additive.
	 */
	// TODO Check if it makes sense to cache results.
	public boolean arePatternsAdditive(Set<Integer> patternA, Set<Integer> patternB) {
		for (Integer varA : patternA) {
			for (Integer varB : patternB) {
				if (!additiveVariablePairs[varA][varB]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Determines all maximal cliques using the compatibility graph
	 */
	private void computeMaximalCliques() {
		if (maxCliques.isEmpty()) {
			Q = new HashSet<Integer>();
			expand(new HashSet<Integer>(vertices), new HashSet<Integer>(vertices));
		}
		// System.out.println(maxCliques);
	}

	/**
	 * Implementation of the "Maximal Cliques" algorithm of Etsuji Tomita, Akira
	 * Tanaka and Haruhisa Takahashi
	 */
	private void expand(HashSet<Integer> subg, HashSet<Integer> cand) {
		if (subg.isEmpty()) {
			maxCliques.add(new HashSet<Integer>(Q));
		} else {
			int u = vertexWithMaxSucc(subg, cand);
			Set<Integer> extU = new HashSet<Integer>();
			extU.addAll(cand);
			extU.removeAll(neighbors.get(u));
			while (extU.iterator().hasNext()) {
				Integer q = extU.iterator().next();
				Q.add(q);
				HashSet<Integer> subgq = new HashSet<Integer>();
				subgq.addAll(subg);
				subgq.retainAll(neighbors.get(q));
				HashSet<Integer> candq = new HashSet<Integer>();
				candq.addAll(cand);
				candq.retainAll(neighbors.get(q));
				expand(subgq, candq);
				cand.remove(q);
				Q.remove(q);
				extU.remove(q);
			}
		}
	}

	public Set<Set<Set<Integer>>> getMaximalCliques() {
		assert (!maxCliques.isEmpty());
		Set<Set<Set<Integer>>> cliques = new HashSet<Set<Set<Integer>>>((int) (maxCliques.size() / 0.75) + 1);
		for (HashSet<Integer> clique : maxCliques) {
			Set<Set<Integer>> patterns = new HashSet<Set<Integer>>((int) (clique.size() / 0.75) + 1);
			for (Integer index : clique) {
				patterns.add(patternCollection.get(index));
			}
			cliques.add(patterns);
		}
		return cliques;
	}

	/**
	 * Returns the Vertex in the subgraph which is connected with a maximal number
	 * of other vertices in candidates.
	 *
	 * @param cand
	 * @return
	 */
	private int vertexWithMaxSucc(HashSet<Integer> subg, HashSet<Integer> cand) {
		int maxSucc = -1;
		int maxVertex = -1;
		for (Integer u : subg) {
			HashSet<Integer> neighbours = neighbors.get(u);
			neighbours.retainAll(cand);
			if (neighbours.size() > maxSucc) {
				maxVertex = u;
				maxSucc = neighbours.size();
			}
		}
		assert (maxVertex > -1);
		return maxVertex;
	}
}
