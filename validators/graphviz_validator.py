#! /usr/bin/env python

import sys, os, importlib
import networkx as nx
import networkx.algorithms as al

def find_leafnodes(G):
    leafnode = []
    for i in G.nodes:
        head =  []
        if nx.descendants(G, i) == set(): # Find all leaf nodes.
            for a in nx.ancestors(G, i):  # Get all ancestors for leaf node.
                if nx.ancestors(G, a) == set():  # Determine if ancestor is a head node.
                    head.append(a)
        if len(head) == 1: # If this leaf had only one head then append to leafnode.
            leafnode.append(i)

    print leafnode
    return leafnode 

if __name__ == '__main__':
	arg = sys.argv[1]
	goal_nodes = arg.split(', ')

	G = nx.DiGraph(nx.drawing.nx_pydot.read_dot('policy.dot'))

	print "\n$> Policy Statistics from graph (Paladinus): \n"
	print "\t Nodes: %d" % G.number_of_nodes()
	print "\t Edges: %d" % G.number_of_edges()
	# print "\tStrong: %s" % str(0 == len(list(nx.simple_cycles(G))))
	print " Strong Cyclic: %s" % str(len(find_leafnodes(G)) == len(goal_nodes))