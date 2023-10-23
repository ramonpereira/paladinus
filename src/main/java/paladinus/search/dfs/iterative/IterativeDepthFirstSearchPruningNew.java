package paladinus.search.dfs.iterative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import paladinus.heuristic.Heuristic;
import paladinus.problem.Problem;
import paladinus.search.SearchConnector;
import paladinus.search.SearchFlag;
import paladinus.search.SearchNode;
import paladinus.search.dfs.DepthFirstSearch;
import paladinus.util.Pair;

/**
 * 
 * An iterative (pruning) depth-first search algorithm for FOND Planning.
 * 
 * @author Ramon Fraga Pereira
 *
 */
public class IterativeDepthFirstSearchPruningNew extends DepthFirstSearch {
	
	protected double POLICY_SIZE = 0;
	protected double NEW_POLICY_BOUND = 0;
	
	private boolean checkSolvedStates = false;
	
	private Map<SearchNode, Double> nonPromisingNodes = new HashMap<>();

	public IterativeDepthFirstSearchPruningNew(Problem problem, Heuristic heuristic, String strategies, String criterion, String checkSolved) {
		super(problem, heuristic, strategies, criterion);
		if(this.checkSolvedStates || (checkSolved != null && checkSolved.contains("ON"))) {
			this.checkSolvedStates = true;
			System.out.println("Check Solveds: TRUE");
		}
	}
	
	@Override
	public Result run() {
		/* Start measuring search time. */
		starttime = System.currentTimeMillis();

		/* Get initial state and insert it with depth 0. */
		this.initialNode = this.lookupAndInsertNode(problem.getSingleInitialState(), 0);
		assert ((SearchNode) this.initialNode).getDepth() == 0;

		SearchFlag flag = doIterativeSearch(false, (SearchNode) this.initialNode);
		this.searchStatus = flag;

		/* Finish measuring search time. */
		endtime = System.currentTimeMillis();
		
		System.out.println("\n# Closed-Solved Nodes        = " + this.closedSolvedNodes.size());
		System.out.println("\n# Closed-Non-Promising Nodes = " + this.nonPromisingNodes.size());
		
		if (DEBUG)
			dumpStateSpace(this.NUMBER_ITERATIONS);

		if(timeout())
			return Result.TIMEOUT;
		
		if (flag == SearchFlag.GOAL) {
			return Result.PROVEN;
		} else if (flag == SearchFlag.NON_PROMISING || flag == SearchFlag.NO_POLICY) {
			return Result.DISPROVEN;
		} else return Result.TIMEOUT;
	}
	
	protected SearchFlag doIterativeSearch(Boolean unitaryBound, SearchNode node) {
		if(unitaryBound) {
			this.POLICY_BOUND = 0;			
		} else {
			this.POLICY_BOUND = node.getHeuristic();
			if(this.POLICY_BOUND == Double.POSITIVE_INFINITY)
				return SearchFlag.NO_POLICY;
		}
		SearchFlag flag = SearchFlag.NO_POLICY;
		
		this.NEW_POLICY_BOUND = Double.POSITIVE_INFINITY;
		
		System.out.println("\n> Bound Initial: " + this.POLICY_BOUND);
		System.out.println();
		do {
			System.out.println("> Bound: " + this.POLICY_BOUND);
			this.POLICY_SIZE = 0d;
			
			this.dumpingCounterStateSpace = 0;
			this.NUMBER_ITERATIONS++;

			Set<SearchNode> closedSolved = new HashSet<>();
			this.closedVisitedNodes.clear();
			this.nonPromisingNodes.clear();
			
			Pair<SearchFlag, Set<SearchNode>> resultSearch = doIterativeSearch(node, closedSolved, this.POLICY_SIZE, this.POLICY_BOUND);
			flag = resultSearch.first;
			this.closedSolvedNodes = resultSearch.second;
			
			if(unitaryBound) {
				this.POLICY_BOUND++;				
			} else this.POLICY_BOUND = this.NEW_POLICY_BOUND;
			
			this.NEW_POLICY_BOUND = Double.POSITIVE_INFINITY;
		} while (flag != SearchFlag.GOAL && this.POLICY_BOUND < Double.POSITIVE_INFINITY && flag != SearchFlag.TIMEOUT);
		return flag;
	}
	
