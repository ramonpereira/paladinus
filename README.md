# PaladinusPlanner

Paladinus: An Iterative Depth-First Search FOND Planner.

## Usage

```bash
- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas 
- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl 
- Example (2): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-sas/blocksworld_p1.sas 
- Example (3): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl

Help:
 -h (-help)                             : print this message

Paladinus options:
 -printPolicy                           : print policy to stdout
 -exportPolicy FILENAME                 : export policy to file
 -exportDot FILENAME                    : export polity as DOT graph (GraphViz)
 -timeout N                             : set timeout in seconds

Search algorithms:
 -s (-search) [ITERATIVE_DFS |          : set search algorithm [default:
 ITERATIVE_DFS_PRUNING | DFS]             ITERATIVE_DFS]

Heuristics:
 -heuristic [HMAX | HADD | FF | PDBS |  : set heuristic [default: FF]
 LMCUT | BLIND | BLIND_DEADEND]            

Action Selection and Evaluation Function Criteria:
 -as (-actionSelectionCriterion) VAL    : set actionSelectionCriterion
                                          [default: MIN_MAX_H]
 -ef (-evaluationFunctionCriterion) VAL : set evaluationFunctionCriterion
                                          [default: MAX]

Translate:
 -t (-type) [FOND]                      : use fond translate (Example: -t
                                          <domain_file> <problem_file>)
```