# Paladinus: An Iterative DFS FOND Planner

**Paladinus** is an Iterative Depth-First Search FOND planner written in Java. It has two iterative depth-first search (IDFS) algorithms for FOND Planning:

1. IDFS (Iterative Depth-First Search).
2. IDFS Pruning (Iterative Depth-First Search with Pruning).

## Requirements

* Java JDK 16.0.1+
* Python 3.6+ for [translator-fond](/translator-fond/) tool to translate FOND PDDL files to SAS.
* [Commons-IO](https://commons.apache.org/proper/commons-io/) 2.11.  Obtained via Maven but also available under `lib/` (for manual compile).
* [junit 4](https://junit.org/junit4/). Obtained via Maven but also available under `lib/` (for manual compile).
* [args4j](https://args4j.kohsuke.org/). Obtained via Maven but also available under `lib/` (for manual compile).
* [GraphViz](https://graphviz.readthedocs.io/) for visualizing the state-space and the output policy.

# Build (and test)

The planner is set-up as a Java Maven project.  To build it without tests:

```shell
$ mvn clean package -Dmaven.test.skip.exec
```

This will generate a JAR files (with and without dependencies) under `target/` and all classes under `target/classes`. We can test it by getting the help with `-h` (or `-help`):

```shell
$ java -jar target/paladinus-1.1-jar-with-dependencies.jar -h
```

This will work in the machine where it was compiled, as all dependencies would have been obtained by Maven. If executed elsewhere, use the full package with dependencies included `paladinus-1.1-SNAPSHOT-jar-with-dependencies.jar`.

To run it from directly from the classes:

```shell
$ java -cp  target/classes/:lib/commons-io-2.11.0.jar:lib/args4j-2.33.jar paladinus.PaladinusPlanner -help
```
## Usage Patterns

Paladinus accepts either SAS files or domain and problem PDDL files: as input, with the following patterns:

```shell
$ java [java_options] -jar ./target/paladinus-1.1-jar-with-dependencies.jar [planner_options] <problem.sas>

or

$ java [java_options] -jar ./target/paladinus-1.1-jar-with-dependencies.jar [planner_options] <domain.pddl> <problem.pddl>
```

Useful Java Options are:

 * `-Xmx4g`: to set the maximum heap space to 4 GB.
 * `-ea`: to enable assertions.

The main class in the JAR file is `paladinus.PaladinusPlanner` so we can also run it as follows as well:

```shell
$ java [java_options] -cp ./target/paladinus-1.1-jar-with-dependencies.jar \
    paladinus.PaladinusPlanner [planner_options] <problem.sas>

or

$ java [java_options] -cp ./target/classes/:lib/commons-io-2.11.0.jar: lib/args4j-2.33.jar  \
    paladinus.PaladinusPlanner [planner_options] <domain.pddl> <problem.pddl>
```

The last call using the compiled classes rather than the JAR file is useful when developing, as the IDE (e.g., VSCode) will compile automatically those classes.

### Running from anywhere

The above commands assume we are executing the planner from its root folder so that the FOND translator is in `./translator-fond` folder. If executing from elsewhere, we can use `-translatorPath` to point to the `translate.py` script. For example:

```shell
-translatorPath $HOME/planners/paladinus.git/translator-fond/translate.py
```

This will allow Paladinus to find the translator to use when PDDL files are provided.

## Examples

To run plain Iterative DFS:

```shell
$ # Using SAS inputs
$ java -jar ./target/paladinus-1.1-jar-with-dependencies.jar \
    -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    benchmarks/blocksworld-sas/blocksworld_p2.sas

$ # Using PDDL inputs (and calling the main class explicitly)
$ java -cp ./target/paladinus-1.1-jar-with-dependencies.jar \
    paladinus.PaladinusPlanner \
    -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    benchmarks/blocksworld-original/domain.pddl \
    benchmarks/blocksworld-original/p10.pddl


$ # Using PDDL inputs and using the compiled classes
$ java -cp ./target/classes::lib/commons-io-2.11.0.jar \
    paladinus.PaladinusPlanner \
    -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    benchmarks/blocksworld-original/domain.pddl \
    benchmarks/blocksworld-original/p10.pddl
```

Note `-printPolicy` to print the policy in standard output.

To run IDFS Pruning, use `-search ITERATIVE_DFS_PRUNING` and `-heuristic HADD`:

```shell
$ java -cp ./target/paladinus-1.1-jar-with-dependencies.jar \
    paladinus.PaladinusPlanner \
    -search ITERATIVE_DFS_PRUNING -heuristic HADD -printPolicy \
    benchmarks/blocksworld-original/domain.pddl \
    benchmarks/blocksworld-original/p10.pddl
```

To run it from a directory different than that from the planner (see use of `-translatorPath`):

```shell
java -cp <path/to/paladinus>/target/paladinus-1.1-jar-with-dependencies.jar \
    -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    -translatorPath paladinus.git/translator-fond/translate.py \
    paladinus.git/benchmarks/blocksworld-original/domain.pddl  \
    paladinus.git/benchmarks/blocksworld-original/p10.pddl
```


## Policy Output and Visualization

Paladinus outputs a policy when one exists for FOND planning task.
We provide two types of outputs:

- A text file (`policy.txt`), containing the output policy that maps states into actions; and
- A graph visualization for the output policy (`policy.dot`);

Our policy visualization outputs a [DOT file](https://en.wikipedia.org/wiki/DOT_(graph_description_language)) that corresponds to a graph of the solution policy.

To report the policy solution, one can use the following options:

* `-printPolicy`: prints the policy to standard output.
* `-exportPolicy FILENAME`: exports the policy to a `.txt` file.
* `-exportDot FILENAME`: exports the graph visualization file (DOT graph using GraphViz) of the policy.

For more details, check [POLICY.md](POLICY.md).

## Developing and Debugging

When developing in a Maven aware IDE (e.g., VSCODE) it may be convenient to run the planner from `target/classes` (instead of the JAR file) since
these will be automatically re-generated by the IDE when there is a change in source code, so

```
$ java [java_options] -cp ./target/classes/:lib/commons-io-2.11.0.jar:lib/args4j-2.33.jar  \
    paladinus.PaladinusPlanner [planner_options] <domain.pddl> <problem.pddl>
```

Paladinus provides a **state-space debugger** that shows the expanded states for every iteration of our algorithms. The state-space debugger outputs a [DOT file](https://en.wikipedia.org/wiki/DOT_(graph_description_language)) for every state expansion during the search. For more details, have a look at [DEBUG.md](DEBUG.md).

## Contributors

- Ramon Fraga Pereira
- Frederico Messa
- André Grahl Pereira
- Giuseppe De Giacomo

## Reference and Citation

Please, use the following reference when citing Paladinus.

- [Iterative Depth-First Search for FOND Planning](https://ojs.aaai.org/index.php/ICAPS/article/view/19789/19548), Ramon Fraga Pereira, André Grahl Pereira, Frederico Messa, and Giuseppe De Giacomo. [The 32nd International Conference on Automated Planning and Scheduling (ICAPS), 2022](http://icaps22.icaps-conference.org). 
  - You can find the BibTex [here](idfs-paladinus-icaps22.bib)!

## Acknowledgements

This work has been partially funded by the [ERC Advanced Grant "WhiteMech"](whitemech.github.io/)
(No. 834228) and by the [TAILOR research network](https://tailor-network.eu/) (No. 952215).

## License

This software is released under the GNU Lesser General Public License version 3 or later.

