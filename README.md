# VMAlloc

VMAlloc is a collection of algorithms for the Virtual Machine Consolidation (VMC) problem.
VMC instances in the format accepted by VMAlloc can be found
[here](http://sat.inesc-id.pt/dome/benchmarks.html).

VMAlloc contains implementations of the algorithms and techniques proposed in our
[SAT'17](https://link.springer.com/chapter/10.1007/978-3-319-66263-3_13),
[AAAI'18](https://www.aaai.org/ocs/index.php/AAAI/AAAI18/paper/download/17227/16245),
[IJCAI'18](https://www.ijcai.org/proceedings/2018/0191.pdf),
[Heuristics'18](https://link.springer.com/article/10.1007/s10732-018-9400-2) and
[IJCAI'19](https://www.ijcai.org/proceedings/2019/0165.pdf) papers.
It also includes implementations of all the algorithms used in those papers' experimental
evaluations, such as [VMPMBBO](https://www.sciencedirect.com/science/article/pii/S0167739X15000564).

# Instructions

The VMAlloc jar can be easily compiled through Maven by running

```
mvn package
```

in the root directory.
A 'target' directory will be created with the VMAlloc jar in it.

## Running vmalloc.jar

The algorithm to run is chosen through the **-a** option.

**Always** provide a time limit using the **-t** option, some algorithms (like the evolutionary
ones) may not work properly without one.

If trying to replicate the results in our papers, **always** use the **-ip** option.
Some instances may contain platform constraints, which are not considered in our publications,
but are supported by VMAlloc.

The **-m** option is used to specify a migration budget percentile.
For example, if a percentile of 0.05 is provided, then the sum of the memory requirements of the
virtual machines in the pre-existing mappings migrated to some other server cannot exceed the total
memory capacity times 0.05.

The **-dp** option specifies a file in which to store the solution set produced by the algorithm,
in the format used by the [MOEA Framework](http://moeaframework.org/).
These files can then be used to compute performance metrics that measure the quality of the solution
set (further details are provided later).

For randomized algorithms, **-ms** can be used to specify how many seeds to use.
In that case, the time limit provided using **-t** is used for each seed.
However, we advise agaisn't using this option and recommend just running each seed individually.
This feature will likely be removed in the future.

### Division Reduction

In our AAAI'18 paper we proposed a division reduction technique that enables our algorithms to
handle objectives that are sums of divisions of linear pseudo-Boolean expressions, such as the
resource wastage objective.
This technique is not considered in the SAT'17 and Heuristics'18 papers.
In the SAT'17 paper, we consider a slightly different linear formulation of resource wastage.
In the Heuristics'18 paper, we consider the true resource wastage formulation, but algorithms
that cannot handle it consider the linear formulation as an approximation instead.

This can be controlled through the **-ida** and **-ide** options.
The **-ida** option tells the algorithm to use the linear formulation in the solving process.
However, objective values printed by VMAlloc (to standard output or the solution file provided
through **-dp**) are still computed using the nonlinear formulation.
In order to print the values according to the linear formulation, use the **-ide** option.

### PCLD, SampleMCS, MCSEnumPD and SCLD

The Pareto-MCS algorithm (PCLD) was proposed at SAT'17 and later extended in the AAAI'18 and
IJCAI'18 papers.
To run the version of SAT'17:

```
java -server -jar vmalloc.jar -ip -ida -ide -a PCLD -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To run with division reduction enabled (AAAI'18 and IJCAI'18):

```
java -server -jar vmalloc.jar -ip -a PCLD -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

At AAAI'18, we proposed MCS sampling (SampleMCS) and path diversification (MCSEnumPD) as
techniques to improve diversity of the solution set.
MCS sampling is enabled through the **-h** option. Hence, to run the SampleMCS algorithm:

```
java -server -jar vmalloc.jar -ip -a PCLD -h -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To enable path diversification and, consequently, run the MCSEnumPD algorithm use **-pd**:

```
java -server -jar vmalloc.jar -ip -a PCLD -pd -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

At IJCAI'18 we proposed stratification in order to improve convergence and diversity.
The stratified algorithm is referred to as SCLD.
Stratification is enabled through the **-st** option.
It receives a string argument indicating the strategy to use in order to integrate with
division reduction.
Options are **MERGED** and **SPLIT** (further details are in the paper).
Two partitioning strategies were proposed: Literal-Weight Ratio (LWR) and Fixed Partition Number
(FIXED).
By default, LWR is used.
The ratio in LWR can be controlled through the **-lwr** option:

```
java -server -jar vmalloc.jar -ip -a PCLD -st <MERGED/SPLIT> -lwr <ratio> -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To run the FIXED partitioning strategy, use the **-pn** option to specify a fixed number of
partitions, overriding LWR:

```
java -server -jar vmalloc.jar -ip -a PCLD -st <MERGED/SPLIT> -pn <partition number> -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

### ENUM, Hash Functions, Symmetry Breaking and Heuristic Reduction

In our Heuristics'18 paper we proposed a model enumeration based algorithm called ENUM.
ENUM makes use of XOR hash functions in order to sample the search space instead of simply
enumerating solutions.
ENUM can handle the nonlinear resource wastage objective without the need for division
reduction.
To run the simple enumeration algorithm (without hash functions):

```
java -server -jar vmalloc.jar -ip -a HE -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

Use **-h** to enable hash functions:

```
java -server -jar vmalloc.jar -ip -a HE -h -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

At Heuristics'18, we proposed using symmetry breaking to discard equivalent solutions from
the search space.
Use **-s** to enable symmetry breaking:

```
java -server -jar vmalloc.jar -ip -a HE -h -s -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

Symmetry breaking should also work with PCLD and its variants, but it has not been tested yet.

We also proposed to use a bin-packing heuristic to simplify the problem before handing it to
ENUM.
Use **-r** to enable heuristic reduction:

```
java -server -jar vmalloc.jar -ip -a HE -h -r -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

Hash functions, symmetry breaking and heuristic reduction can be freely combined as desired.
Heuristic reduction in particular is compatible with all of VMAlloc's algorithms, but it is only
useful if coupled with algorithms that make use of constraint solvers.

### GIA

[GIA](https://dspace.mit.edu/handle/1721.1/46322) was used as a benchmark in the SAT'17 and
Heuristics'18 papers.
It was also used to evaluate the symmetry breaking and heuristic reduction techniques proposed in
the latter.
To run the SAT'17 version:

```
java -server -jar vmalloc.jar -ip -ida -ide -a GIA -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To run the Heuristics'18 version simply remove the **-ide** option:

```
java -server -jar vmalloc.jar -ip -ida -a GIA -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

Symmetry breaking and heuristic reduction can be enabled through the **-s** and **-r** options
respectively, just like with the ENUM algorithm.

### VMPMBBO, MGGA, MOEA/D and NSGAII

[VMPMBBO](https://www.sciencedirect.com/science/article/pii/S0167739X15000564) was used as a
benchmark in all papers.
The configuration used in our papers is the default one.
To run VMPMBBO:

```
java -server -jar vmalloc.jar -ip -a BBO -it SFF -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To run the SAT'17 version add the **-ida** and **-ide** options:

```
java -server -jar vmalloc.jar -ip -ida -ide -a BBO -it SFF -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

[MGGA](https://ieeexplore.ieee.org/abstract/document/5724828) was used as benchmark in all
papers except for the one at IJCAI'18.
To run MGGA with the same configuration as in our papers:

```
java -server -jar vmalloc.jar -ip -a GGA -it SFF -ps 12 -cr 0.8 -mr 0.0 -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

To run the SAT'17 version add the **-ida** and **-ide** options:

```
java -server -jar vmalloc.jar -ip -ida -ide -a GGA -it SFF -ps 12 -cr 0.8 -mr 0.0 -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

[MOEA/D](https://ieeexplore.ieee.org/abstract/document/4358754) is a general-purpose evolutionary
algorithm that was used as a benchmark in our IJCAI'18 and Heuristics'18 papers.
To run MOEA/D with the same configuration as in our papers:

```
java -server -jar vmalloc.jar -ip -a MOEAD -it SFF -ps 100 -cr 0.8 -mr 0.05 -ns 0.2 -delta 0.9 -eta 0.02 -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

[NSGAII](https://ieeexplore.ieee.org/abstract/document/996017) is also a general-purpose
evolutionary algorithm that was considered in all our papers except for the AAAI'18 one.
Unfortunately, NSGAII has not been working since we upgraded to version 2.12 of the MOEA
framework and the most recent version of SAT4J.
This is due to a bug in the MOEA Framework that gets triggered in versions of java later than 6,
and VMAlloc is no longer compatible with Java 6.
This bug has been fixed in the MOEA Framework github repository, but the changes have not been
pushed to the maven repository yet.
In case this gets fixed in the meantime, or if you decide to use the github version of the MOEA
Framework, here is the command for running NSGAII with the same configuration as in our papers:

```
java -server -jar vmalloc.jar -ip -a GA -it SFF -ps 100 -cr 0.8 -mr 0.05 -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

Add the **-ida** and **-ide** options to run the SAT'17 version:

```
java -server -jar vmalloc.jar -ip -ida -ide -a GA -it SFF -ps 100 -cr 0.8 -mr 0.05 -t <time limit> -m <migration budget percentile> -dp <solution set file path> <instance file path>
```

### Smart Operators

MOEA/D and NSGAII support the smart operators proposed in our IJCAI'19 paper.
To enable just smart mutation with the same configuration used in our paper, add the following options:

```
-smr 0.01 -mc 20000
```

To also enable smart improvement, add the following options:

```
-smr 0.01 -mc 20000 -si -irr 0.2 -imc 500000 -pmc 20000
```

## Performance Analysis

VMAlloc is also able to perform computation of multi-objective performance metrics.
In particular, we compute hypervolume, inverted generational distance, the epsilon-indicator and
spacing.
The analysis is done through the **-ap** option by providing the solution set files generated
using the **-dp** option.
The argument to the **-ap** option is a sequence of <label>:<solution set file path> pairs
separated by ';'.
<label> is the name of the algorithm that produced the file, and <solution set file path> is the
file path provided to the **-dp** option when the algorithm is executed.
The same <label> may appear multiple times with different files, in which case each file is
considered a separate run with a different seed for the same algorithm.
To run the analyzer:

```
java -jar vmalloc.jar -ip -m <migration budget percentile> -ap <label1>:<path1>;<label2>:<path2>;...;<labelN>:<pathN> <instance file path>
```

If replicating the results of our SAT'17 papers, add the **-ide** option:

```
java -jar vmalloc.jar -ip -ide -m <migration budget percentile> -ap <label1>:<path1>;<label2>:<path2>;...;<labelN>:<pathN> <instance file path>
```
