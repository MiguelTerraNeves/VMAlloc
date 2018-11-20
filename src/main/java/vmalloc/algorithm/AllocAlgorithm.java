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

package vmalloc.algorithm;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.Clock;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.MathUtils;

/**
 * Abstract superclass for Virtual Machine Consolidation problem solvers.
 * @author Miguel Terra-Neves
 */
public abstract class AllocAlgorithm {
    
    /**
     * Integer constant used to represent that no timeout was set.
     */
    public static final long NO_TIMEOUT = -1;

    /**
     * Time instant from which calls to {@link #allocate()} must terminate. If running with multiple seeds
     * (see {@link #runMultipleSeeds(int)}), then the i-th run must terminate after {@code timeout*(i+1)}
     * seconds. If {@code timeout} is set to {@link #NO_TIMEOUT}, then no time limit is imposed.
     */
    private long timeout = NO_TIMEOUT;
    
    /**
     * Stores the best energy consumption objective cost found so far.
     */
    private double best_energy = Double.MAX_VALUE;
    
    /**
     * Stores the best resource wastage objective cost found so far.
     */
    private double best_wastage = Double.MAX_VALUE;
    
    /**
     * Stores the best migration objective cost found so far.
     */
    private long best_mig_cost = Long.MAX_VALUE;
    
    /**
     * Writer object for logging population objective cost evolution over time.
     */
    private Writer progress_writer = null;
    
    /**
     * The Virtual Machine Consolidation problem instance.
     */
    protected final VMCwMProblem instance;
    
    /**
     * Stores the solution set computed by the algorithm on the last call to {@link #allocate()} or
     * {@link #allocateMultipleSeeds(int)}.
     */
    protected NondominatedPopulation solutions = null;
    
    /**
     * Stores the solution sets computed for each individual seed on the last call to
     * {@link #allocateMultipleSeeds(int)}.
     */
    protected List<NondominatedPopulation> results = null;
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver.
     * @param instance The problem instance.
     */
    public AllocAlgorithm(VMCwMProblem instance) {
        this(instance, VMCwMProblem.Encoding.INTEGER);
    }
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses a given solution
     * encoding type.
     * @param instance The problem instance.
     * @param encoding The encoding type.
     */
    protected AllocAlgorithm(VMCwMProblem instance, VMCwMProblem.Encoding encoding) {
        this.instance = instance;
        this.instance.setEncoding(encoding);
        this.solutions = new NondominatedPopulation();
        this.results = new LinkedList<NondominatedPopulation>();
    }

    /**
     * Adds the contents of a physical machine vector to a set of physical machines.
     * @param vec The physical machine vector.
     * @param set The set of physical machines.
     */
    protected void addVecToSet(PhysicalMachineVec vec, Set<PhysicalMachine> set) {
        for (int i = 0; i < vec.size(); ++i) {
            set.add(vec.get(i));
        }
    }
    
    /**
     * Normalizes a set of resource requirement values in respect to a given resource capacity.
     * @param reqs An array with the resource values to normalize.
     * @param cap The capacity values agaisn't which to normalize.
     * @return An array with the normalized resource values.
     */
    private double[] normalizeRequirements(BigInteger[] reqs, BigInteger cap) {
        double[] norm_reqs = new double[reqs.length];
        for (int i = 0; i < reqs.length; ++i) {
            norm_reqs[i] = MathUtils.divideBigIntegers(reqs[i], cap, RoundingMode.HALF_EVEN).doubleValue();
        }
        return norm_reqs;
    }
    
    /**
     * Retrieves the CPU requirements of a set of virtual machines normalized in respect to a given
     * physical machine.
     * @param vms The virtual machines.
     * @param pm The physical machine.
     * @return An array with the normalized CPU requirement values.
     */
    protected double[] getNormalizedCPURequirements(VirtualMachineVec vms, PhysicalMachine pm) {
        return normalizeRequirements(vms.getCPUs(), pm.getCPU());
    }
    
    /**
     * Retrieves the memory requirements of a set of virtual machines normalized in respect to a given
     * physical machine.
     * @param vms The virtual machines.
     * @param pm The physical machine.
     * @return An array with the normalized memory requirement values.
     */
    protected double[] getNormalizedMemoryRequirements(VirtualMachineVec vms, PhysicalMachine pm) {
        return normalizeRequirements(vms.getMemories(), pm.getMemory());
    }

