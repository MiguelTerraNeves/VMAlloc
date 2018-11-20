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

package vmalloc.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;

/**
 * Class with several static utility methods and constants.
 * @author Miguel Terra-Neves
 */
public class MiscUtils {
    
    /**
     * Injects a (possibly partial) allocation into a solution object.
     * @param instance The problem instance.
     * @param solution The solution.
     * @param allocation The (possibly partial) allocation.
     * @param leftover_vms If {@code allocation} is a partial allocation, contains the virtual machines left
     * out of the allocation.
     */
    public static void injectPartialAllocationInSolution(VMCwMProblem instance,
                                                         Solution solution,
                                                         MappingVec allocation,
                                                         VirtualMachineVec leftover_vms) {
        IVec<Set<Integer>> anti_coloc_jobs_per_pm =
                new Vec<Set<Integer>>(instance.getPhysicalMachines().size());
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            anti_coloc_jobs_per_pm.push(new HashSet<Integer>());
        }
        // Set solution variables for VMs in partial allocation
        assert(allocation != null);
        for (int i = 0; i < allocation.size(); ++i) {
            VirtualMachine vm = allocation.get(i).getVirtualMachine();
            PhysicalMachine pm = allocation.get(i).getPhysicalMachine();
            int vm_idx = instance.getVirtualMachineIndex(vm);
            int pm_idx = instance.getPhysicalMachineIndex(pm);
            instance.setVirtualMachineMapping(solution, vm_idx, pm_idx);
            if (vm.isAntiColocatable()) {
                anti_coloc_jobs_per_pm.get(pm_idx).add(new Integer(vm.getJobID()));
            }
        }
        // Randomly place leftover VMs
        assert(leftover_vms != null);
        for (int i = 0; i < leftover_vms.size(); ++i) {
            VirtualMachine vm = leftover_vms.get(i);
            IVec<Integer> selectable_idxs = new Vec<Integer>();
            for (int j = 0; j < instance.getPhysicalMachines().size(); ++j) {
                if (    !(vm.isAntiColocatable() &&
                          anti_coloc_jobs_per_pm.get(j).contains(new Integer(vm.getJobID()))) &&
                        vm.canRunInPhysicalMachine(instance.getPhysicalMachines().get(j))) {
                    selectable_idxs.push(new Integer(j));
                }
            }
            int pm_idx = selectable_idxs.get(PRNG.nextInt(selectable_idxs.size())).intValue();
            instance.setVirtualMachineMapping(solution, i, pm_idx);
            if (vm.isAntiColocatable()) {
                anti_coloc_jobs_per_pm.get(pm_idx).add(new Integer(vm.getJobID()));
            }
        }
    }
    
    /**
     * Checks if a given allocation is a feasible allocation.
     * @param instance The problem instance.
     * @param allocation The allocation.
     * @return True if the allocation is feasible, false otherwise.
     */
    public static boolean allocationIsValid(VMCwMProblem instance, MappingVec allocation) {
        // Check if VMs are mapped to a single PM
        Set<String> mapped_vms = new HashSet<String>();
        for (int i = 0; i < allocation.size(); ++i) {
            String vm_id = allocation.get(i).getVirtualMachine().getID();
            if (mapped_vms.contains(vm_id)) {
                System.out.println("ERROR: VM " + vm_id + " placed in multiple PMs");
                return false;
            }
            mapped_vms.add(vm_id);
        }
        // Check if all VMs are mapped
        int nvms = 0;
        for (int i = 0; i < instance.getJobs().size(); ++i) {
            nvms += instance.getJobs().get(i).nVirtualMachines();
        }
        if (nvms != mapped_vms.size()) {
            System.out.println("ERROR: only " + mapped_vms.size() + " mapped out of a total of " + nvms);
            return false;
        }
        // Check if capacities are not exceeded
        Map<Integer, VirtualMachineVec> vms_per_pm = new HashMap<Integer, VirtualMachineVec>();
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            vms_per_pm.put(new Integer(instance.getPhysicalMachines().get(i).getID()),
                           new VirtualMachineVec());
        }
        for (int i = 0; i < allocation.size(); ++i) {
            Integer pm_id = new Integer(allocation.get(i).getPhysicalMachine().getID());
            VirtualMachine vm = allocation.get(i).getVirtualMachine();
            vms_per_pm.get(pm_id).push(vm);
        }
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            PhysicalMachine pm = instance.getPhysicalMachines().get(i);
            BigInteger cpu_usage = BigInteger.ZERO;
            BigInteger mem_usage = BigInteger.ZERO;
            IVec<VirtualMachine> vms = vms_per_pm.get(new Integer(pm.getID()));
            for (int j = 0; j < vms.size(); ++j) {
                cpu_usage = cpu_usage.add(vms.get(j).getCPU());
                mem_usage = mem_usage.add(vms.get(j).getMemory());
            }
            if (cpu_usage.compareTo(pm.getCPU()) > 0 || mem_usage.compareTo(pm.getMemory()) > 0) {
                System.out.println("ERROR: capacity exceeded for PM " + pm.getID());
                return false;
            }
        }
        // Check if anti-colocation is not violated
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            VirtualMachineVec vms =
                    vms_per_pm.get(new Integer(instance.getPhysicalMachines().get(i).getID()));
            Set<Integer> anti_coloc_jobs = new HashSet<Integer>();
            for (int j = 0; j < vms.size(); ++j) {
                VirtualMachine vm = vms.get(j);
                Integer job_id = new Integer(vm.getJobID());
                if (vm.isAntiColocatable()) {
                    if (anti_coloc_jobs.contains(job_id)) {
                        System.out.println("ERROR: anti-colocation violated for job " + job_id);
                        return false;
                    }
                    anti_coloc_jobs.add(job_id);
                }
            }
        }
        // Check if migration percentile is not exceeded
        Map<String, Integer> vm_to_pm = new HashMap<String, Integer>();
        for (int i = 0; i < allocation.size(); ++i) {
            vm_to_pm.put(allocation.get(i).getVirtualMachine().getID(),
                         new Integer(allocation.get(i).getPhysicalMachine().getID()));
        }
        BigDecimal total_mem_cap = BigDecimal.ZERO;
        for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
            total_mem_cap =
                    total_mem_cap.add(new BigDecimal(instance.getPhysicalMachines().get(i).getMemory()));
        }
        BigInteger max_mig_mem =
                total_mem_cap.multiply(new BigDecimal(instance.getMaxMigrationPercentile())).toBigInteger();
        BigInteger migged_mem = BigInteger.ZERO;
        for (int i = 0; i < instance.getMappings().size(); ++i) {
            VirtualMachine vm = instance.getMappings().get(i).getVirtualMachine();
            Integer prev_pm_id = new Integer(instance.getMappings().get(i).getPhysicalMachine().getID());
            if (!vm_to_pm.get(vm.getID()).equals(prev_pm_id)) {
                migged_mem = migged_mem.add(vm.getMemory());
            }
        }
        if (migged_mem.compareTo(max_mig_mem) > 0) {
            System.out.println("ERROR: migration budget exceeded");
            return false;
        }
        return true;
    }
    
}
