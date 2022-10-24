package paladinus.search;

import java.util.HashSet;
import java.util.Set;

import paladinus.state.Operator;

/**
 * A connector associates with a node <tt>node</tt> a set of successor nodes
 * <tt>succ_1, ..., succ_n</tt>. One connector corresponds to an AND
 * conjunction, whereas several outgoing connectors from one node are
 * interpreted disjunctively. Hence, a list of outgoing connectors corresponds
 * to a disjunction over conjunctions over possible successor states.
 *
 * @author Ramon Fraga Pereira
 *
 */
public class SearchConnector {
	
	public enum EvaluationFunctionCriterion {
		MIN, MAX
	}

	/**
	 * Parent node to which this connector is attached
	 */
	SearchNode parent;

	/**
	 * Child nodes
	 */
	Set<SearchNode> children;

	/**
	 * Operator which corresponds to this connector.
	 */
	Operator operator;

	boolean isSafe = true;

	/**
	 * Base cost of this connector
	 */
	double baseCost;

	/**
	 * True iff. all of its children are proven.
	 */
	private boolean isProven = false;

	/**
	 * True iff. at least one of its children is disproven.
	 */
	private boolean isDisproven = false;
	
	private double estimatedCost = 0;
	
	private double avgBranchingFactor = 0;

	private Set<SearchNode> visitedChildren = new HashSet<>();
	
	private EvaluationFunctionCriterion evaluationFunctionCriterion;

	/**
	 * Creates a new connector. Links parent and child nodes back to this connector.
	 *
	 * @param parent   Node to which this connector is attached
	 * @param children Child nodes
	 * @param operator Name of operator inducing this connector
	 */
	public SearchConnector(SearchNode parent, Set<SearchNode> children, Operator operator) {
		this.parent = parent;
		this.children = children;
		this.operator = operator;
		baseCost = operator.getCost();
		if(parent.outgoingConnectors.contains(this)){
			parent.outgoingConnectors.remove(this);
		}
		parent.outgoingConnectors.add(this);
		for (SearchNode child : children) {
			child.incomingConnectors.add(this);
		}
	}
	
