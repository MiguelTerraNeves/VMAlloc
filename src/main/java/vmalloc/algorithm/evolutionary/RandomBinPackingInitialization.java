package vmalloc.algorithm.evolutionary;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

import vmalloc.domain.VirtualMachine;

/**
 * This class implements an initialization operator for Virtual Machine Consolidation problems that performs
 * random bin-packing in order to produce a set of feasible solutions. When it is not possible to maintain
 * feasibility, virtual machines are mapped randomly while still satisfying anti-colocation and platform
 * constraints. Only supports Virtual Machine Consolidation problems.
 * @author Miguel Terra-Neves
 */
public class RandomBinPackingInitialization extends FeasibleInitialization {
    
    /**
     * The VMC problem instance.
     */
    private final VMCwMProblem instance;

    /**
     * Creates an instance of the random bin-packing initialization operator.
     * @param problem The problem instance.
     * @param populationSize The size of the population.
     */
    public RandomBinPackingInitialization(VMCwMProblem problem, int populationSize) {
        super(problem, populationSize);
        this.instance = problem;
    }
    
    /**
     * Attempts to initialize a solution as a feasible solution using random bin-packing. When it is not
     * possible to maintain feasibility, virtual machines are mapped randomly while still satisfying
     * anti-colocation and platform constraints.
     * @param solution The solution to initialize.
     */
    @Override
    protected void attemptFeasibleInitialization(Solution solution) {
        // Initialize
        BigInteger[] remaining_cpu_caps = new BigInteger[instance.getPhysicalMachines().size()];
        BigInteger[] remaining_mem_caps = new BigInteger[instance.getPhysicalMachines().size()];
        IVec<Set<Integer>> anti_coloc_jobs_per_pm =
                new Vec<Set<Integer>>(instance.getPhysicalMachines().size());
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            remaining_cpu_caps[i] = instance.getPhysicalMachines().get(i).getCPU();
            remaining_mem_caps[i] = instance.getPhysicalMachines().get(i).getMemory();
            anti_coloc_jobs_per_pm.push(new HashSet<Integer>());
        }
        // Place VMs randomly while trying to satisfy constraints
        VirtualMachine[] shuffled_vms = new VirtualMachine[instance.getVirtualMachines().size()];
        instance.getVirtualMachines().copyTo(shuffled_vms);
        PRNG.shuffle(shuffled_vms);
        for (int i = 0; i < shuffled_vms.length; ++i) {
            VirtualMachine vm = shuffled_vms[i];
            // Retrieve PM indexes that maintain feasibility
            IVec<Integer> selectable_idxs = new Vec<Integer>();
            for (int j = 0; j < instance.getPhysicalMachines().size(); ++j) {
                BigInteger rem_cpu = remaining_cpu_caps[j];
                BigInteger rem_mem = remaining_mem_caps[j];
                if (    vm.getCPU().compareTo(rem_cpu) <= 0 &&
                        vm.getMemory().compareTo(rem_mem) <= 0 &&
                        !(vm.isAntiColocatable() &&
                          anti_coloc_jobs_per_pm.get(j).contains(new Integer(vm.getJobID()))) &&
                        vm.canRunInPhysicalMachine(instance.getPhysicalMachines().get(j))) {
                    selectable_idxs.push(new Integer(j));
                }
            }
            // If not possible to maintain feasibility, retrieve allowed PM indexes without an
            // anti-colocatable VM
            if (selectable_idxs.size() == 0) {
                for (int j = 0; j < instance.getPhysicalMachines().size(); ++j) {
                    if (    !(vm.isAntiColocatable() &&
                              anti_coloc_jobs_per_pm.get(j).contains(new Integer(vm.getJobID()))) &&
                            vm.canRunInPhysicalMachine(instance.getPhysicalMachines().get(j))) {
                        selectable_idxs.push(new Integer(j));
                    }
                }
            }
            // Randomly select one of the selected PMs
            int pm_idx = selectable_idxs.get(PRNG.nextInt(selectable_idxs.size())).intValue();
            instance.setVirtualMachineMapping(solution, i, pm_idx);
            remaining_cpu_caps[pm_idx] = remaining_cpu_caps[pm_idx].subtract(vm.getCPU());
            remaining_mem_caps[pm_idx] = remaining_mem_caps[pm_idx].subtract(vm.getMemory());
            if (vm.isAntiColocatable()) {
                anti_coloc_jobs_per_pm.get(pm_idx).add(new Integer(vm.getJobID()));
            }
        }
    }
    
}
