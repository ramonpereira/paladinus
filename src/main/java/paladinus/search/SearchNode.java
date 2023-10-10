package paladinus.search;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import paladinus.explicit.ExplicitOperator;
import paladinus.explicit.ExplicitState;
import paladinus.problem.Problem;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.util.Pair;

/**
 * 
 * @author Ramon Fraga Pereira
 *
 */
public class SearchNode extends AbstractNode implements Comparable<SearchNode> {
	
	protected double heuristic;
	
	protected double heuristicTieBreak;

	protected SearchNode parent;

	private int depth = -1;
	
	private int branchingFactor = 0;
	
	private boolean solved = false;
	
	private int numberStateVariables;
	
	private boolean binaryNovelty = false;
	
	private double quantifiedNovel = 0;
	
	private Set<ExplicitState> statesSeenSoFar = new HashSet<>();
	
	/**
	 * Incoming connectors.
	 */
	protected Set<SearchConnector> incomingConnectors = new LinkedHashSet<SearchConnector>();

	/**
	 * Outgoing connectors.
	 */
	protected Set<SearchConnector> outgoingConnectors = new LinkedHashSet<SearchConnector>();

	/**
	 * The outgoing connector currently marked.
	 */
	protected SearchConnector markedConnector = null;
	
	public SearchNode(State state, HeuristicSearch searchManager, int depth, int alternatingIndex) {
		super(state);
		
		assert depth >= 0;
		
		this.depth = depth;
		
		if(searchManager.getHeuristic() != null) {
			this.heuristic = searchManager.getHeuristic().getHeuristic(state);
			
			/*
			if(searchManager.getHeuristicTieBreak() != null)
				this.heuristicTieBreak = searchManager.getHeuristicTieBreak().getHeuristic(state);
			*/
		}
		
		if (this.state.isGoalState()) {
			this.setGoalNode(true);
			this.setProven();
		} else if(this.isDeadEndNode())
			this.setDeadEndNode(true);
		
		this.computeBranchingFactor(searchManager.getProblem());
	}
	
	private void computeBranchingFactor(Problem problem) {
		List<Operator> applicableOps = this.state.getApplicableOps(problem.getOperators());
		for(Operator op: applicableOps) {
			ExplicitOperator explicitOp = (ExplicitOperator) op;
			this.branchingFactor += explicitOp.getNondeterministicEffect().size();
		}
	}
	
	public void computeQuantifiedNovel() {
		int numberFactsBN = 0;
		ExplicitState currentState = ((ExplicitState) this.state);
		Set<Pair<Integer, Integer>> stateFacts = currentState.getVarsPropositions();
		for(Pair<Integer, Integer> fact: stateFacts) {
			if(this.getHeuristicNoveltyBF(fact, currentState) > 0) {
				numberFactsBN++;
			}
		}
		this.quantifiedNovel = (numberStateVariables - numberFactsBN);
	}
	
	public void computeBinaryNovelty() {
		ExplicitState currentState = ((ExplicitState) this.state);
		currentState.setHeuristic(heuristic);
		Set<Pair<Integer, Integer>> stateFacts = currentState.getVarsPropositions();
		for(Pair<Integer, Integer> fact: stateFacts) {
			if(this.getHeuristicNoveltyBF(fact, currentState) > 0) {
				this.binaryNovelty = true;
				break;
			}
		}
	}
	
	private double getHeuristicNoveltyBF(Pair<Integer, Integer> fact, ExplicitState state) {
		if(state.getVarsPropositions().contains(fact)) {
			return this.getNoveltyOfFact(fact, state) - state.getHeuristic();			
		}
		return Double.NEGATIVE_INFINITY;
	}
	
	private double getNoveltyOfFact(Pair<Integer, Integer> fact, ExplicitState state) {
		double minH = Double.POSITIVE_INFINITY;
		if(state.getVarsPropositions().contains(fact)) {
			for(ExplicitState s: this.statesSeenSoFar) {
				if(s.getVarsPropositions().contains(fact)) {
					if(s.getHeuristic() < minH)
						minH = s.getHeuristic();
				}
			}
			return minH;
		}
		return Double.NEGATIVE_INFINITY;
	}
	
	public void dump() {
		System.out.println("Dumping Node {");
		state.dump();
		System.out.println("h-Value:" + heuristic);
		System.out.println("index: " + index + " }");
	}

	@Override
	public int compareTo(SearchNode o) {
		return (int) (heuristic - o.heuristic);
	}
	
	public boolean isDeadEndNode() {
		return heuristic == Double.POSITIVE_INFINITY;
	}
	
	public double getHeuristic() {
		return heuristic;
	}
	
	public void setHeuristic(double heuristic) {
		this.heuristic = heuristic;
	}
	
	public SearchNode getParent() {
		return parent;
	}
	
	public void setParent(SearchNode parent) {
		this.parent = parent;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public int getBranchingFactor() {
		return branchingFactor;
	}
	
	public Set<SearchConnector> getIncomingConnectors() {
		return incomingConnectors;
	}
	
	public Set<SearchConnector> getOutgoingConnectors() {
		return outgoingConnectors;
	}
	
	public SearchConnector getMarkedConnector() {
		return markedConnector;
	}
	
	public void setMarkedConnector(SearchConnector markedConnector) {
		this.markedConnector = markedConnector;
	}
	
	public void setSolved(boolean solved) {
		this.solved = solved;
	}
	
	public boolean isSolved() {
		return solved;
	}
	
	public boolean getBinaryNovelty() {
		return binaryNovelty;
	}
	
	public double getQuantifiedNovel() {
		return quantifiedNovel;
	}
	
	public void setStatesSeenSoFar(Set<ExplicitState> statesSeenSoFar) {
		this.statesSeenSoFar = statesSeenSoFar;
	}
	
	public void setNodesSeenSoFar(Set<SearchNode> nodesSeenSoFar) {
		for(SearchNode n: nodesSeenSoFar) {
			ExplicitState s = ((ExplicitState) n.state);
			s.setHeuristic(n.getHeuristic());
			this.statesSeenSoFar.add(s);
		}
	}
	
	public Set<ExplicitState> getStatesSeenSoFar() {
		return statesSeenSoFar;
	}
	
	@Override
	public String toString() {
		return "Index = " + this.index + ": h-value = " + getHeuristic();
	}

	public void setNumberStateVariables(int numStateVars) {
		this.numberStateVariables = numStateVars;
	}
}