	/**
	 * Creates a new connector. Links parent and child nodes back to this connector.
	 *
	 * @param parent Node to which this connector is attached
	 * @param children Child nodes
	 * @param operator Name of operator inducing this connector
	 * @param criterion Child criterion
	 */
	public SearchConnector(SearchNode parent, Set<SearchNode> children, Operator operator, EvaluationFunctionCriterion criterion) {
		this(parent, children, operator);
		this.evaluationFunctionCriterion = criterion;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SearchConnector)) {
			return false;
		}
		SearchConnector c = (SearchConnector) o;
		if (parent.equals(c.parent) && children.equals(c.children)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return parent.hashCode() + children.hashCode();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		/*
		buffer.append(parent.toString());
		buffer.append(" -> [ ");
		for (SearchNode child : children) {
			buffer.append(child.toString());
			buffer.append(" ");
		}
		buffer.append("] hash " + hashCode());
		*/
		buffer.append(this.getOperator() + " -> estimate = " + this.estimatedCost);
		return buffer.toString();
	}

	public void dump() {
		System.out.println("Dumping Connector {");
		parent.dump();
		System.out.println(" ->  ");
		for (SearchNode child : children) {
			child.dump();
		}
		System.out.println("}");
	}

	public void setBaseCost(double baseCost) {
		this.baseCost = baseCost;
	}

	public double getBaseCost() {
		return baseCost;
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	public void setVisitedChildren(Set<SearchNode> visitedChildren) {
		this.visitedChildren = visitedChildren;
	}
	
	public Set<SearchNode> getVisitedChildren() {
		return visitedChildren;
	}
	
	public double getAvgBranchingFactor() {
		return avgBranchingFactor;
	}
	
	public void setAvgBranchingFactor(double avgBranchingFactor) {
		this.avgBranchingFactor = avgBranchingFactor;
	}
	
	public int getNonVisitedChildrenSize() {
		int nonVisitedSize = 0;
		
		for(SearchNode child: this.getChildren())
			if(!this.visitedChildren.contains(child))
				nonVisitedSize += 1;
		
		return nonVisitedSize;
		
	}

	/**
	 * Get the set of child nodes.
	 *
	 * @return children
	 */
	public Set<SearchNode> getChildren() {
		return children;
	}

	public void setChildren(Set<SearchNode> children) {
		this.children = children;
	}

	/**
	 * Get parent node.
	 *
	 * @return parent
	 */
	public SearchNode getParent() {
		return parent;
	}
	
	public double getEstimatedCost() {
		return estimatedCost;
	}

	/**
	 * Get this connector's proven status.
	 *
	 * @return true iff. all children are proven.
	 */
	public boolean isProven() {
		checkProvenAndDisprovenStatus();
		return isProven;
	}

	/**
	 * Get this connector's disproven status.
	 *
	 * @return true iff. at least one child is disproven.
	 */
	public boolean isDisproven() {
		checkProvenAndDisprovenStatus();
		return isDisproven;
	}

	/**
	 * Check this connector's proven status.
	 */
	private void checkProvenAndDisprovenStatus() {
		if (isProven || isDisproven) {
			return;
		}
		isProven = true;
		isDisproven = false;
		for (SearchNode child : children) {
			isProven &= child.isProven();
			isDisproven |= child.isDisproven();
		}
	}
	
	public EvaluationFunctionCriterion getEvaluationFunctionCriterion() {
		return evaluationFunctionCriterion;
	}
	
	public void setEvaluationFunctionCriterion(EvaluationFunctionCriterion criterion) {
		this.evaluationFunctionCriterion = criterion;
	}
	
	/**
	 * Get the maximum or minimum cost estimate of this connector's children according to child criterion.
	 * 
	 * @return child cost estimate
	 */
	public double getEvaluationFunctionAccordingToCriterion() {
		if(this.evaluationFunctionCriterion == EvaluationFunctionCriterion.MAX)
			return this.getMaxChildEstimate();
		else if (this.evaluationFunctionCriterion == EvaluationFunctionCriterion.MIN)
			return this.getMinChildEstimate();
			
		return this.getMaxChildEstimate();
	}

	/**
	 * Get the maximum cost estimate of this connector's children.
	 *
	 * @return maximum child cost estimate
	 */
	public double getMaxChildEstimate() {
		double max = -1;
		for (SearchNode child : children) {
			if(this.visitedChildren.contains(child))
				continue;
			
			if (child.heuristic > max)
				max = child.heuristic;
		}
		if(!this.visitedChildren.isEmpty() && max == -1)
			return Double.POSITIVE_INFINITY;
		
		assert max >= 0;
		this.estimatedCost = max;
		return max;
	}

	/**
	 * Get the average cost estimate of this connector's children.
	 *
	 * @return average child cost estimate
	 */
	public double getAverageChildEstimate() {
		double average = 0;
		int num = 0;
		for (SearchNode child : children) {
			if(this.visitedChildren.contains(child))
				continue;
			
			average += child.heuristic;
			num++;
		}
		if (num > 0) {
			average = average / num;
		} else {
			assert average == 0;
		}
		this.estimatedCost = average;
		return average;
	}
	
	/**
	 * Get the minimum estimated cost of this connector's children.
	 *
	 * @return average child cost estimate
	 */
	public double getMinChildEstimate() {
		double min = Double.POSITIVE_INFINITY;
		for (SearchNode child : children) {
			if(this.visitedChildren.contains(child))
				continue;
			
			if (child.heuristic < min) {
				min = child.heuristic;
			}
		}
		assert min >= 0;
		this.estimatedCost = min;
		return min;
	}
	
	/**
	 * Get the sum of the estimated costs of this connector's children.
	 *
	 * @return average child cost estimate
	 */
	public double getSumChildEstimate() {
		double sum = 0;
		for (SearchNode child : children) {
			if(this.visitedChildren.contains(child))
				continue;
			
			sum += child.getHeuristic();
		}
		assert sum >= 0;
		this.estimatedCost = sum;
		return sum;
	}
	
	/*
	 * Get the maximum estimated cost times the number children of this connector.
	 */
	public double getMaxChildEstimateTimesChildrenSize() {
		double costEstimate = this.getMaxChildEstimate() * this.getNonVisitedChildrenSize();
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	/*
	 * Get the maximum estimated cost to the power the number children of this connector.
	 */
	public double getMaxChildEstimateToPowerChildrenSize() {
		double costEstimate = Math.pow(this.getNonVisitedChildrenSize(), this.getMaxChildEstimate());
		
		if(this.getNonVisitedChildrenSize() == 1)
			costEstimate = this.getMaxChildEstimate();
		
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	/*
	 * Get the minimum estimated cost times the number children of this connector.
	 */
	public double getMinChildEstimateTimesChildrenSize() {
		double costEstimate = this.getMinChildEstimate() * this.getNonVisitedChildrenSize();
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	/*
	 * Get the minimum estimated cost to the power the number children of this connector.
	 */
	public double getMinChildEstimateToPowerChildrenSize() {
		double costEstimate = Math.pow(this.getNonVisitedChildrenSize(), this.getMinChildEstimate());
		
		if(this.getNonVisitedChildrenSize() == 1)
			costEstimate = this.getMinChildEstimate();
		
		this.estimatedCost = costEstimate;
		return costEstimate;
	}

	/*
	 * Get the sum of the estimated cost for all children times the number children of this connector.
	 */
	public double getSumChildEstimateTimesChildrenSize() {
		double costEstimate = this.getSumChildEstimate() * this.getNonVisitedChildrenSize();
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	/*
	 * Get the sum of the estimated cost for all children to the power the number children of this connector.
	 */
	public double getSumChildEstimateToPowerChildrenSize() {
		double costEstimate = 0;
		for(SearchNode child: this.getChildren()) {
			if(this.visitedChildren.contains(child))
				continue;
			
			costEstimate += Math.pow(this.getNonVisitedChildrenSize(), child.getHeuristic());
		}
		if(this.getNonVisitedChildrenSize() == 1)
			costEstimate = this.getSumChildEstimate();
		
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	/*
	 * Get the sum of the average branching factor to the power of every children heuristic value.  
	 */
	public double getSumEstimatedBranchingFactorToPowerHeuristicValue() {
		double costEstimate = 0;
		for(SearchNode child: this.getChildren()) {
			if(this.visitedChildren.contains(child))
				continue;
			
			costEstimate += Math.pow(this.avgBranchingFactor, child.getHeuristic());
		}
		if(this.avgBranchingFactor == 1)
			costEstimate = this.getSumChildEstimate();
		
		this.estimatedCost = costEstimate;
		return costEstimate;
	}
	
	public double getMaxAvgAndhValueParent() {
		return Math.max(this.parent.heuristic, this.getAverageChildEstimate());
	}
	
	public int getNumberNoveltyChildren() {
		int novelChildren = 0;
		
		for(SearchNode s: this.children)
			if(s.getBinaryNovelty())
				novelChildren++;
		
		return novelChildren;
	}
	
	public double getAvgNovelChildren() {
		double sumNovelChildren = 0;
		
		for(SearchNode s: this.children) {
			sumNovelChildren += s.getQuantifiedNovel();
		}
		
		return sumNovelChildren / this.children.size();
	}
}