	protected Pair<SearchFlag, Set<SearchNode>> doIterativeSearch(SearchNode node, Set<SearchNode> closedSolved, double policySize, double policyBound) {
		if (DEBUG)
			dumpStateSpace(this.NUMBER_ITERATIONS);
		
		if(RECURSION_COUNTER >= Integer.MAX_VALUE)
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.NON_PROMISING, closedSolved);
		
		if(timeout())
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.TIMEOUT, null);

		RECURSION_COUNTER++;
		
		if(node.isGoalNode() || closedSolved.contains(node)) {
			closedSolved.addAll(this.closedVisitedNodes);
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, closedSolved);
		} else if (this.nonPromisingNodes.containsKey(node) && (policySize >= this.nonPromisingNodes.get(node))) {
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.NON_PROMISING, closedSolved);
		} else if (this.closedVisitedNodes.contains(node))
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
		
		this.closedVisitedNodes.add(node);
		
		PriorityQueue<SearchConnector> connectors = this.getNodeConnectors(node);
		NODE_EXPANSIONS++;
		
		/* Gather all successors of n.
		 */
		Set<SearchNode> successorStatesOfNode = new HashSet<>(); 		
		for(SearchConnector c: connectors) {
			successorStatesOfNode.addAll(c.getChildren());
		}
		/* Try to improve h(n) with a 1-step look-ahead.
		 */
		node.setHeuristic(1 + this.getMinEstimateFromSetOfNodes(successorStatesOfNode));
		
		boolean allConnectorsNonPromising = true;
		while(!connectors.isEmpty()) {
			SearchConnector c = connectors.poll();
			
			if(policySize + 1 + c.getEvaluationFunctionAccordingToCriterion() > policyBound && closedSolved.size() == 0) {
				if(policySize + 1 + c.getEvaluationFunctionAccordingToCriterion() < this.NEW_POLICY_BOUND )
					this.NEW_POLICY_BOUND = policySize + 1 + c.getEvaluationFunctionAccordingToCriterion();				
			} else if(policySize + 1 > policyBound) {
				if(policySize + 1 < this.NEW_POLICY_BOUND)
					this.NEW_POLICY_BOUND = policySize + 1;
			} else { 
				Set<SearchNode> pathsFound = new HashSet<>();
				
				boolean newGoalPathFound = true;
				
				boolean connectorNonPromising = false;
				
				Set<SearchNode> copyClosedSolved = new HashSet<>(closedSolved);
				
				while(newGoalPathFound == true) {
					newGoalPathFound = false;
					List<SearchNode> findingGoalPath = new ArrayList<>();
					
					for(SearchNode s: c.getChildren()) {
						if(!pathsFound.contains(s))
							findingGoalPath.add(s);
					}

					for(SearchNode s: findingGoalPath) {
						FIXED_POINT_COUNTER++;
						
						Pair<SearchFlag, Set<SearchNode>> resultSearch = doIterativeSearch(s, copyClosedSolved, policySize+1, policyBound);
						
						SearchFlag flag = resultSearch.first;
						copyClosedSolved = new HashSet<SearchNode>(resultSearch.second);
						
						node.setHeuristic(1 + this.getMinEstimateFromSetOfNodes(successorStatesOfNode));
						
						if(flag == SearchFlag.NON_PROMISING) {
							newGoalPathFound = false;
							connectorNonPromising = true;
							break;
						}
						if(flag == SearchFlag.GOAL){
							newGoalPathFound = true;
							pathsFound.add(s);
						}
					}
					if(pathsFound.size() == c.getChildren().size()) {
						this.closedVisitedNodes.remove(node);
						node.setMarkedConnector(c);
						return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, copyClosedSolved);
					}
					if(!connectorNonPromising)
						allConnectorsNonPromising = false;
				}
			}
		}
		if(allConnectorsNonPromising) {
			this.closedVisitedNodes.remove(node);
			if(this.nonPromisingNodes.containsKey(node)) {
				this.nonPromisingNodes.replace(node, policySize);
			} else {
				this.nonPromisingNodes.put(node, policySize);	
			}
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.NON_PROMISING, closedSolved);
		}
		this.closedVisitedNodes.remove(node);
		return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
	}
	
	@Override
	public void printStats(boolean simulatePlan) {
		System.out.println("# Fixed Point Counter       = " + this.FIXED_POINT_COUNTER);
		System.out.println("# Number Iterations         = " + this.NUMBER_ITERATIONS);
		super.printStats(simulatePlan);
	}
}
