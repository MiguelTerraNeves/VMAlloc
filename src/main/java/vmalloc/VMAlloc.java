/*
 * VMAlloc, Copyright (c) 2018, Miguel Terra-Neves, Vasco Manquinho, Ines Lynce
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package vmalloc;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.moeaframework.analysis.sensitivity.ResultFileReader;
import org.moeaframework.core.FrameworkException;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Settings;
import org.moeaframework.core.Solution;
import org.moeaframework.core.spi.ProblemFactory;
import org.moeaframework.util.ReferenceSetMerger;
import org.sat4j.specs.IVec;

import vmalloc.algorithm.AllocAlgorithm;
import vmalloc.algorithm.BBOAlloc;
import vmalloc.algorithm.BestFitDecreasingAlloc;
import vmalloc.algorithm.ConstraintBasedAllocAlgorithm;
import vmalloc.algorithm.DEAlloc;
import vmalloc.algorithm.EvolutionaryAllocAlgorithm;
import vmalloc.algorithm.EvolutionaryAllocAlgorithm.InitializationType;
import vmalloc.algorithm.FirstFitDecreasingAlloc;
import vmalloc.algorithm.GAAlloc;
import vmalloc.algorithm.GGAAlloc;
import vmalloc.algorithm.GIAAlloc;
import vmalloc.algorithm.HashEnumAlloc;
import vmalloc.algorithm.LinearSearchAlloc;
import vmalloc.algorithm.MCSAlloc;
import vmalloc.algorithm.MOEADAlloc;
import vmalloc.algorithm.MultiObjectiveConstraintBasedAllocAlgorithm;
import vmalloc.algorithm.PBOAlloc;
import vmalloc.algorithm.ParetoCLD;
import vmalloc.algorithm.ParetoLBX;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.algorithm.evolutionary.VMCwMProblemProvider;
import vmalloc.domain.Job;
import vmalloc.domain.JobVec;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.exception.HeuristicReductionFailedException;
import vmalloc.preprocess.HeuristicReducer;
import vmalloc.utils.IOUtils;
import vmalloc.utils.MiscUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Application's main class.
 * @author Miguel Terra-Neves
 */
public class VMAlloc {

    /**
     * String representation of the linear search algorithm.
     */
    private static final String LINEAR_SEARCH = "LS";
    
    /**
     * String representation of the minimal correction subset algorithm.
     */
    private static final String MCS = "MCS";
    
    /**
     * String representation of the pseudo-Boolean optimization algorithm.
     */
    private static final String PBO = "PBO";
    
    /**
     * String representation of the first-fit decreasing bin-packing algorithm.
     */
    private static final String FFD = "FFD";
    
    /**
     * String representation of the best-fit decreasing bin-packing algorithm.
     */
    private static final String BFD = "BFD";
    
    /**
     * String representation of the differential evolution algorithm.
     */
    private static final String DE = "DE";
    
    /**
     * String representation of the genetic algorithm.
     */
    private static final String GA = "GA";
    
    /**
     * String representation of the decomposition-based evolutionary algorithm.
     */
    private static final String MOEAD = "MOEAD";
    
    /**
     * String representation of the biogeography-based optimization algorithm.
     */
    private static final String BBO = "BBO";
    
    /**
     * String representation of the grouping genetic algorithm.
     */
    private static final String GGA = "GGA";
    
    /**
     * String representation of the guided improvement algorithm.
     */
    private static final String GIA = "GIA";
    
    /**
     * String representation of the model enumeration algorithm.
     */
    private static final String HASH = "HE";
    
    /**
     * String representation of the Pareto minimal correction subset approach that uses the CLD algorithm.
     */
    private static final String PARETO_CLD = "PCLD";
    
    /**
     * String representation of the Pareto minimal correction subset approach that uses the LBX algorithm.
     */
    private static final String PARETO_LBX = "PLBX";
    
    /**
     * Default literals to distinct weights ratio for stratification in MCS based algorithms.
     */
    private static final String DEFAULT_LIT_WEIGHT_RATIO = "2.0";
    
    /**
     * String representation of the random population initialization operator.
     */
    private static final String RAND_INIT = "RAND";
    
    /**
     * String representation of the random bin-packing population initialization operator.
     */
    private static final String RAND_PACKING_INIT = "RBP";
    
    /**
     * String representation of the shuffled first-fit population initialization operator.
     */
    private static final String SHUFFLED_FIRST_FIT_INIT = "SFF";
    
    /**
     * String representation of the shuffled VMCwM bin-packing heuristic based population initialization
     * operator.
     */
    private static final String SHUFFLED_VMCWM_INIT = "SVMCWM";
    
    /**
     * String representation of the mixed bin-packing population initialization operator.
     */
    private static final String MIXED_INIT = "MIXED";
    
    /**
     * String representation of the merged stratification strategy.
     */
    private static final String MERGED_STRAT = "MERGED";
    
