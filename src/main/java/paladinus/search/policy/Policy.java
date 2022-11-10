package main.java.paladinus.search.policy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.paladinus.Global;
import main.java.paladinus.PaladinusPlanner;
import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;
import main.java.paladinus.util.Pair;

/**
 * A policy is a mapping from states to actions, aka. plan.
 *
 * @author Robert Mattmueller
 *
 */

public class Policy {

	/**
	 * Set of seen operators.
	 */
	private Set<Operator> seenOperators = new HashSet<>();

	/**
	 * Policy entries, that are mappings from state to an applicable operator.
	 */
	final LinkedHashMap<State, Pair<Operator, Integer>> entries;

	/**
	 * The problem this policy works on
	 */
	public final Problem problem;
	
	private boolean valid = false;

	public Policy(Problem problem) {
		this.problem = problem;
		entries = new LinkedHashMap<State, Pair<Operator, Integer>>();
	}

	public boolean containsEntry(State state) {
		return entries.containsKey(state);
	}

	public Operator getOperator(State state) {
		assert entries.containsKey(state);
		return entries.get(state).first;
	}

	public int getDistance(State state) {
		return entries.get(state).second;
	}

	public void addEntry(State key, Operator op) {
		seenOperators.add(op);
		addEntry(key, op, -1);
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void addEntry(State key, Operator op, int distance) {
		assert problem.getOriginalOperatorMap().containsKey(op.getName());
		if (Global.options.getPolicyType().equals("STRONG_CYCLIC"))
			entries.put(key, new Pair<Operator, Integer>(problem.getOriginalOperatorMap().get(op.getName()), distance));
		else entries.put(key, new Pair<Operator, Integer>(problem.getModifiedOperatorMap().get(op.getName()), distance));
	}

	public void removeEntry(State state) {
		entries.remove(state);
	}

	public int size() {
		return entries.size();
	}

	public Set<Operator> getActions() {
		return seenOperators;
	}

	public Map<State, Pair<Operator, Integer>> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		if (problem.isFullObservable) {
			return policyToString();
		} else {
			return toStringDebugCompactOutput();
		}
	}