    /**
     * Computes the intersection of the physical machines in which two virtual machines can both run.
     * @param pms The physical machines.
     * @param vm1 A virtual machine.
     * @param vm2 Another virtual machine.
     * @return A vector with the indexes of the physical machines in {@code pms} on which both {@code vm1}
     * and {@code vm2} can run.
     */
    protected IVecInt intersectAllowedPhysicalMachineIndexes(PhysicalMachineVec pms,
                                                             VirtualMachine vm1,
                                                             VirtualMachine vm2) {
        IVecInt indexes = new VecInt();
        Set<PhysicalMachine> vm1_unallowed = new HashSet<PhysicalMachine>();
        Set<PhysicalMachine> vm2_unallowed = new HashSet<PhysicalMachine>();
        addVecToSet(vm1.getUnallowedPhysicalMachines(), vm1_unallowed);
        addVecToSet(vm2.getUnallowedPhysicalMachines(), vm2_unallowed);
        for (int i = 0; i < pms.size(); ++i) {
            if (!vm1_unallowed.contains(pms.get(i)) && !vm2_unallowed.contains(pms.get(i))) {
                indexes.push(i);
            }
        }
        return indexes;
    }
    
    /**
     * Checks if two virtual machines are mapped to the same physical machine in a given virtual machine to
     * physical machine map.
     * @param vm1 A virtual machine.
     * @param vm2 Another virtual machine.
     * @param mapping_map A map object with virtual machine to physical machine mappings.
     * @return True if {@code vm1} and {@code vm2} are mapped to the same physical machine in
     * {@code mapping_map}, false otherwise.
     */
    protected boolean mappedToSamePhysicalMachine(VirtualMachine vm1,
                                                  VirtualMachine vm2,
                                                  Map<VirtualMachine, PhysicalMachine> mapping_map) {
        PhysicalMachine pm1 = null;
        PhysicalMachine pm2 = null;
        if (mapping_map.containsKey(vm1)) {
            pm1 = mapping_map.get(vm1);
        }
        if (mapping_map.containsKey(vm2)) {
            pm2 = mapping_map.get(vm2);
        }
        return (pm1 == null && pm2 == null) ||
               (pm1 != null && pm2 != null && pm1.getID() == pm2.getID());
    }
    
    /**
     * Retrieves a copy of a vector of physical machines sorted according to a given criteria.
     * @param pms The physical machines.
     * @param comp A comparator that implements the sorting criteria.
     * @return An array with the physical machines sorted.
     */
    protected PhysicalMachine[] sortedPhysicalMachines(PhysicalMachineVec pms,
                                                       Comparator<PhysicalMachine> comp) {
        PhysicalMachine[] pm_array = new PhysicalMachine[pms.size()];
        pms.copyTo(pm_array);
        Arrays.sort(pm_array, comp);
        return pm_array;
    }
    
    /**
     * Converts an allocation (vector of virtual machine to physical machine mappings) to an equivalent map
     * of virtual machines to physical machines.
     * @param allocation The allocation.
     * @return A map of virtual machines to physical machines equivalent to {@code allocation}.
     */
    protected Map<VirtualMachine, PhysicalMachine> allocationToMap(MappingVec allocation) {
        Map<VirtualMachine, PhysicalMachine> map = new HashMap<VirtualMachine, PhysicalMachine>();
        for (int i = 0; i < allocation.size(); ++i) {
            Mapping mapping = allocation.get(i);
            map.put(mapping.getVirtualMachine(), mapping.getPhysicalMachine());
        }
        return map;
    }

    /**
     * Retrieves the remaining time allotted for calls to {@link #allocate()}. Default value is
     * {@link Long#MAX_VALUE}.
     * @return The remaining time in seconds.
     */
    protected long getRemainingTime() {
        if (this.timeout == NO_TIMEOUT) {
            return Long.MAX_VALUE; // FIXME: implicit limit, should change eventually
        }
        assert(this.timeout >= 0);
        return Math.max(this.timeout - (long)Clock.getInstance().getElapsed(), 0);
    }
    
    /**
     * Retrieves the time instant from which calls to {@link #allocate()} must terminate. If no timeout was
     * set, {@link #NO_TIMEOUT} is returned.
     * @return The timeout if it was set, {@link #NO_TIMEOUT} otherwise.
     */
    protected long getTimeout() { return this.timeout; }
    
