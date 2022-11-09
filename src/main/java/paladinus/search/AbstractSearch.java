package paladinus.search;

import java.util.concurrent.Callable;

import paladinus.problem.Problem;
import paladinus.search.policy.Policy;
import paladinus.simulator.PlanSimulator;

/**
 * An abstract search engine.
 *
 * @author Robert Mattmueller
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractSearch implements Callable {
	/**
	 * Problem this search will operate on.
	 */
	protected Problem problem;
	
	protected double POLICY_BOUND = 0;

	public enum Result {
		// problem has been neither solved nor proven unsolvable yet
		UNDECIDED,

		// existence of a winning strategy for the protagonist
		PROVEN,

		// non-existence of a winning strategy for the protagonist
		DISPROVEN,
		
		EXPANDED_ALL,

		// indicating time-out
		TIMEOUT,
		
		// indicating out of memory
		OUT_OF_MEMORY
	};

	/**
	 * Indicates that no time-out is used.
	 */
	public static final long NO_TIMEOUT = Long.MAX_VALUE;

	/**
	 * System time when search started.
	 */
	protected long starttime;

	/**
	 * System time when search ended.
	 */
	public long endtime;

	/**
	 * Search time-out.
	 */
	private long timeout = AbstractSearch.NO_TIMEOUT;

	/**
	 * Counter for recursion.
	 */
	public static int RECURSION_COUNTER = 0;
	
	/**
	 * Counter for node expansions.
	 */
	public static int NODE_EXPANSIONS = 0;

	/**
	 * Counter for nodes.
	 */
	public static int NODES = 0;

	/**
	 * Start node of the search.
	 */
	protected AbstractNode initialNode;

	/**
	 * Expected costs of the resulting plan;
	 */
	protected Double planCost = null;

	/**
	 * Dump the policy in an arbitrary format.
	 */
	public void dumpPolicy() {
		System.out.println(getPolicy());
	}

	public void printPolicy(String filename) {
		getPolicy().printPolicyToFile(filename);
	}
	
	@Override
	public Object call() throws Exception {
		return this.run();
	}

	/**
	 * Return a policy in the form of an explicit state-action table.
	 *
	 * @return Explicit state action table representing the policy that was found or
	 *         null if no policy was found (so far).
	 */
	public abstract Policy getPolicy();

	/**
	 * Perform a complete run of the search algorithm.
	 *
	 * @return Indicator of result. <tt>AbstractSearch.PROTAGONIST_WINS</tt> if the
	 *         protagonist provably wins, <tt>AbstractSearch.ANTAGONIST_WINS</tt> if
	 *         the antagonist provably wins, and <tt>AbstractSearch.TIMEOUT</tt> if
	 *         time-out occurred before proof.
	 */
	public abstract Result run();

	/**
	 * Set the time-out for the search.
	 *
	 * @param timeout Time-out in milliseconds
	 */
	public void setTimeout(long timeout) {
		assert timeout > 0 : "A timeout of 0 or less seconds does not make sense.";
		this.timeout = timeout;
	}

	/**
	 * Check whether a time-out has occurred.
	 *
	 * @return True iff a time-out has been set and has been exceeded.
	 */
	protected boolean timeout() {
		if (getTimeout() == AbstractSearch.NO_TIMEOUT) {
			return false;
		}
		return System.currentTimeMillis() - starttime > getTimeout();
	}

	public AbstractSearch(Problem problem) {
		this.problem = problem;
	}

	/**
	 * Print statistics about the search.
	 *
	 * @param simulatePlan Indicates if the plan should be simulated to compute costs.
	 */
	public abstract void printStats(boolean simulatePlan);
	
	public void validatePolicy() {
	}

	/**
	 * Simulate the resulting policy and compute expected costs. Simulation time is measured.
	 */
	protected void simulatePlan() {
		assert (getPolicy() != null);
		long simulatorTime = System.currentTimeMillis();
		planCost = (new PlanSimulator(problem)).performValueIteration(getPolicy());
		long simulatorEndTime = System.currentTimeMillis();
		System.out.println("Plan cost (expected number of steps to goal): " + planCost);
		System.out.println("Plan simulator time: " + (simulatorEndTime - simulatorTime) / 1000 + " seconds.");
	}

	/**
	 * Get expected costs of the resulting plan.
	 *
	 * @return plan costs
	 */
	public Double getPlanCost() {
		return planCost;
	}

	/**
	 * Get root node of this search.
	 *
	 * @return initial node
	 */
	public AbstractNode getInitialNode() {
		return initialNode;
	}
	
	public Problem getProblem() {
		return problem;
	}

	public long getTimeout() {
		return timeout;
	}
	
	public double getPolicyBound() {
		return POLICY_BOUND;
	}
}
