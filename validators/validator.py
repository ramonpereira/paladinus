#! /usr/bin/env python

"""
 Author: Ramon Fraga Pereira
 Adapted from PRP planner (https://github.com/QuMuLab/planner-for-relevant-policies).
"""

import os, importlib
import argparse
from fondparser import grounder
from fondparser.formula import *
from normalizer import flatten
from translator import translate_policy as translator
import time

import networkx as nx
from networkx.drawing.nx_pydot import write_dot

class State:
    def __init__(self, fluents):
        self.fluents = set(fluents)
        self.hash_val = hash(','.join(sorted(list(map(str, fluents)))))

    def __hash__(self):
        return self.hash_val

    def __eq__(self, other):
        return self.hash_val == other.hash_val

    def is_goal(self, goal):
        return goal <= self.fluents


class VALAction:
    def __init__(self, prec, effs, name, mapping):
        self.name = name
        self.ppres = set(filter(lambda x: x > 0, prec))
        self.npres = set([-1 * i for i in filter(lambda x: x < 0, prec)])
        self.effs = effs
        self.mapping = mapping

    def __str__(self):
        return "%s: +Pre = %s / -Pre = %s / Effs = %s" % (self.name, \
                        str([self.mapping[fl] for fl in self.ppres]), \
                        str([self.mapping[fl] for fl in self.npres]), \
                        str(["(%s --> %s)" % (','.join([self.mapping[fl] for fl in c]), ','.join([self.mapping[fl] for fl in e])) for (c,e) in self.effs]))

""" Global variables. """
problem = None
fluents = {}
unfluents = {}
actions = {}
init_state = None
goal_state = None
unhandled = []
nodes = {}
states_to_actions = dict()

def validate_and_generate_graph(dfile, pfile, sol, val):
    """ Validate the policy and generate graph structure. """

    module_validator = importlib.import_module(val)
    problem = grounder.GroundProblem(dfile, pfile)

    global fluents
    fluents = {}
    global unfluents
    unfluents = {}

    index = 1
    # for f in set_fluents:
    for f in problem.fluents:
        # if 'not' in f:
        #     continue
        # f = f.replace(',', '')
        fluents[str(f).lower()] = index
        fluents["not(%s)" % str(f).lower()] = -1 * index
        unfluents[index] = str(f).lower()
        unfluents[-1 * index] = "not(%s)" % str(f).lower()
        index += 1

    # print fluents

    global actions
    actions = {}

    for op in problem.operators:
        if '_' == op.name[-1]:
            op_name = op.name[:-1].lower()
        else:
            op_name = op.name.lower()

        actions[op_name] = [VALAction(_convert_conjunction(fluents, op.precondition),
                                      _convert_cond_effect(fluents, eff), op_name, unfluents)
                            for eff in flatten(op)]

        # print("\n%s\n%s" % (op.name, '\n'.join(map(str, actions[op.name]))))

    # return
    # print actions
    global init_state
    init_state = State(_convert_conjunction(fluents, problem.init))
    # print _state_string(unfluents, init_state)

    global goal_state
    goal_state = State([-1])
    # print _state_string(unfluents, goal_state)
    goal_fluents = set(_convert_conjunction(fluents, problem.goal))

    open_list = [init_state]

    global nodes
    nodes = {init_state: 1, goal_state: 2}
    node_index = 3

    G = nx.MultiDiGraph()
    G.add_node(1, label="s0")
    G.add_node(2, label="G")

    module_validator.load(sol, fluents)

    # print "\nStarting the FOND simulation..."

    unhandled = []

    while open_list:
        u = open_list.pop(0)
        assert nodes[u] in G

        # print "\n--------\nHandling state:"
        # print _state_string(unfluents, u)

        next_actions = module_validator.next_action(u)
        for a in next_actions:
            if not a:
                # G.node[nodes[u]]['label'] = 'X'
                unhandled.append(u)
            else:
                i = 0
                if 'goal' in a:
                    continue
                for outcome in actions[a]:
                    v = progress(u, outcome, unfluents)
                    # print("\nNew state:")
                    # print(_state_string(unfluents, v))

                    i += 1
                    if v.is_goal(goal_fluents):
                        v = goal_state
                    elif v not in nodes:
                        nodes[v] = node_index
                        node_index += 1
                        G.add_node(nodes[v], label=node_index-1)
                        open_list.append(v)

                    states_to_actions[str(nodes[u]) + ' -> ' + str(nodes[v])] = a
                    G.add_edge(nodes[u], nodes[v], label="%s (%d)" % (a, i))
    return G


def _convert_cond_effect(mapping, eff):
    if isinstance(eff, And):
        return [_create_cond(mapping, f) for f in filter(lambda x: '=' not in str(x), eff.args)]
    elif isinstance(eff, Primitive) or (isinstance(eff, Not) and isinstance(eff.args[0], Primitive)):
        return [_create_cond(mapping, eff)]
    elif isinstance(eff, When):
        return [_create_cond(mapping, eff)]
    else:
        assert False, "Error: Tried converting a non-standard effect: %s" % str(eff)


def _create_cond(mapping, eff):
    if isinstance(eff, Primitive) or (isinstance(eff, Not) and isinstance(eff.args[0], Primitive)):
        return (set(), set([mapping[str(eff).lower()]]))
    elif isinstance(eff, When):
        return (set(_convert_conjunction(mapping, eff.condition)), set(_convert_conjunction(mapping, eff.result)))
    else:
        assert False, "Error: Tried converting a non-standard single effect: %s" % str(eff)


