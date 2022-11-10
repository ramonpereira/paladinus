/**
 * Interface to use both types of states (e.g. belief and explicit states) into the Node class.
 */
package main.java.paladinus.state;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.heuristic.pdb.Abstraction;
import main.java.paladinus.problem.Problem;

/**
 * @author Manuela Ortlieb
 *
 */
public abstract class State {
	/**
	 * The problem this state originates from
	 */
	public Problem problem;

	/**
	 * Indicates if this state is an abstract or a concrete state.
	 */
	protected final boolean isAbstractedState;

	/**
	 * If this is an abstracted state, there has to be a an corresponding
	 * abstraction containing abstracted operators.
	 */
	public Abstraction abstraction;

	/**
	 * Unique id which is used to identify a state.
	 */
	public final BigInteger uniqueID;

	/**
	 * HashCode which is used in collections which use hashing.
	 */
	public final int hashCode;

	/**
	 * Indicates if all operators were checked yet.
	 */
	private boolean applicableOpsInitialized = false;

	/**
	 * Note: This is only used to assert, that applicability for each operator is
	 * checked on exactly one set of operators. To avoid inconsistence.
	 */
	private Collection<Operator> checkedOps;

	/**
	 * After checking applicability of every operator this list contains each
	 * applicable operator. TODO Does it make sense to use a list instead of a set?
	 */
	private List<Operator> applicableOps;

	public State(Problem problem, BigInteger uniqueID, boolean isAbstractedState, Abstraction abstraction) {
		this.problem = problem;
		assert (problem != null);
		this.uniqueID = uniqueID;
		hashCode = uniqueID.intValue();
		this.isAbstractedState = isAbstractedState;
		this.abstraction = abstraction;
		assert (!isAbstractedState || (abstraction != null));
		assert ((abstraction != null) || !isAbstractedState);
	}

	/**
	 * Set the problem the state will consider itself associated with
	 */
	public void setProblem(Problem p) {
		problem = p;
	}

	public boolean isGoalState() {
		if (isAbstractedState) {
			return abstraction.goal.isSatisfiedIn(this);
		}
		return problem.getGoal().isSatisfiedIn(this);
	}

	public List<Operator> getApplicableOps(Collection<Operator> ops) {
		if (!applicableOpsInitialized) {
			List<Operator> applicableOps = new ArrayList<Operator>();
			for (Operator op : ops) {
				if (isApplicable(op)) {
					applicableOps.add(op);
				}
			}
			this.applicableOps = Collections.unmodifiableList(applicableOps);
			applicableOpsInitialized = true;
			checkedOps = ops;
		} else {
			if (!ops.equals(checkedOps)) {
				applicableOpsInitialized = false;
				getApplicableOps(ops);
			}
		}
		return applicableOps;
	}

	public abstract Set<State> apply(Operator operator);

	public void free() {
	};

	public abstract List<ExplicitState> getAllExplicitWorldStates();

	public abstract void dump();

	/**
	 * HashCode which is generally not unique.
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Check equality for this object and given object.
	 *
	 * @param o object to compare
	 * @return true iff both objects are explicit states and variable value
	 *         assignments are equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof State)) {
			return false;
		}
		State s = (State) o;
		return s.uniqueID.equals(uniqueID);
	}

	/**
	 * Note: It is important that this method is overwritten by symbolic operator,
	 * since sensing operators should not be applicable if there is no splitting.
	 *
	 * @param operator
	 * @return
	 */
	public abstract boolean isApplicable(Operator operator);

	/**
	 * Reset applicable operators to enforce that all operators are tested again.
	 * Note: Necessary after determinization for initial state.
	 */
	public void resetApplicableOps() {
		if (applicableOpsInitialized) {
			applicableOpsInitialized = false;
			applicableOps = new ArrayList<Operator>(applicableOps.size());
			checkedOps = null;
		}
	}

	/**
	 * Return this state as a condition.
	 *
	 * @return
	 */
	public abstract Condition toCondition();

	/**
	 * String representation of this state with proposition names.
	 *
	 * @return string representation of this state
	 */
	public abstract String toStringWithPropositionNames();
	
	/**
	 * String representation of this state with proposition names.
	 *
	 * @return string representation of this state
	 */
	public abstract String toStringPropositionNames();
}