    /**
     * String representation of the probability split stratification strategy.
     */
    private static final String SPLIT_STRAT = "SPLIT";
    
    /**
     * Default population size.
     */
    private static final String DEFAULT_POPSIZE = "100";
    
    /**
     * Default crossover rate for differential evolution.
     */
    private static final String DEFAULT_DE_CROSSOVER_RATE = "0.1";
    
    /**
     * Default step-size for differential evolution.
     */
    private static final String DEFAULT_DE_STEP_SIZE = "0.5";
    
    /**
     * Default population size per archipelago for biogeography-based optimization.
     */
    private static final String DEFAULT_BBO_POPSIZE = "3";
    
    /**
     * Default immigration rate for biogeography-based optimization.
     */
    private static final String DEFAULT_BBO_IMMIGRATION_RATE = "0.5";
    
    /**
     * Default mutation rate for biogeography-based optimization.
     */
    private static final String DEFAULT_BBO_MUTATION_RATE = "0.05";
    
    /**
     * Default cross-archipelago migration rate for biogeography-based optimization.
     */
    private static final String DEFAULT_BBO_CROSS_MIGRATION_RATE = "0.5";
    
    /**
     * Default crossover rate for the genetic algorithm.
     */
    private static final String DEFAULT_GA_CROSSOVER_RATE = "0.8";
    
    /**
     * Default mutation rate for the genetic algorithm.
     */
    private static final String DEFAULT_GA_MUTATION_RATE = "0.05";
    
    /**
     * Default crossover rate for the decomposition-based evolutionary algorithm.
     */
    private static final String DEFAULT_MOEAD_CROSSOVER_RATE = "0.8";
    
    /**
     * Default mutation rate for the decomposition-based evolutionary algorithm.
     */
    private static final String DEFAULT_MOEAD_MUTATION_RATE = "0.05";
    
    /**
     * Default neighborhood size for the decomposition-based evolutionary algorithm.
     */
    private static final String DEFAULT_MOEAD_NEIGHBORHOOD_SIZE = "0.2";
    
    /**
     * Default neighborhood size for the decomposition-based evolutionary algorithm.
     */
    private static final String DEFAULT_MOEAD_DELTA = "0.9";
    
    /**
     * Default neighborhood size for the decomposition-based evolutionary algorithm.
     */
    private static final String DEFAULT_MOEAD_ETA = "0.02";
    
    /**
     * Default mutation rate for the grouping genetic algorithm.
     */
    private static final String DEFAULT_GGA_MUTATION_RATE = "0.0";
    
    /**
     * Default crossover rate for the grouping genetic algorithm.
     */
    private static final String DEFAULT_GGA_CROSSOVER_RATE = "0.8";
    
    /**
     * Default migration budget percentile.
     */
    private static final String DEFAULT_MIG_PERCENTILE = "1.0";
    