def _convert_conjunction(mapping, conj):
    if isinstance(conj, And):
        return [mapping[str(f).lower()] for f in filter(lambda x: '=' not in str(x), conj.args)]
    elif isinstance(conj, Primitive) or (isinstance(conj, Not) and isinstance(conj.args[0], Primitive)):
        return [mapping[str(conj).lower()]]
    else:
        assert False, "Error: Tried converting a non-standard conjunction: %s" % str(conj)


def _state_string(mapping, state):
    return '\n'.join(sorted([mapping[i] for i in state.fluents]))


def progress(s, o, m):
    assert o.ppres <= s.fluents and 0 == len(o.npres & s.fluents), \
        "Failed to progress %s:\nPrecondition: %s\nState:\n%s" % \
        (o.name, str(o.ppres), _state_string(m, s))

    # print("\nProgressing the following operator:")
    # print((o))

    adds = set()
    dels = set()
    for eff in o.effs:
        negeff = set(filter(lambda x: x < 0, eff[0]))
        poseff = eff[0] - negeff
        negeff = set(map(lambda x: x * -1, negeff))
        if (poseff <= s.fluents) and 0 == len(negeff & s.fluents):
            for reff in eff[1]:
                if reff < 0:
                    dels.add(reff * -1)
                else:
                    adds.add(reff)

    if 0 != len(adds & dels):
        print("Warning: Conflicting adds and deletes on action %s" % str(o))

    return State(((s.fluents - dels) | adds))

def generate_actions_mapping_and_unfluents():
    with open('action.map', 'w') as f:
        for a in actions:
            f.write("\n%s:\n" % a)
            i = 0
            for outcome in actions[a]:
                i += 1
                f.write("%d: %s\n" % (i, " / ".join(["%s -> %s" % (map(str, [unfluents[fl] for fl in c]), map(str, [unfluents[fl] for fl in e])) for (c,e) in outcome.effs])))

    if len(unhandled) > 0:
        with open('unhandled.states', 'w') as f:
            for s in unhandled:
                f.write("\n%s\n" % _state_string(unfluents, s))

    print("  Action mapping: action.map")
    if len(unhandled) > 0:
        print("Unhandled states: unhandled.states")

def validate(G):
    # Analyze the final controller.
    print("\n$> Policy Statistics from graph: \n")
    print("\t Nodes: %d" % G.number_of_nodes())
    print("\t Edges: %d" % G.number_of_edges())
    print("     Unhandled: %d" % len(unhandled))
    print("\tStrong: %s" % str(0 == len(list(nx.simple_cycles(G)))))
    print(" Strong Cyclic: %s" % str(G.number_of_nodes() == len(nx.single_source_shortest_path(G.reverse(), nodes[goal_state]))))

    # write_dot(G, 'graph.dot')

    # print("\n     Policy output as graph: graph.dot")
    # _generate_actions_mapping_and_unfluents()


def extract_all_shortest_plans(G):
    shortest_paths = [p for p in nx.all_shortest_paths(G, nodes[init_state], nodes[goal_state])]
    all_shortest_plans, _ = _extract_plans_from_paths(shortest_paths)

    return all_shortest_plans

def extract_all_plans(G):
    paths = [p for p in nx.all_simple_paths(G, nodes[init_state], nodes[goal_state])]
    all_plans, set_actions = _extract_plans_from_paths(paths)

    return all_plans, set_actions

def _extract_plans_from_paths(paths):
    plans = []
    actions = set()
    paths_set = []
    for p in paths:
        if not p in paths_set:
            paths_set.append(p)

    for p in paths_set:
        plan = []
        for i, a in enumerate(p):
            if (i < len(p)-1):
                s_to_action = str(p[i]) + ' -> ' + str(p[i+1])
                plan.append(states_to_actions[s_to_action])
                actions.add(states_to_actions[s_to_action])
        plans.append(plan)

    return plans, actions

if __name__ == '__main__':
    """
    Usage: python validator.py -d <DOMAIN> -p <PROBLEM> -s <POLICY> -m <MODULE>
    <DOMAIN> and <PROBLEM> are the original FOND domain and problem files.
    <POLICY> is a file that contains the policy.
    <MODULE> is the module for the <POLICY> interpreter.
    validators/<MODULE>.py should exist and contain (at least) two functions: load and next_action.
    Example Usage: python validator.py domain.pddl p09.pddl policy.out prp
    """
    parser = argparse.ArgumentParser(description="Validator that validates and generates data structure from a valid policy.")

    parser.add_argument('-d', dest='domain_path')
    parser.add_argument('-p', dest='problem_path')
    parser.add_argument('-s', dest='policy_file')
    parser.add_argument('-m', dest='validator_module', default="prp")

    args = parser.parse_args()

    domain = args.domain_path
    problem = args.problem_path
    policy = args.policy_file
    validator_module = args.validator_module

    translator.translate('output.sas', 'policy.out', 'policy-translated.out')

    """
    Validate the policy and generate the graph structure.
    """
    G = validate_and_generate_graph(domain, problem, policy, validator_module)

    """
    Validate policy (check if it is a Strong and/or Strong Cyclic policy).
    """
    validate(G)
