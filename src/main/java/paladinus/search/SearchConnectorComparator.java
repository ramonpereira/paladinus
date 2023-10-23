package paladinus.search;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import paladinus.Global;
import paladinus.util.ActionSelectionRule;

/**
 *
 * @author Ramon Fraga Pereira
 *
 */
public class SearchConnectorComparator implements Comparator<SearchConnector> {

	public static boolean DEBUG = false;
	
	private ActionSelectionRule rule = ActionSelectionRule.MIN_H;
	
	private Set<SearchNode> closedVisited = new HashSet<>();

	public SearchConnectorComparator(ActionSelectionRule rule) {
		this.rule = rule;
	}
	
	public SearchConnectorComparator(ActionSelectionRule rule, Set<SearchNode> closedVisited) {
		this.rule = rule;
		this.closedVisited = closedVisited;
	}
	
	private int tieBreakerNumberOfOutcomesMinFirst(SearchConnector o1, SearchConnector o2) {
		return o1.getChildren().size() - o2.getChildren().size();
	}
	
	private int tieBreakerNumberOfOutcomesMaxFirst(SearchConnector o1, SearchConnector o2) {
		return o2.getChildren().size() - o1.getChildren().size();
	}
	
	private int tieBreakerMinSumFirst(SearchConnector o1, SearchConnector o2) {
		return (int) (o1.getSumChildEstimate() - o2.getSumChildEstimate());
	}
	
	private int tieBreakerMaxSumFirst(SearchConnector o1, SearchConnector o2) {
		return (int) (o2.getSumChildEstimate() - o1.getSumChildEstimate());
	}
	
	private int tieBreakerMinHTimesOutcomeSizeFirst(SearchConnector o1, SearchConnector o2) {
		return (int) (o1.getMinChildEstimateTimesChildrenSize() - o2.getMinChildEstimateTimesChildrenSize());
	}
	
	private int tieBreakerMaxHTimesOutcomeSizeFirst(SearchConnector o1, SearchConnector o2) {
		return (int) (o2.getMaxChildEstimateTimesChildrenSize() - o1.getMaxChildEstimateTimesChildrenSize());
	}
	
	private int tieBreakerMinMaxHTimesOutcomeSizeFirst(SearchConnector o1, SearchConnector o2) {
		return (int) (o1.getMaxChildEstimateTimesChildrenSize() - o2.getMaxChildEstimateTimesChildrenSize());
	}

	@Override
	public int compare(SearchConnector o1, SearchConnector o2) {
		int result = 0;
		
		o1.setVisitedChildren(this.closedVisited);
		o2.setVisitedChildren(this.closedVisited);
		
		switch (this.rule) {
			/* Action Selection functions considering Min (h-value). */
			case MIN_H:
				result = (int) (o1.getMinChildEstimate() - o2.getMinChildEstimate());
				break;
				
			case MIN_H_TIMES_CHILDREN_SIZE:
				result = (int) ((o1.getMinChildEstimateTimesChildrenSize()) - (o2.getMinChildEstimateTimesChildrenSize()));
				break;
				
			case MIN_H_POWER_CHILDREN_SIZE:
				result = (int) ((o1.getMinChildEstimateToPowerChildrenSize()) - (o2.getMinChildEstimateToPowerChildrenSize()));
				break;
				
			/* Action Selection functions considering Sum (h-value). */
			case MIN_SUM_H:
				result = (int) (o1.getSumChildEstimate() - o2.getSumChildEstimate());
				break;	
				
			case MIN_SUM_H_TIMES_CHILDREN_SIZE:
				result = (int) ((o1.getSumChildEstimateTimesChildrenSize()) - (o2.getSumChildEstimateTimesChildrenSize()));
				break;
				
			case MIN_SUM_H_POWER_CHILDREN_SIZE:
				result = (int) ((o1.getSumChildEstimateToPowerChildrenSize()) - (o2.getSumChildEstimateToPowerChildrenSize()));
				break;
			
			/* Action Selection functions considering Max (h-value). */
			case MIN_MAX_H:
				result = (int) (o1.getMaxChildEstimate() - o2.getMaxChildEstimate());
				break;
				
			case MAX_H:
				result = (int) (o2.getMaxChildEstimate() - o1.getMaxChildEstimate());
				break;

			case MIN_MAX_H_TIMES_CHILDREN_SIZE:
				result = (int) ((o1.getMaxChildEstimateTimesChildrenSize()) - (o2.getMaxChildEstimateTimesChildrenSize()));
				break;
				
			case MIN_MAX_H_POWER_CHILDREN_SIZE:
				result = (int) ((o1.getMaxChildEstimateToPowerChildrenSize()) - (o2.getMaxChildEstimateToPowerChildrenSize()));
				break;				
				
			/* Action Selection functions considering Mean (h-value). */
			case MEAN_H:
				double avgComparison = (o1.getAverageChildEstimate() - o2.getAverageChildEstimate());
				result = (int) avgComparison;
				if(avgComparison < 0)
					result = -1;
				break;
				
			/* Action Selection function that aims to estimate the branching factor. */
			case MIN_SUM_H_ESTIMATED_BRANCHING_FACTOR:
				result = (int) (o1.getSumEstimatedBranchingFactorToPowerHeuristicValue() - o2.getSumEstimatedBranchingFactorToPowerHeuristicValue());
				break;
				
			case MAX_AVG_H_VALUE:
				result = (int) (o1.getMaxAvgAndhValueParent() - o2.getMaxAvgAndhValueParent());
				break;					
				
			default:
				assert false;
				break;
		}
		
		if(result == 0) {
			if(Global.options.getTieBreakConnectors().equals("MIN_OUTCOMES_SIZE")) {
				result = tieBreakerNumberOfOutcomesMinFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MAX_OUTCOMES_SIZE")) {
				result = tieBreakerNumberOfOutcomesMaxFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MIN_SUM")) {
				result = tieBreakerMinSumFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MAX_SUM")) {
				result = tieBreakerMaxSumFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MIN_MAX_H_TIMES_OUTCOMES_SIZE")) {
				result = tieBreakerMinMaxHTimesOutcomeSizeFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MIN_H_TIMES_OUTCOMES_SIZE")) {
				result = tieBreakerMinHTimesOutcomeSizeFirst(o1, o2);
			} else if(Global.options.getTieBreakConnectors().equals("MAX_H_TIMES_OUTCOMES_SIZE")) {
				result = tieBreakerMaxHTimesOutcomeSizeFirst(o1, o2);
			}
		}
		return result;
	}
}