    /**
     * Updates the values of the best objective function costs found so far if a given triple of new values
     * improves on the current best costs.
     * @param energy The energy consumption cost.
     * @param wastage The resource wastage cost.
     * @param migration The migration cost.
     * @param print_new_best True if the new best objective costs are to be printed if the triple improves
     * on those values, false otherwise.
     */
    protected void updateBestObjectiveCosts(double energy,
                                            double wastage,
                                            long migration,
                                            boolean print_new_best) {
        boolean do_print = false;
        if (energy < this.best_energy) {
            this.best_energy = energy;
            do_print = true;
        }
        if (wastage < this.best_wastage) {
            this.best_wastage = wastage;
            do_print = true;
        }
        if (this.instance.getNumberOfObjectives() == 3 && migration < this.best_mig_cost) {
            this.best_mig_cost = migration;
            do_print = true;
        }
        if (print_new_best && do_print) {
            if (this.instance.getNumberOfObjectives() == 3) {
                System.out.printf("e %.5f \tw %.5f \tm %d\n",
                                  this.best_energy, this.best_wastage, (long)this.best_mig_cost);
            }
            else {
                System.out.printf("e %.5f \tw %.5f\n", this.best_energy, this.best_wastage);
            }
            printElapsedTime();
        }
    }
    
    
    /**
     * Updates the values of the best objective function costs found so far if a given solutions improves on
     * those values. If the solution is not feasible, then nothing happens.
     * @param solution The solution.
     * @param print_new_best True if the new best objective costs are to be printed if the solution improves
     * on those values, false otherwise.
     */
    protected void updateBestObjectiveCosts(Solution solution, boolean print_new_best) {
        if (!solution.violatesConstraints()) {
            updateBestObjectiveCosts(VMCwMProblem.getEnergyCost(solution),
                                     VMCwMProblem.getWastageCost(solution),
                                     (this.instance.getNumberOfObjectives() == 3) ?
                                             (long)VMCwMProblem.getMigrationCost(solution) : 0,
                                     print_new_best);
        }
    }
    
    /**
     * Enables population objective cost progress logging.
     * @param path The path to the log file.
     * @throws IOException If an error occurs opening the log file.
     */
    public void enableProgressLog(String path) throws IOException {
        if (this.progress_writer != null) {
            this.progress_writer.close();
        }
        this.progress_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
    }
    
    /**
     * Checks if population objective cost progress logging has been enabled.
     * @return True if logging is enabled, false otherwise.
     */
    public boolean progressLogEnabled() { return this.progress_writer != null; }
    
