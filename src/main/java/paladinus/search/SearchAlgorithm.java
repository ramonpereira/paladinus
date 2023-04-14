package paladinus.search;

/**
 * 
 * @author Ramon Fraga Pereira
 *
 * Enumeration of available search algorithms (AO* Search, LAO* Search, Best First Size-Based Search, and FIP Search).
 */
public enum SearchAlgorithm {

	ITERATIVE_DFS,	
	ITERATIVE_DFS_PRUNING,
	
	DFS,
	
	ITERATIVE_DFS_LEARNING
}
