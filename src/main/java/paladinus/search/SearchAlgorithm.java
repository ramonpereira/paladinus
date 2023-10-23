package paladinus.search;

/**
 * 
 * @author Ramon Fraga Pereira
 *
 * Enumeration of available search algorithms (AO* Search, LAO* Search, Best First Size-Based Search, and FIP Search).
 */
public enum SearchAlgorithm {

	DFS,
	
	ITERATIVE_DFS,
	
	ITERATIVE_DFS_LEARNING,
	ITERATIVE_DFS_LEARNING_NEW,
	
	ITERATIVE_DFS_PRUNING,
	ITERATIVE_DFS_PRUNING_NEW,
	
	ITERATIVE_DFS_PRUNING_LEARNING,
	ITERATIVE_DFS_PRUNING_LEARNING_NEW,
}