	public String toStringDebugCompactOutput() {
		StringBuffer buffer = new StringBuffer();
		for (State state : entries.keySet()) {
			buffer.append("if ");
			buffer.append(state);
			buffer.append("\n");
			buffer.append("then ");
			buffer.append(getOperator(state));
			buffer.append(";");
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * Note: Use this only for debugging in POND since computing explicit
	 * representation of a belief state is very expensive (and generally not
	 * tractable).
	 *
	 * @return string representation of this policy with explicit description of the
	 *         state.
	 */
	public String toStringDebugExplicitOutput() {
		StringBuffer buffer = new StringBuffer();
		for (State state : entries.keySet()) {
			buffer.append("if ");
			buffer.append(state.toStringWithPropositionNames());
			buffer.append("\n");
			buffer.append("then ");
			buffer.append(getOperator(state));
			buffer.append(";");
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * Assemble a string representing this state-action table in the plan output
	 * format used in the Uncertainty Part of the 6th International Planning
	 * Competition 2008.
	 *
	 * @return Compact (yet unfactored) policy representation of this state-action table.
	 */
	public String toStringPolicy() {
		assert problem.isFullObservable;
		StringBuffer buffer = new StringBuffer();
		List<List<String>> propositionNames = problem.propositionNames;

		// Collect and enumerate proposition names. Skip negated propositions.
		List<String> allPropositions = new ArrayList<String>();
		Map<String, Integer> allPropositionsMap = new LinkedHashMap<String, Integer>();
		int i = 0;
		for (List<String> names : propositionNames) {
			for (String name : names) {
				if (!name.startsWith("(not ")) {
					allPropositions.add(name);
					allPropositionsMap.put(name, i);
					i++;
				}
			}
		}

		// Output proposition names.
		buffer.append(allPropositions.size());
		buffer.append(" ");
		for (String name : allPropositions) {
			buffer.append(name);
			buffer.append(" ");
		}
		buffer.append("\n%%\n");

		// Collect and enumerate operator names.
		List<String> allOperators = new ArrayList<String>();
		Map<String, Integer> allOperatorsMap = new LinkedHashMap<String, Integer>();
		i = 0;
		for (State state : entries.keySet()) {
			String name = getOperator(state).getName();
			if (!allOperatorsMap.containsKey(name)) {
				allOperators.add(name);
				allOperatorsMap.put(name, i);
				i++;
			}
		}

		// Output operators
		buffer.append(allOperators.size());
		buffer.append(" ");
		for (String name : allOperators) {
			buffer.append("(");
			buffer.append(name);
			buffer.append(")");
			buffer.append(" ");
		}
		buffer.append("\n%%\n");

		// Actual policy
		buffer.append("policy ");
		buffer.append(entries.size());
		buffer.append(" ");

		// For each mapping from partial state to action...
		for (State state : entries.keySet()) {
			String operatorName = getOperator(state).getName();

			// determine size of partial state (number of satisfied propositions)
			i = 0;
			Map<Integer, Integer> variableValuePairs = ((ExplicitState) state).variableValueAssignment;
			for (int var : variableValuePairs.keySet()) {
				int value = variableValuePairs.get(var);
				String name = propositionNames.get(var).get(value);
				if (!name.startsWith("(not ")) {
					i++;
				}
			}

			// ... print that number ...
			buffer.append(i);
			buffer.append(" ");

			// ... print the propositions of that partial state (or rather their
			// indices as determined above) ...
			for (int var : variableValuePairs.keySet()) {
				int value = variableValuePairs.get(var);
				String name = propositionNames.get(var).get(value);
				if (!name.startsWith("(not ")) {
					buffer.append(allPropositionsMap.get(name));
					buffer.append(" ");
				}
			}

			// ... and print the number of the operator to be applied in that
			// state.
			buffer.append(allOperatorsMap.get(operatorName));
			buffer.append(" ");
		}
		return buffer.toString();
	}

	/**
	 * Return the occurrences of sensing actions in the resulting policy. So if a
	 * sensing action is used n times, it is count n times.
	 *
	 * @return occurrences of sensing
	 */
	public int getNumberOfSensingApplication() {
		int result = 0;
		for (State state : entries.keySet()) {
			if (getOperator(state).isSensing) {
				result++;
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void printPRPpolicyToFile(String filename) {
		filename = filename.replace(".txt", "");
		filename = filename + ".out";
		
		String prpPolicy = "";
		
		for(State s: this.getEntries().keySet()) {
			prpPolicy += "If holds: " + s + "\n";
			prpPolicy += "Execute: " + this.getEntries().get(s).first + " / SC / d=0\n\n";
		}
		
		System.out.println("\n@> PRP Policy file: " + filename);
		File policy = new File(filename);
		try {
			FileWriter writer = new FileWriter(policy);
			writer.write(prpPolicy);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filename
	 */
	public void printPolicyToFile(String filename) {
		String policyToPrint = "";
		
		for(State s: this.getEntries().keySet()) {
			policyToPrint += "If holds: " + ((ExplicitState) s).toStringPropositionNames() + "\n";
			policyToPrint += "Execute: " + this.getEntries().get(s).first + "\n\n";
		}
		
		System.out.println("\n@> Policy file: " + filename);
		File policy = new File(filename);
		try {
			FileWriter writer = new FileWriter(policy);
			writer.write(policyToPrint);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String policyToString() {
		String policyToPrint = "";
		for(State s: this.getEntries().keySet()) {
			policyToPrint += "If holds: " + ((ExplicitState) s).toStringPropositionNames() + "\n";
			policyToPrint += "Execute: " + this.getEntries().get(s).first + "\n\n";
		}
		return policyToPrint;
	}
	
	/**
	 * Prints the plan to file with given file name.
	 *
	 * @param filename
	 */
	public void printToFile(String filename) {
		if (filename.trim().equals("")) {
			filename = PaladinusPlanner.getNameOfProblemInstance() + ".fond_plan";
		}
		System.out.println("@> Policy file: " + filename);
		File plan = new File(filename);
		try {
			FileWriter writer = new FileWriter(plan);
			writer.write(toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