    /**
     * Prints a help message to standard output.
     * @param options An object that represents the application's options.
     */
    // TODO: print proper help message
    private static void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("vmalloc", options);
    }
    
    /**
     * Discards the platform constraints from a set of a jobs. Platform constraints are the ones where some
     * virtual machines are allowed to run only in a subset of the physical machines.
     * @param jobs The jobs.
     */
    private static void discardPlatformConstraints(JobVec jobs) {
        for (int i = 0; i < jobs.size(); ++i) {
            Job job = jobs.get(i);
            for (int j = 0; j < job.nVirtualMachines(); ++j) {
                job.getVirtualMachine(j).clearUnallowedPhysicalMachines();
            }
        }
    }
    
    /**
     * Discards the anti-colocation constraints from a set of a jobs.
     * @param jobs The jobs.
     */
    private static void discardAntiColocationConstraints(JobVec jobs) {
        for (int i = 0; i < jobs.size(); ++i) {
            Job job = jobs.get(i);
            for (int j = 0; j < job.nVirtualMachines(); ++j) {
                job.getVirtualMachine(j).setFullyColocatable();
            }
        }
    }
    
    /**
     * Pretty-prints an allocation to the standard output.
     * @param maps The allocation.
     */
    private static void printAllocation(MappingVec maps) {
        for (int i = 0; i < maps.size(); ++i) {
            PhysicalMachine pm = maps.get(i).getPhysicalMachine();
            VirtualMachine vm = maps.get(i).getVirtualMachine();
            System.out.println("p " + vm.getJobID() + "-" + vm.getIndex() + " -> " + pm.getID());
        }
    }
    
    /**
     * Analyzes a set of population files. The population files are given as a string with comma-separated
     * key-value pairs. The elements of a pair are separated by ':'. The key corresponds to a label and the
     * value a path to a population file.
     * @param instance The problem instance.
     * @param dataset_desc The string with the dataset to be analyzed.
     * @throws IOException If an error occurs reading from the population file.
     */
    private static void analyze(VMCwMProblem instance, String dataset_desc) throws IOException {
        // Parse dataset description
        String[] label_fpath_pairs = dataset_desc.split(";");
        Multimap<String, String> dataset = ArrayListMultimap.create();
        for (int i = 0; i < label_fpath_pairs.length; ++i) {
            String[] label_fpath_pair = label_fpath_pairs[i].split(":");
            String label = label_fpath_pair[0], path = label_fpath_pair[1];
            dataset.put(label, path);
        }
        // Build reference set
        ReferenceSetMerger merger = new ReferenceSetMerger();
        for (Iterator<String> it = dataset.keySet().iterator(); it.hasNext();) {
            String label = it.next();
            Iterator<String> path_it = dataset.get(label).iterator();
            for (int i = 0; path_it.hasNext();) {
                String path = path_it.next();
                ResultFileReader reader = new ResultFileReader(instance, new File(path));
                try {
                    for (; reader.hasNext(); ++i) {
                        NondominatedPopulation pop = reader.next().getPopulation();
                        Population ref_pop = new Population();
                        for (int j = 0; j < pop.size(); ++j) {
                            Solution sol = pop.get(j);
                            instance.evaluate(sol);
                            if (!sol.violatesConstraints()) {
                                instance.normalize(sol);
                                ref_pop.add(sol);
                            }
                        }
                        merger.add(label + "_seed" + i, ref_pop);
                    }
                }
                catch (FrameworkException fe) {     // FIXME: replace with more useful exception class
                    throw new RuntimeException("error reading population from " + path, fe);
                }
                finally {
                    reader.close();
                }
            }
        }
        // Sanity check in order to avoid errors in the MOEA Framework
        NondominatedPopulation ref_set = merger.getCombinedPopulation();
        boolean add_default = ref_set.size() <= 1;
        for (int i = 0; i < instance.getNumberOfObjectives() && !add_default; ++i) {
            double obj_val = ref_set.get(0).getObjective(i);
            double obj_min = obj_val, obj_max = obj_val;
            for (int j = 1; j < ref_set.size(); ++j) {
                Solution sol = ref_set.get(j);
                obj_min = Math.min(obj_min, sol.getObjective(i));
                obj_max = Math.max(obj_max, sol.getObjective(i));
            }
            if (obj_max - obj_min < Settings.EPS) {     // error in IGD computation if an objective's range is
                add_default = true;                     // empty; in that case, add default reference set
            }
        }
        if (add_default) {
            ref_set.addAll(instance.getNormalizedDefaultReferenceSet());
        }
        File ref_set_file = IOUtils.makeTemporaryFile("ref_set", ".pop", true);
        instance.dumpReferenceSet(ref_set_file.getAbsolutePath(), ref_set);
        // Do analysis
        try {
            instance.analyzePopulations(dataset, ref_set_file.getAbsolutePath());
        }
        catch (FrameworkException fe) {
            throw new RuntimeException("Not possible to analyze populations for " + dataset_desc, fe);
        }
    }
    
    /**
     * Converts a set of populations in a given population file to the default encoding. The old file is
     * overwritten. It assumes that the original population is encoded with the binary integer encoding
     * ({@link VMCwMProblem.Encoding#BINARY_INTEGER}).
     * Older versions of VMAlloc did not enforce the default encoding when dumping populations to a file.
     * This method can be used to convert such files.
     * @param instance The problem instance.
     * @param pop_file_path The path to the population file.
     * @throws IOException If an error occurs reading from the population file.
     */
    private static void convertToDefaultEncoding(VMCwMProblem instance, String pop_file_path)
            throws IOException {
        instance.setEncoding(VMCwMProblem.Encoding.BINARY_INTEGER);
        File pop_file = new File(pop_file_path);
        File tmp_pop_file = IOUtils.makeTemporaryFile(pop_file_path, "", true);
        tmp_pop_file.delete(); // FileUtils.moveFile requires the destination file to not exist
        FileUtils.moveFile(pop_file, tmp_pop_file);
        ResultFileReader reader = new ResultFileReader(instance, tmp_pop_file);
        List<NondominatedPopulation> populations = new LinkedList<NondominatedPopulation>();
        while (reader.hasNext()) {
            populations.add(reader.next().getPopulation());
        }
        instance.dumpPopulations(pop_file_path, populations);
        reader.close();
    }
    
    /**
     * Dumps a given VMC instance to a given file as a Multi-Objective Combinatorial Optimization problem in
     * OPB format.
     * @param instance The VMC instance.
     * @param path The file's path.
     * @param ign_den Boolean indicating if denominators should be ignored.
     */
    // FIXME: requires a dummy instance of MultiObjectiveConstraintBasedAllocAlgorithm
    private static void dumpMOCO(VMCwMProblem instance, String path, boolean ign_den) {
        ParetoCLD printer = new ParetoCLD(instance);
        if (ign_den) printer.ignoreDenominators();
        printer.dumpMOCO(path);
    }
    
    /**
     * The application's entry point.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        Clock.getInstance().reset();
        System.out.println("c Parsing");
        Options options = new Options();
        options.addOption("a", "algorithm", true,
                          "Choose the allocation algorithm. Options are Linear Search (" +
                          LINEAR_SEARCH + "), Minimum Correction Set (" + MCS + "), " +
                          "Pseudo-Boolean Optimization (" + PBO + "), First-Fit Decreasing (" + FFD +
                          "), Best-Fit Decreasing (" + BFD + "), Differential Evolution (" +
                          DE + "), Genetic Algorithm (" + GA + "), Decomposition-Based Evolutionary " +
                          "Algorithm (" + MOEAD + "), Biogeography-Based Optimization (" + BBO +
                          "), Grouping Genetic Algorithm (" + GGA + "), Guided Improvement Algorithm (" +
                          GIA + "), Hash-based Enumeration (" + HASH + "), Pareto CLD (" + PARETO_CLD +
                          ") and Pareto LBX (" + PARETO_LBX + "). Default is " + PARETO_CLD + ".");
        options.addOption("t", "time-limit", true,
                          "Set the time limit in seconds. No limit by default.");
        options.addOption("m", "migration-percentile", true,
                          "Set the fraction of total memory capacity that can be used up in " +
                          "migrations. Default is " + DEFAULT_MIG_PERCENTILE + ".");
        options.addOption("r", "heuristic-reduction", false, "Enable heuristic reduction.");
        options.addOption("ra", "reduction-algorithm", true,
                          "Choose the allocation algorithm used for heuristic reduction. Options " +
                          "are " + FFD + " and " + BFD + ". Default is " + BFD + ".");
        options.addOption("s", "break-symmetries", false, "Enable symmetry breaking.");
        options.addOption("ip", "ignore-platform", false,
                          "Ignore platform specific restrictions.");
        options.addOption("ic", "ignore-colocation", false, "Ignore anti-colocation constraints.");
        options.addOption("ide", "ignore-den-eval", false,
                          "Ignore objective function denominators when evaluating solution quality.");
        options.addOption("ida", "ignore-den-alloc", false,
                          "Ignore objective function denominators when searching for optimal allocations. " +
                          "Supported by " + PARETO_CLD + ", " + PARETO_LBX + " and " + GIA + ".");
        options.addOption("h", "enable-hash", false,
                          "Enables hash functions for hash-based algorithms. Supported algorithms are "
                          + HASH + ", " + GIA + ", " + MCS + " and " + PARETO_CLD + ".");
        options.addOption("pd", "enable-path-diversification", false,
                          "Enables path diversification. Supported by " + PARETO_CLD + ".");
        options.addOption("st", "stratify", true,
                          "Enables stratification with the given strategy for handling division reduction. " +
                          "Options are merging reduced objectives (" + MERGED_STRAT + ") and probability " +
                          "splitting (" + SPLIT_STRAT + "). Supported by " + PARETO_CLD + ".");
        options.addOption("lwr", "literal-weight-ratio", true,
                          "Sets the literals to distinct weights ratio for stratification.");
        options.addOption("pn", "partition-number", true,
                          "Sets the number of partitions per objective for stratification. Overrides " +
                          "literal to weight ratio.");
        options.addOption("pa", "print-allocations", false, "Enable solution printing.");
        options.addOption("it", "initialization-type", true,
                          "Attempts to force initialization of population in evolutionary " +
                          "algorithms with feasible solutions using the given approach. Options " +
                          "are Random (" + RAND_INIT + "), Random Bin-Packing (" + RAND_PACKING_INIT +
                          "), Shuffled First-Fit (" + SHUFFLED_FIRST_FIT_INIT + "), Shuffled " +
                          "VMCwM Heuristic (" + SHUFFLED_VMCWM_INIT + ") and Mixed Initialization " +
                          "(" + MIXED_INIT + "). Default is " + RAND_INIT + ".");
        options.addOption("dp", "dump-population", true,
                          "Dump the final population to the given file. " +
                          "This option exists mostly for evaluation purposes.");
        options.addOption("dm", "dump-moco", true,
                          "Dump the instance encoded in multi-objective OPB format.");
        options.addOption("ap", "analyze-populations", true,
                          "Enables analysis of sets of populations stored in files generated using " +
                          "the 'dp' option instead of computing an allocation. The population " +
                          "files are specified by providing a list of <label>:<file_path> pairs, " +
                          "separated by ';'. This option exists mostly for evaluation purposes.");
        options.addOption("cp", "convert-population", true,
                          "Converts a given population file to the default encoding. This option is meant " +
                          "for converting population files produced by older versions of VMAlloc, when a " +
                          "default encoding was not enforced when dumping populations to files.");
        options.addOption("lp", "log-progress", true,
                          "Logs the objective values of the population to the given file whenever a new " +
                          "solution is found. This option exists mostly for evaluation purposes.");
        options.addOption("ms", "multiple-seeds", true,
                          "Runs the selected algorithm multiple times with the given number of " +
                          "different seeds. The timeout is reset between runs. This option " +
                          "exists mostly for evaluation purposes.");
        options.addOption("ps", "population-size", true,
                          "Set the population size for evolutionary algorithms. Default is " +
                          DEFAULT_POPSIZE + ". For " + BBO + ", this sets the population size per " +
                          "subsystem. In this case, default is " + DEFAULT_BBO_POPSIZE + ".");
        options.addOption("cr", "crossover-rate", true,
                          "Set the crossover rate for the " + DE + ", " + MOEAD + ", " + GA + " and " + GGA +
                          " algorithms. Default values are " + DEFAULT_DE_CROSSOVER_RATE + ", " +
                          DEFAULT_MOEAD_CROSSOVER_RATE + ", " + DEFAULT_GA_CROSSOVER_RATE + " and " +
                          DEFAULT_GGA_CROSSOVER_RATE + " respectively.");
        options.addOption("ss", "step-size", true,
                          "Set the step size for the " + DE + " algorithm. Default is " +
                          DEFAULT_DE_STEP_SIZE + ".");
        options.addOption("mr", "mutation-rate", true,
                          "Set the mutation rate for the " + BBO + ", " + GA + ", " + MOEAD + " and " + GGA +
                          " algorithms. " + BBO + ", " + MOEAD + " and " + GA + " use uniform single variable " +
                          "mutation and the default values are " + DEFAULT_BBO_MUTATION_RATE + ", " +
                          DEFAULT_MOEAD_MUTATION_RATE + " and " + DEFAULT_GA_MUTATION_RATE + " respectively. " +
                          GGA + " uses group mutation and the default value is " + DEFAULT_GGA_MUTATION_RATE + ".");
        options.addOption("ir", "immigration-rate", true,
                          "Set the immigration rate for the " + BBO + " algorithm. Default is " +
                          DEFAULT_BBO_IMMIGRATION_RATE + ".");
        options.addOption("cmr", "cross-migration-rate", true,
                          "Set the cross migration rate for the " + BBO + " algorithm. Default is " +
                          DEFAULT_BBO_CROSS_MIGRATION_RATE + ".");
        options.addOption("ns", "neighborhood-size", true,
                          "Set the neighborhood size for the " + MOEAD + " algorithm as a fraction of the " +
                          "population size. Default is " + DEFAULT_MOEAD_NEIGHBORHOOD_SIZE);
        options.addOption("delta", true,
                          "Set the probability of crossover with an individual from the neighborhood versus " +
                          "the entire population in the " + MOEAD + " algorithm. Default is " +
                          DEFAULT_MOEAD_DELTA);
        options.addOption("eta", true,
                          "Set the maximum number of individuals that can be replaced by a new solution, in " +
                          "the " + MOEAD + " algorithm, as a fraction of the population size. Default is " +
                          DEFAULT_MOEAD_ETA);
        CommandLineParser cl_parser = new DefaultParser();
        try {
            CommandLine cl = cl_parser.parse(options, args);
            InputParser in_parser = new InputParser(cl.getArgs()[0]);
            in_parser.parse();
            System.out.println("c Parsing time: " + Clock.getInstance().getElapsed() + " seconds");
            PhysicalMachineVec pms = in_parser.getPhysicalMachines();
            JobVec jobs = in_parser.getJobs();
            MappingVec mappings = in_parser.getMappings();
            double max_mig_percentile = Double.parseDouble(cl.getOptionValue("m", DEFAULT_MIG_PERCENTILE));
            if (cl.hasOption("ip")) {
                System.out.println("c Discarding platform constraints");
                discardPlatformConstraints(jobs);
            }
            if (cl.hasOption("ic")) {
                System.out.println("c Discarding anti-colocation constraints");
                discardAntiColocationConstraints(jobs);
            }
            VMCwMProblem orig_instance = new VMCwMProblem(pms, jobs, mappings, max_mig_percentile);
            ProblemStatistics stats = new ProblemStatistics(orig_instance);
            stats.printStatistics();
            VMCwMProblem instance = orig_instance;
            VMCwMProblemProvider instance_provider =
                    new VMCwMProblemProvider(pms, jobs, mappings, max_mig_percentile);
            if (cl.hasOption("ide")) {
                System.out.println("c Discarding objective function denominators from solution evaluation");
                orig_instance.discardDenominators();
                instance.discardDenominators();
                instance_provider.discardDenominators();
            }
            if (cl.hasOption("dm")) {
                dumpMOCO(instance, cl.getOptionValue("dm"), cl.hasOption("ida"));
                return;
            }
            if (cl.hasOption("r")) {
                AllocAlgorithm reduction_alg = null;
                if (!cl.hasOption("ra") || cl.getOptionValue("ra").equals(BFD)) {
                    reduction_alg = new BestFitDecreasingAlloc(instance);
                }
                else if (cl.getOptionValue("ra").equals(FFD)) {
                    reduction_alg = new FirstFitDecreasingAlloc(instance);
                }
                else {
                    printHelpMessage(options);
                    return;
                }
                System.out.println("c Applying heuristic reduction");
                HeuristicReducer reducer = new HeuristicReducer(instance, reduction_alg);
                try {
                    reducer.apply();
                    if (reducer.getReducedInstance().getPhysicalMachines().size() < pms.size()) {
                        instance = reducer.getReducedInstance();
                    }
                    System.out.println("c Solution using " + instance.getPhysicalMachines().size() +
                                       " PMs found");
                    System.out.println("c Elapsed time: " + Clock.getInstance().getElapsed() + " seconds");
                    stats = new ProblemStatistics(instance);
                    stats.printStatistics();
                }
                catch (HeuristicReductionFailedException e) {
                    System.out.println("c Heuristic reduction failed");
                }
            }
            ProblemFactory.getInstance().addProvider(instance_provider); // necessary in order to perform analysis
            if (cl.hasOption("ap")) {
                analyze(instance, cl.getOptionValue("ap"));
                return;
            }
            if (cl.hasOption("cp")) {
                convertToDefaultEncoding(instance, cl.getOptionValue("cp"));
                return;
            }
            AllocAlgorithm alloc = null;
            if (cl.getOptionValue("a").equals(LINEAR_SEARCH)) {
                alloc = new LinearSearchAlloc(instance);
            }
            else if (cl.getOptionValue("a").equals(PBO)) {
                alloc = new PBOAlloc(instance);
            }
            else if (cl.getOptionValue("a").equals(FFD)) {
                alloc = new FirstFitDecreasingAlloc(instance);
            }
            else if (cl.getOptionValue("a").equals(BFD)) {
                alloc = new BestFitDecreasingAlloc(instance);
            }
            else if (!cl.hasOption("a")||
                     cl.getOptionValue("a").equals(HASH) ||
                     cl.getOptionValue("a").equals(GIA) ||
                     cl.getOptionValue("a").equals(MCS) ||
                     cl.getOptionValue("a").equals(PARETO_CLD) ||
                     cl.getOptionValue("a").equals(PARETO_LBX)) {
                ConstraintBasedAllocAlgorithm cb_alloc = null;
                if (cl.getOptionValue("a").equals(HASH)) {
                    cb_alloc = new HashEnumAlloc(instance);
                    System.out.println("c ========= HE Configuration =========");
                }
                else if (cl.getOptionValue("a").equals(MCS)) {
                    cb_alloc = new MCSAlloc(instance);
                    System.out.println("c ======== MCS Configuration =========");
                }
                else {
                    MultiObjectiveConstraintBasedAllocAlgorithm mo_alloc = null;
                    if (!cl.hasOption("a") || cl.getOptionValue("a").equals(PARETO_CLD)) {
                        mo_alloc = new ParetoCLD(instance);
                        System.out.println("c ======== PCLD Configuration ========");
                        if (cl.hasOption("pd")) {
                            System.out.println("c  Path Diversification:  enabled");
                            mo_alloc.enablePathDiversification();
                        }
                        else {
                            System.out.println("c  Path Diversification:  disabled");
                        }
                        if (cl.hasOption("st")) {
                            System.out.println("c  Stratification:        " + cl.getOptionValue("st"));
                            mo_alloc.enableStratification(cl.getOptionValue("st").equals(MERGED_STRAT));
                            if (cl.hasOption("pn")) {
                                int partitions = Integer.parseInt(cl.getOptionValue("pn"));
                                mo_alloc.setFixedPartitionNumber(partitions);
                                System.out.println("c  Partitions:            " + partitions);
                            }
                            else {
                                double lw_ratio =
                                        Double.parseDouble(cl.getOptionValue("lwr", DEFAULT_LIT_WEIGHT_RATIO));
                                mo_alloc.setLitWeightRatio(lw_ratio);
                                System.out.println("c  Literal-Weight Ratio:  " + lw_ratio);
                            }
                        }
                        else {
                            System.out.println("c  Stratification:        disabled");
                        }
                    }
                    else if (cl.getOptionValue("a").equals(PARETO_LBX)) {
                        mo_alloc = new ParetoLBX(instance);
                        System.out.println("c ======== PLBX Configuration ========");
                    }
                    else if (cl.getOptionValue("a").equals(GIA)) {
                        mo_alloc = new GIAAlloc(instance);
                        System.out.println("c ======== GIA Configuration =========");
                    }
                    if (cl.hasOption("ida")) {
                        System.out.println("c  Denominators:          ignored");
                        mo_alloc.ignoreDenominators();
                    }
                    else {
                        System.out.println("c  Denominators:          reduced");
                    }
                    cb_alloc = mo_alloc;
                }
                if (cl.hasOption("h")) { // FIXME: PLBX does not support hash functions
                    cb_alloc.enableHashFunctions();
                    System.out.println("c  Hash Functions:        enabled");
                }
                else {
                    System.out.println("c  Hash Functions:        disabled");
                }
                if (cl.hasOption("s")) {
                    System.out.println("c  Symmetry Breaking:     enabled");
                    cb_alloc.enableSymmetryBreaking();
                }
                else {
                    System.out.println("c  Symmetry Breaking:     disabled");
                }
                System.out.println("c ====================================");
                alloc = cb_alloc;
            }
            else {
                // Evolutionary approaches
                EvolutionaryAllocAlgorithm ea_alloc = null;
                if (cl.getOptionValue("a").equals(DE)) {
                    DEAlloc de_alloc = new DEAlloc(instance);
                    double cr = Double.parseDouble(cl.getOptionValue("cr", DEFAULT_DE_CROSSOVER_RATE));
                    double ss = Double.parseDouble(cl.getOptionValue("ss", DEFAULT_DE_STEP_SIZE));
                    de_alloc.setCrossoverRate(cr);
                    de_alloc.setStepSize(ss);
                    ea_alloc = de_alloc;
                    System.out.println("c ========= DE Configuration =========");
                    System.out.println("c  Crossover rate:       " + cr);
                    System.out.println("c  Step size:            " + ss);
                }
                else if (cl.getOptionValue("a").equals(BBO)) {
                    BBOAlloc bbo_alloc = new BBOAlloc(instance);
                    System.out.println("c ========= BBO Configuration ========");
                    double mr = Double.parseDouble(cl.getOptionValue("mr", DEFAULT_BBO_MUTATION_RATE));
                    double ir = Double.parseDouble(
                            cl.getOptionValue("ir", DEFAULT_BBO_IMMIGRATION_RATE));
                    double cmr = Double.parseDouble(
                            cl.getOptionValue("cmr", DEFAULT_BBO_CROSS_MIGRATION_RATE));
                    bbo_alloc.setMutationRate(mr);
                    bbo_alloc.setImmigrationRate(ir);
                    bbo_alloc.setCrossSystemMigrationRate(cmr);
                    ea_alloc = bbo_alloc;
                    System.out.println("c  Immigration rate:     " + ir);
                    System.out.println("c  Mutation rate:        " + mr);
                    System.out.println("c  Cross migration rate: " + cmr);
                    // FIXME: needs to be corrected if BBO implementation changes
                    System.out.println("c  Number of subsystems: " + ((mappings.size() > 0) ? "6" : "4"));
                }
                else if (cl.getOptionValue("a").equals(MOEAD)) {
                    MOEADAlloc moead_alloc = new MOEADAlloc(instance);
                    System.out.println("c ======== MOEAD Configuration =======");
                    double mr = Double.parseDouble(cl.getOptionValue("mr", DEFAULT_MOEAD_MUTATION_RATE));
                    double cr = Double.parseDouble(cl.getOptionValue("cr", DEFAULT_MOEAD_CROSSOVER_RATE));
                    double ns = Double.parseDouble(cl.getOptionValue("ns", DEFAULT_MOEAD_NEIGHBORHOOD_SIZE));
                    double delta = Double.parseDouble(cl.getOptionValue("delta", DEFAULT_MOEAD_DELTA));
                    double eta = Double.parseDouble(cl.getOptionValue("eta", DEFAULT_MOEAD_ETA));
                    moead_alloc.setMutationRate(mr);
                    moead_alloc.setCrossoverRate(cr);
                    moead_alloc.setNeighborhoodSize(ns);
                    moead_alloc.setDelta(delta);
                    moead_alloc.setEta(eta);
                    System.out.println("c  Crossover rate:       " + cr);
                    System.out.println("c  Mutation rate:        " + mr);
                    System.out.println("c  Neighborhood size:    " + ns);
                    System.out.println("c  Delta:                " + delta);
                    System.out.println("c  Eta:                  " + eta);
                    ea_alloc = moead_alloc;
                }
                else if (cl.getOptionValue("a").equals(GA)) {
                    GAAlloc ga_alloc = new GAAlloc(instance);
                    double mr = Double.parseDouble(cl.getOptionValue("mr", DEFAULT_GA_MUTATION_RATE));
                    double cr = Double.parseDouble(cl.getOptionValue("cr", DEFAULT_GA_CROSSOVER_RATE));
                    ga_alloc.setMutationRate(mr);
                    ga_alloc.setCrossoverRate(cr);
                    System.out.println("c ========= GA Configuration =========");
                    System.out.println("c  Crossover rate:       " + cr);
                    System.out.println("c  Mutation rate:        " + mr);
                    ea_alloc = ga_alloc;
                }
                else if (cl.getOptionValue("a").equals(GGA)) {
                    GGAAlloc gga_alloc = new GGAAlloc(instance);
                    double mr = Double.parseDouble(cl.getOptionValue("mr", DEFAULT_GGA_MUTATION_RATE));
                    double cr = Double.parseDouble(cl.getOptionValue("cr", DEFAULT_GGA_CROSSOVER_RATE));
                    gga_alloc.setMutationRate(mr);
                    gga_alloc.setCrossoverRate(cr);
                    ea_alloc = gga_alloc;
                    System.out.println("c ========= GGA Configuration ========");
                    System.out.println("c  Crossover rate:       " + cr);
                    System.out.println("c  Mutation rate:        " + mr);
                }
                else {
                    printHelpMessage(options);
                    return;
                }
                String default_pop_size = null;
                if (cl.getOptionValue("a").equals(BBO)) {
                    default_pop_size = DEFAULT_BBO_POPSIZE;
                }
                else {
                    default_pop_size = DEFAULT_POPSIZE;
                }
                int ps = Integer.parseInt(cl.getOptionValue("ps", default_pop_size));
                ea_alloc.setPopulationSize(ps);
                System.out.println("c  Population size:      " + ps);
                System.out.print("c  Initialization type:  ");
                InitializationType init_type;
                if (!cl.hasOption("it") || cl.getOptionValue("it").equals(RAND_INIT)) {
                    init_type = InitializationType.RANDOM;
                    System.out.print(RAND_INIT + IOUtils.NEW_LINE);
                }
                else if (cl.getOptionValue("it").equals(RAND_PACKING_INIT)) {
                    init_type = InitializationType.RANDOM_PACKING;
                    System.out.print(RAND_PACKING_INIT + IOUtils.NEW_LINE);
                }
                else if (cl.getOptionValue("it").equals(SHUFFLED_FIRST_FIT_INIT)) {
                    init_type = InitializationType.SHUFFLED_FIRST_FIT;
                    System.out.print(SHUFFLED_FIRST_FIT_INIT + IOUtils.NEW_LINE);
                }
                else if (cl.getOptionValue("it").equals(SHUFFLED_VMCWM_INIT)) {
                    init_type = InitializationType.SHUFFLED_VMCWM_HEURISTIC;
                    System.out.print(SHUFFLED_VMCWM_INIT + IOUtils.NEW_LINE);
                }
                else if (cl.getOptionValue("it").equals(MIXED_INIT)) {
                    init_type = InitializationType.MIXED;
                    System.out.print(MIXED_INIT + IOUtils.NEW_LINE);
                }
                else {
                    printHelpMessage(options);
                    return;
                }
                System.out.println("c ====================================");
                ea_alloc.setInitializationType(init_type);
                alloc = ea_alloc;
            }
            if (cl.hasOption("lp")) {
                System.out.println("c Logging progress to " + cl.getOptionValue("lp"));
                alloc.enableProgressLog(cl.getOptionValue("lp"));
            }
            if (cl.hasOption("t")) {
                alloc.setTimeout(Integer.parseInt(cl.getOptionValue("t")));
            }
            if (cl.hasOption("ms")) {
                alloc.allocateMultipleSeeds(Integer.parseInt(cl.getOptionValue("ms")));
            }
            else {
                alloc.allocate();
            }
            if (alloc.foundSolution()) {
                System.out.println("s SUCCESS");
                NondominatedPopulation solutions = alloc.getSolutions();
                double[] energy_costs = VMCwMProblem.getEnergyCosts(solutions);
                double[] wastage_costs = VMCwMProblem.getWastageCosts(solutions);
                double[] migration_costs = null;
                if (mappings.size() > 0) {
                    migration_costs = VMCwMProblem.getMigrationCosts(solutions);
                    assert(energy_costs.length == migration_costs.length);
                }
                assert(energy_costs.length == wastage_costs.length);
                for (int i = 0; i < energy_costs.length; ++i) {
                    if (mappings.size() > 0) {
                        System.out.printf("e %.5f \tw %.5f \tm %d\n",
                                          energy_costs[i], wastage_costs[i], (long)migration_costs[i]);
                    }
                    else {
                        System.out.printf("e %.5f \tw %.5f\n",
                                          energy_costs[i], wastage_costs[i]);
                    }
                }
                if (cl.hasOption("pa")) {
                    IVec<MappingVec> allocations = alloc.getAllocations();
                    for (int i = 0; i < allocations.size(); ++i) {
                        assert(MiscUtils.allocationIsValid(orig_instance, allocations.get(i)));
                        System.out.println("s SOLUTION " + i);
                        printAllocation(allocations.get(i));
                    }
                }
            }
            else {
                System.out.println("s FAILURE");
            }
            // FIXME: maybe should check if solution was found first?
            if (cl.hasOption("dp")) {
                instance.dumpPopulations(cl.getOptionValue("dp"), alloc.getPopulations());
            }
        }
        catch (ParseException e) {
            printHelpMessage(options);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            printHelpMessage(options);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("c PARSING ERROR!");
        }
    }

}