    /**
     * If population objective cost progress logging is enabled, adds a snapshot of the current population to
     * the log.
     */
    protected void logProgress() {
        if (progressLogEnabled()) {
            try {
                for (int i = 0; i < this.solutions.size(); ++i) {
                    Solution sol = this.solutions.get(i);
                    String str = String.format("%1$.5f %2$.5f",
                                               VMCwMProblem.getEnergyCost(sol),
                                               VMCwMProblem.getWastageCost(sol));
                    str += (this.instance.getNumberOfObjectives() == 3) ?
                           String.format(" %1$d\n", (long)VMCwMProblem.getMigrationCost(sol)) : "\n";
                    this.progress_writer.write(str);
                }
                this.progress_writer.write("#\n");  // FIXME: separator constant?
                this.progress_writer.flush();
            }
            catch (IOException e) {     // FIXME: convert to runtime exception?
                System.out.println("c Error occurred logging current population");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Evaluates and stores a solution in the solution set if it is feasible. If {@code print_objectives}
     * is true, then the best objective costs found so far are printed if the solution improves at least one
     * of them. If {@code disable_log} is true, the current population is not logged even if progress
     * logging is enabled.
     * @param solution The solution.
     * @param print_new_best True if the new best objective costs are to be printed if the solution improves
     * on those values, false otherwise.
     * @param disable_log True if progress logging is to be skipped, false otherwise.
     */
    protected void saveSolution(Solution solution, boolean print_new_best, boolean disable_log) {
        this.instance.evaluate(solution);
        if (!solution.violatesConstraints()) {
            this.solutions.add(solution);
            updateBestObjectiveCosts(solution, print_new_best);
            if (!disable_log) logProgress();
        }
    }
    
    /**
     * Evaluates and stores a solution in the solution set if it is feasible. If {@code print_objectives}
     * is true, then the best objective costs found so far are printed if the solution improves at least one
     * of them. If progress logging is enabled, the current population is logged.
     * @param solution The solution.
     * @param print_new_best True if the new best objective costs are to be printed if the solution improves
     * on those values, false otherwise.
     */
    protected void saveSolution(Solution solution, boolean print_new_best) {
        saveSolution(solution, print_new_best, false);
    }
    
    /**
     * Evaluates and stores a solution in the solution set if it is feasible.
     * @param solution The solution.
     */
    protected void saveSolution(Solution solution) {
        saveSolution(solution, false);
    }
    
    /**
     * Evaluates and stores an allocation (vector of virtual machine to physical machine mappings) in the
     * solution set if it is feasible. If {@code print_objectives} is true, then the best objective costs
     * found so far are printed if the solution improves at least one of them.
     * @param allocation The allocation.
     * @param print_objectives True if the best objective costs are to be printed, false otherwise.
     */
    protected void saveSolution(MappingVec allocation, boolean print_objectives) {
        saveSolution(this.instance.allocationToSolution(allocation), print_objectives);
    }
    
    /**
     * Evaluates and stores an allocation (vector of virtual machine to physical machine mappings) in the
     * solution set if it is feasible.
     * @param allocation The allocation.
     */
    protected void saveSolution(MappingVec allocation) {
        saveSolution(allocation, false);
    }
    
    /**
     * Clears the solution set.
     */
    protected void clearSolutions() { this.solutions.clear(); }

    /**
     * Prints the amount of time, in seconds, elapsed since the start of the application.
     */
    protected void printElapsedTime() {
        System.out.println("c Elapsed time: " + Clock.getInstance().getElapsed() + " seconds");
    }

    /**
     * Sets the time instant from which calls to {@link #allocate()} must terminate. If running with multiple
     * seeds (see {@link #allocateMultipleSeeds(int)}), then sets the amount of time allotted to each run of
     * the algorithm.
     * @param timeout The allotted time in seconds.
     */
    public void setTimeout(long timeout) { this.timeout = timeout; }
    
    /**
     * Checks if, on the last call to {@link #allocate()} or {@link #allocateMultipleSeeds(int)}, the
     * algorithm was able to find at least one solution.
     * @return True if a solution was found, false otherwise.
     */
    public boolean foundSolution() { return this.solutions.size() > 0; }
    
    /**
     * Retrieves the solution set computed by the algorithm on the last call to {@link #allocate()} or
     * {@link #allocateMultipleSeeds(int)}.
     * @return The solution set.
     */
    public NondominatedPopulation getSolutions() {
        return this.solutions;
    }
    
    /**
     * Retrieves the solution set computed by the algorithm on the last call to {@link #allocate()} or
     * {@link #allocateMultipleSeeds(int)}, as a set of allocations (vectors of virtual machine to
     * physical machine mappings).
     * @return The vector of allocations.
     */
    public IVec<MappingVec> getAllocations() {
        return this.instance.populationToAllocations(this.solutions);
    }
    
    /**
     * Retrieves the set of solution sets computed by the algorithms on the last call to
     * {@link #allocateMultipleSeeds(int)}.
     * @return The set of solution sets.
     */
    public List<NondominatedPopulation> getPopulations() {
        if (this.results == null || this.results.size() == 0) {
            assert(this.solutions != null);
            List<NondominatedPopulation> pops = new LinkedList<NondominatedPopulation>();
            pops.add(this.solutions);
            return pops;
        }
        return this.results;
    }

    /**
     * Runs the allocation algorithm.
     */
    public abstract void allocate();
    
    /**
     * Runs the allocation algorithm multiple times with different seeds. If a timeout was set, then the
     * i-th run must terminate after {@code timeout*(i+1)} seconds.
     * @param nseeds The number of seeds to use.
     * @return A list with {@code nseeds} populations with the solution sets for each seed.
     * @see #allocate()
     */
    protected List<NondominatedPopulation> runMultipleSeeds(int nseeds) {
        System.out.println("c Running with " + nseeds + " different seeds");
        List<NondominatedPopulation> results = new LinkedList<NondominatedPopulation>();
        long timeout = this.timeout;
        for (int i = 0; i < nseeds; ++i) {
            if (timeout != NO_TIMEOUT) {
                setTimeout((long)Clock.getInstance().getElapsed() + timeout);
            }
            allocate();
            results.add(this.solutions);
            this.solutions = new NondominatedPopulation();
        }
        setTimeout(timeout);
        return results;
    }
    
    /**
     * Runs the allocation algorithm multiple times with different seeds. If a timeout was set, then the
     * i-th run must terminate after {@code timeout*(i+1)} seconds.
     * @param nseeds The number of seeds to use.
     */
    public void allocateMultipleSeeds(int nseeds) {
        this.results = runMultipleSeeds(nseeds);
        int seed_idx = 0;
        for (Iterator<NondominatedPopulation> it = this.results.iterator(); it.hasNext();) {
            System.out.println("c Population obtained with seed " + ++seed_idx);
            NondominatedPopulation population = it.next();
            for (int i = 0; i < population.size(); ++i) {
                Solution solution = population.get(i);
                assert(!solution.violatesConstraints());
                this.solutions.add(solution);
                double energy = VMCwMProblem.getEnergyCost(solution);
                double wastage = VMCwMProblem.getWastageCost(solution);
                if (this.instance.getMappings().size() > 0) {
                    double migration = VMCwMProblem.getMigrationCost(solution);
                    System.out.printf("e %.5f \tw %.5f \tm %d\n", energy, wastage, (long)migration);
                }
                else {
                    System.out.printf("e %.5f \tw %.5f\n", energy, wastage);
                }
            }
        }
        System.out.println("c Done");
        printElapsedTime();
    }

}
