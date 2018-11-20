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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.moeaframework.core.PRNG;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.Machine;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.MemorySumAggregator;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.CollectionUtils;
import vmalloc.utils.MathUtils;

/**
 * Abstract superclass for Virtual Machine Consolidation problem solvers that rely on heuristics for
 * bin-packing. This class implements the general bin-packing heuristic method. Sub-classes only need to
 * implement the {@link #makeUsageInfoComparator()} comparator method in order to implement different
 * bin-packing policies. Bin-packing algorithms support multiple successive calls to the
 * {@link #allocate()} method with different pre-existing allocations and migration budget percentiles.
 * This algorithm ignores timeouts, since bin-packing heuristics are extremely fast.
 * @author Miguel Terra-Neves
 */
public abstract class BinPackingAllocAlgorithm extends AllocAlgorithm {
    
    /**
     * Container class used to store information on the resource load of a physical machine, like the virtual
     * machines mapped to it, their total CPU and memory requirements, etc.
     * @author Miguel Terra-Neves
     */
    protected class UsageInfo implements Iterable<VirtualMachine>, Comparable<UsageInfo> {
        
        /**
         * The physical machine for which the object is storing information.
         */
        private PhysicalMachine pm = null;
        
        /**
         * Set of virtual machines mapped to the physical machine {@link #pm}.
         */
        private Set<VirtualMachine> mapped_vms = new HashSet<VirtualMachine>();
        
        /**
         * The total CPU requirements of the virtual machines mapped to {@link #pm}.
         */
        private BigInteger used_cpu = BigInteger.ZERO;
        
        /**
         * The total memory requirements of the virtual machines mapped to {@link #pms}.
         */
        private BigInteger used_mem = BigInteger.ZERO;
        
        /**
         * Stores the IDs of the jobs of anti-colocatable virtual machines mapped to the physical machine
         * {@link #pm}.
         */
        private Set<Integer> mapped_anti_coloc_job_ids = new HashSet<Integer>();
        
        /**
         * Creates an empty instance of a usage information object for a physical machine.
         * @param pm The physical machine.
         */
        UsageInfo(PhysicalMachine pm) {
            this.pm = pm;
        }
        
        /**
         * Creates a usage information object that is a copy of another one.
         * @param info The usage information object to copy.
         */
        UsageInfo(UsageInfo info) {
            this.pm = info.pm;
            this.mapped_vms = new HashSet<VirtualMachine>(info.mapped_vms);
            this.used_cpu = info.used_cpu;
            this.used_mem = info.used_mem;
            this.mapped_anti_coloc_job_ids = new HashSet<Integer>(info.mapped_anti_coloc_job_ids);
        }
        
        /**
         * Retrieves the physical machine for which the usage information object is storing data.
         * @return The physical machine.
         */
        PhysicalMachine getPhysicalMachine() {
            return pm;
        }
        
        /**
         * Checks if there are no virtual machines mapped to the object's physical machine.
         * @return True if there are no virtual machines, false otherwise.
         */
        boolean isEmpty() {
            return mapped_vms.isEmpty();
        }
        
        /**
         * Retrieves the CPU capacity still available in the object's physical machine.
         * @return The leftover CPU capacity.
         */
        BigInteger getLeftoverCPU() {
            return pm.getCPU().subtract(used_cpu);
        }
        
        /**
         * Retrieves the memory capacity still available in the object's physical machine.
         * @return The leftover memory capacity.
         */
        BigInteger getLeftoverMemory() {
            return pm.getMemory().subtract(used_mem);
        }
        
        /**
         * Retrieves the CPU capacity still available in the object's physical machine as a percentile of its
         * full CPU capacity.
         * @return The leftover CPU capacity percentile.
         */
        double getLeftoverCPUPercentile() {
            return MathUtils.toPercentile(getLeftoverCPU(), pm.getCPU());
        }
        
        /**
         * Retrieves the memory capacity still available in the object's physical machine as a percentile of
         * its full memory capacity.
         * @return The leftover memory capacity percentile.
         */
        double getLeftoverMemoryPercentile() {
            return MathUtils.toPercentile(getLeftoverMemory(), pm.getMemory());
        }
        
        /**
         * Retrieves the virtual machines mapped to the object's physical machine.
         * @return An array with the virtual machines mapped to the physical machine.
         */
        VirtualMachine[] getPlacedVirtualMachines() {
            VirtualMachine[] vms = new VirtualMachine[mapped_vms.size()];
            mapped_vms.toArray(vms);
            return vms;
        }
        
        /**
         * Checks if a given virtual machine can be mapped to the object's physical machine without violating
         * any of the problem's constraints (resource capacity, platform and anti-colocation constraints).
         * @param vm The virtual machine.
         * @return True if the virtual machine may be mapped to the object's physical machine.
         */
        boolean canHostVirtualMachine(VirtualMachine vm) {
            return getLeftoverCPU().compareTo(vm.getCPU()) >= 0 &&
                   getLeftoverMemory().compareTo(vm.getMemory()) >= 0 &&
                   vm.canRunInPhysicalMachine(pm) &&
                   (!vm.isAntiColocatable() ||
                    !mapped_anti_coloc_job_ids.contains(new Integer(vm.getJobID())));
        }
        
        /**
         * Maps a given virtual machine to the object's physical machine. Usage information is updated
         * accordingly.
         * @param vm The virtual machine.
         */
        void placeVirtualMachine(VirtualMachine vm) {
            mapped_vms.add(vm);
            used_cpu = used_cpu.add(vm.getCPU());
            used_mem = used_mem.add(vm.getMemory());
            if (vm.isAntiColocatable()) {
                mapped_anti_coloc_job_ids.add(vm.getJobID());
            }
        }
        
        /**
         * Removes all virtual machines mapped to the object's physical machine.
         */
        void clear() {
            this.mapped_vms.clear();
            this.mapped_anti_coloc_job_ids.clear();
            this.used_cpu = BigInteger.ZERO;
            this.used_mem = BigInteger.ZERO;
        }

        /**
         * Retrieves an iterator for the usage information object. It iterates over the virtual machines
         * mapped to the object's physical machines.
         * @return The iterator.
         */
        public Iterator<VirtualMachine> iterator() {
            return mapped_vms.iterator();
        }
        
        /**
         * Checks if the usage information object is equal to another object. Two usage information objects
         * are equal if they correspond to the same physical machine.
         * @param obj The other object.
         * @return True if the objects are equal, false otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UsageInfo) {
                UsageInfo info = (UsageInfo)obj;
                return info.getPhysicalMachine().getID() == pm.getID();
            }
            return false;
        }

        /**
         * Compares the object to another usage information object. Usage information objects are compared
         * based on the resource capacities of their physical machines, in lexicographic order (CPU first).
         * @param other The other usage information object.
         * @return An integer smaller than 0 if this object's resource capacities, in lexicographic order, are
         * smaller than {@code other}'s, 0 if their equal, and an integer larger than 0 if this object's
         * capacities are larger.
         */
        public int compareTo(UsageInfo other) {
            PhysicalMachine other_pm = other.pm;
            int diff = pm.getCPU().compareTo(other_pm.getCPU());
            if (diff == 0) {
                diff = pm.getMemory().compareTo(other_pm.getMemory());
            }
            return diff;
        }
        
        /**
         * Produces a string representation of the usage information object.
         * @return The object's string representation.
         */
        @Override
        public String toString() {
            return String.format("ServerID=%d, UsedCPU=%d, LeftoverCPU=%f, UsedMemory=%d, " +
                                 "LeftoverMemory=%f",
                                 pm.getID(),
                                 used_cpu, getLeftoverCPUPercentile(),
                                 used_mem, getLeftoverMemoryPercentile());
        }
        
    }
    
    /**
     * Bin-packing algorithms store their own pre-existing virtual machine to server mappings in order to
     * support successive calls to the {@link #allocate()} method with different pre-existing allocations
     * and migration budget percentiles.
     */
    private MappingVec mappings = null;
    
    /**
     * Bin-packing algorithms store their own migration budget percentile in order to support successive calls
     * to the {@link #allocate()} method with different pre-existing allocations and migration budget
     * percentiles.
     */
    private double max_mig_percentile;
    
    /**
     * Stores a partial allocation that satisfies the problem's constraints if the algorithm fails to find a
     * feasible solution.
     */
    private MappingVec partial_allocation = null;
    
    /**
     * Stores the virtual machines left out of the partial allocation if the algorithm fails to find a
     * feasible solution.
     * @see #partial_allocation
     */
    private VirtualMachineVec leftover_vms = null;
    
    /**
     * Boolean indicating if virtual machines are to be considered in random order.
     */
    private boolean shuffle_vms = false;
    
    /**
     * Heap that stores usage information objects for all physical machines.
     */
    private PriorityQueue<UsageInfo> pm_heap = null;
    
    /**
     * Array that stores the virtual machines of the problem instance. Useful for shuffling.
     */
    private VirtualMachine[] vm_array = null;
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver based on heuristics for
     * bin-packing.
     * @param instance The problem instance.
     */
    public BinPackingAllocAlgorithm(VMCwMProblem instance) {
        super(instance);
        this.mappings = this.instance.getMappings();
        this.max_mig_percentile = this.instance.getMaxMigrationPercentile();
        this.pm_heap = buildPhysicalMachineHeap(this.instance.getPhysicalMachines(),
                                                this.mappings);
        this.vm_array = new VirtualMachine[this.instance.getVirtualMachines().size()];
        this.instance.getVirtualMachines().copyTo(this.vm_array);
    }
    
    /**
     * If a feasible allocation (vector of virtual machine to physical machine mappings) was found on the last
     * call to {@link #allocate()}, retrieves that allocation.
     * @return The allocation.
     */
    public MappingVec getAllocation() { return getAllocations().get(0); }
    
    /**
     * Enables virtual machine shuffling, i.e., virtual machines are to be considered in a randomized order.
     */
    public void enableVirtualMachineShuffling() { this.shuffle_vms = true; }
    
    /**
     * If the last call to {@link #allocate()} failed to find a feasible allocation, retrieves a feasible
     * partial allocation of a subset of the virtual machines.
     * @return The partial allocation.
     */
    public MappingVec getPartialAllocation() {
        assert(this.leftover_vms != null);
        return partial_allocation;
    }
    
    /**
     * If the last call to {@link #allocate()} failed to find a feasible allocation, retrieves the set of
     * virtual machines left out of the partial allocation returned by {@link #getPartialAllocation()}.
     * @return The leftover virtual machines.
     */
    public VirtualMachineVec getLeftoverVirtualMachines() {
        assert(this.leftover_vms != null);
        return leftover_vms;
    }
    
    /**
     * Instructs the algorithm to consider a new set of pre-existing virtual machine to physical machine
     * mappings in the next call to {@link #allocate()}.
     * @param mappings The new pre-existing virtual machine to physical machine mappings.
     */
    public void setMappings(MappingVec mappings) {
        this.mappings = mappings;
        reset();
    }
    
    /**
     * Instructs the algorithm to consider a new migration budget percentile.
     * @param max_mig_percentile The new migration budget percentile.
     */
    public void setMaxMemoryMigrationPercentile(double max_mig_percentile) {
        this.max_mig_percentile = max_mig_percentile;
        reset();
    }
    
    /**
     * Stores a partial allocation and the corresponding set of virtual machines left out of the partial
     * allocation.
     * @param allocation The partial allocations.
     * @param leftover_vms The leftover virtual machines.
     */
    private void savePartialAllocation(MappingVec allocation, VirtualMachineVec leftover_vms) {
        assert(allocation != null);
        assert(leftover_vms != null);
        this.partial_allocation = allocation;
        this.leftover_vms = leftover_vms;
    }
    
    /**
     * Resets the state of the algorithm and re-initializes the usage information heap.
     */
    private void reset() {
        clearSolutions();
        clearHeapUsages(this.pm_heap);
        placeMappings(this.pm_heap, this.instance.getPhysicalMachines(), this.mappings);
    }
    
    /**
     * Adds all usage information objects in an array to a heap.
     * @param heap The heap.
     * @param infos The array.
     */
    private void addAllUsageInfos(PriorityQueue<UsageInfo> heap, UsageInfo[] infos) {
        for (int i = 0; i < infos.length; ++i) {
            heap.add(infos[i]);
        }
    }
    
    /**
     * Updates a set of usage information objects according to a given set of virtual machine to physical
     * machine mappings.
     * @param infos The usage information objects.
     * @param id2idx A map of physical machine IDs to respective indexes in the {@code infos} array.
     * @param mappings The virtual machine to physical machine mappings to incorporate in the usage
     * information objects in {@code infos}.
     */
    private void placeMappings(UsageInfo[] infos, Map<Integer,Integer> id2idx, MappingVec mappings) {
        for (int i = 0; i < mappings.size(); ++i) {
            PhysicalMachine pm = mappings.get(i).getPhysicalMachine();
            int pm_idx = id2idx.get(new Integer(pm.getID())).intValue();
            infos[pm_idx].placeVirtualMachine(mappings.get(i).getVirtualMachine());
        }
    }
    
    /**
     * Updates an usage information heap according to a given set of virtual machine to physical machine
     * mappings.
     * @param heap The usage information heap.
     * @param pms The physical machines.
     * @param mappings The virtual machine to physical machine mappings to incorporate in the usage
     * information heap.
     */
    private void placeMappings(PriorityQueue<UsageInfo> heap,
                               PhysicalMachineVec pms,
                               MappingVec mappings) {
        IVec<UsageInfo> infos = popAllUsageInfos(heap);
        UsageInfo[] infos_array = new UsageInfo[pms.size()];
        Map<Integer,Integer> id2idx = CollectionUtils.makePhysicalMachineIDtoIndexMap(pms);
        for (int i = 0; i < infos.size(); ++i) {
            PhysicalMachine pm = infos.get(i).getPhysicalMachine();
            int pm_idx = id2idx.get(new Integer(pm.getID())).intValue();
            infos_array[pm_idx] = infos.get(i);
        }
        placeMappings(infos_array, id2idx, mappings);
        addAllUsageInfos(heap, infos_array);
    }

    /**
     * Makes the usage information heap for a given set of physical machines, initialized with a given
     * pre-existing set of virtual machine to physical machine mappings. The heap comparator object is the
     * one returned by {@link #makeUsageInfoComparator()}, and specifies the order in which physical machines
     * are to be considered when choosing where to place a virtual machine.
     * @param pms The physical machines.
     * @param mappings The pre-existing virtual machine to physical machine mappings.
     * @return The usage information heap.
     */
    private PriorityQueue<UsageInfo> buildPhysicalMachineHeap(PhysicalMachineVec pms, MappingVec mappings) {
        PriorityQueue<UsageInfo> heap = new PriorityQueue<UsageInfo>(pms.size(), makeUsageInfoComparator());
        UsageInfo[] use_infos = new UsageInfo[pms.size()];
        Map<Integer,Integer> id2idx = CollectionUtils.makePhysicalMachineIDtoIndexMap(pms);
        for (int i = 0; i < pms.size(); ++i) {
            PhysicalMachine pm = pms.get(i);
            int pm_idx = id2idx.get(new Integer(pm.getID())).intValue();
            use_infos[pm_idx] = new UsageInfo(pms.get(i));
        }
        placeMappings(use_infos, id2idx, mappings);
        addAllUsageInfos(heap, use_infos);
        return heap;
    }
    
    /**
     * Removes all usage information objects from a heap and returns them in a vector.
     * @param heap The usage information heap.
     * @return A vector with the usage information objects previously in {@code heap}.
     */
    private IVec<UsageInfo> popAllUsageInfos(PriorityQueue<UsageInfo> heap) {
        IVec<UsageInfo> infos = new Vec<UsageInfo>();
        while (!pm_heap.isEmpty()) {
            infos.push(heap.poll());
        }
        return infos;
    }
    
    /**
     * Removes all virtual machine to physical machine mappings from an usage information heap.
     * @param heap The usage information heap.
     */
    private void clearHeapUsages(PriorityQueue<UsageInfo> heap) {
        IVec<UsageInfo> infos = popAllUsageInfos(heap);
        for (int i = 0; i < infos.size(); ++i) {
            UsageInfo info = infos.get(i);
            info.clear();
            heap.add(info);
        }
    }
    
    /**
     * Creates a copy of a given usage information heap.
     * @param heap The heap.
     * @return A copy of {@code heap}.
     */
    private PriorityQueue<UsageInfo> duplicatePhysicalMachineHeap(PriorityQueue<UsageInfo> heap) {
        PriorityQueue<UsageInfo> copy =
                new PriorityQueue<UsageInfo>(heap.size(), makeUsageInfoComparator());
        for (Iterator<UsageInfo> it = heap.iterator(); it.hasNext();) {
            copy.add(new UsageInfo(it.next()));
        }
        return copy;
    }
    
    /**
     * Retrieves the set of virtual machines mapped to some physical machine in a given allocation (vector of
     * virtual machine to physical machine mappings).
     * @param mappings The allocation.
     * @return The virtual machines mapped to some server in {@code mappings}.
     */
    private Set<VirtualMachine> getMappedVirtualMachines(MappingVec mappings) {
        Set<VirtualMachine> mapped_vms = new HashSet<VirtualMachine>();
        for (int i = 0; i < mappings.size(); ++i) {
            mapped_vms.add(mappings.get(i).getVirtualMachine());
        }
        return mapped_vms;
    }
    
    /**
     * Selects a physical machine to host a given virtual machine, considered in the order by which they are
     * popped from the usage information heap.
     * @param pm_heap The usage information heap.
     * @param vm The virtual machine.
     * @return The physical machine in which to place {@code vm}, or {@code null} if it is not possible to do
     * so without violating some of the problem's constraints.
     */
    private PhysicalMachine placeVirtualMachine(PriorityQueue<UsageInfo> pm_heap, VirtualMachine vm) {
        PhysicalMachine chosen_pm = null;
        IVec<UsageInfo> popped_infos = new Vec<UsageInfo>();
        while (chosen_pm == null && !pm_heap.isEmpty()) {
            UsageInfo info = pm_heap.poll();
            if (info.canHostVirtualMachine(vm)) {
                info.placeVirtualMachine(vm);
                chosen_pm = info.getPhysicalMachine();
            }
            popped_infos.push(info);
        }
        for (int i = 0; i < popped_infos.size(); ++i) {
            pm_heap.add(popped_infos.get(i));
        }
        return chosen_pm;
    }
    
    /**
     * Retrieves the physical machines with at least one virtual machine mapped to them.
     * @param pm_heap The usage information heap.
     * @return A vector with the physical machines with some virtual machine mapped to them.
     */
    private PhysicalMachineVec getUsedPhysicalMachines(PriorityQueue<UsageInfo> pm_heap) {
        PhysicalMachineVec used_pms = new PhysicalMachineVec();
        for (Iterator<UsageInfo> it = pm_heap.iterator(); it.hasNext();) {
            UsageInfo info = it.next();
            if (!info.isEmpty()) {
                used_pms.push(info.getPhysicalMachine());
            }
        }
        return used_pms;
    }
    
    /**
     * Retrieves the usage information objects in an array, sorted according to the criteria implemented in
     * the comparator returned by {@link #makeUsageInfoComparator()}.
     * @param pm_heap The usage information heap.
     * @return The sorted array of usage information objects.
     */
    private UsageInfo[] getSortedInfos(PriorityQueue<UsageInfo> pm_heap) {
        UsageInfo[] infos = new UsageInfo[pm_heap.size()];
        pm_heap.toArray(infos);
        Arrays.sort(infos, makeUsageInfoComparator());
        return infos;
    }
    
    /**
     * Retrieves the virtual machine to physical machine mappings in a given usage information heap.
     * @param pm_heap The usage information heap.
     * @return The virtual machine to physical machine mappings in {@code pm_heap}.
     */
    private MappingVec retrieveMappings(PriorityQueue<UsageInfo> pm_heap) {
        MappingVec mappings = new MappingVec();
        for (Iterator<UsageInfo> it = pm_heap.iterator(); it.hasNext();) {
            UsageInfo info = it.next();
            PhysicalMachine pm = info.getPhysicalMachine();
            for (Iterator<VirtualMachine> vm_it = info.iterator(); vm_it.hasNext();) {
                VirtualMachine vm = vm_it.next();
                mappings.push(new Mapping(vm, pm));
            }
        }
        return mappings;
    }
    
    /**
     * Computes the total memory requirements of the virtual machines of a vector contained in a given set.
     * @param vms The virtual machine vector.
     * @param vm_subset The virtual machine subset.
     * @return The total memory requirements of the virtual machines in {@code vms} contained in
     * {@code vm_subset}.
     */
    private BigInteger subsetMemoryRequirementSum(VirtualMachine[] vms, Set<VirtualMachine> vm_subset) {
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < vms.length; ++i) {
            if (vm_subset.contains(vms[i])) {
                sum = sum.add(vms[i].getMemory());
            }
        }
        return sum;
    }
    
    /**
     * Makes a machine comparator that compares two machines based on a lexicographic ordering of their
     * resource values (CPU first). Sorting with this comparator results in a machine array sorted in
     * decreasing order of CPU values, with ties broken using the memory values.
     * @return The decreasing resource value machine comparator.
     */
    private Comparator<Machine> makeDecreasingMachineComparator() {
        return new Comparator<Machine>() {
            public int compare(Machine m0, Machine m1) {
                BigInteger diff = m1.getCPU().subtract(m0.getCPU());
                if (diff.equals(BigInteger.ZERO)) {
                    diff = m1.getMemory().subtract(m0.getMemory());
                }
                return diff.intValue();
            }
        };
    }
    
    /**
     * Makes a physical machine comparator that compares two physical machines based on a lexicographic
     * ordering of their resource capacities (CPU first). Sorting with this comparator results in a physical
     * machine array sorted in decreasing order of CPU capacities, with ties broken using the memory
     * capacities, and then using the physical machine IDs.
     * @return The decreasing resource capacity physical machine comparator.
     */
    protected Comparator<PhysicalMachine> makeDecreasingPhysicalMachineComparator() {
        return new Comparator<PhysicalMachine>() {
            Comparator<Machine> cap_comp = makeDecreasingMachineComparator();
            public int compare(PhysicalMachine m0, PhysicalMachine m1) {
                int diff = cap_comp.compare(m0, m1);
                if (diff == 0) {
                    diff = m0.getID() - m1.getID();
                }
                return diff;
            }
        };
    }
    
    /**
     * Makes a virtual machine comparator that compares two virtual machines based on a lexicographic
     * ordering of their resource requirements (CPU first). Sorting with this comparator results in a virtual
     * machine array sorted in decreasing order of CPU requirements, with ties broken using the memory
     * requirements, then using the job IDs, and at last using the virtual machine indexes within their jobs.
     * @return The decreasing resource requirement virtual machine comparator.
     */
    protected Comparator<VirtualMachine> makeDecreasingVirtualMachineComparator() {
        return new Comparator<VirtualMachine>() {
            Comparator<Machine> cap_comp = makeDecreasingMachineComparator();
            public int compare(VirtualMachine m0, VirtualMachine m1) {
                int diff = cap_comp.compare(m0, m1);
                if (diff == 0) {
                    diff = m0.getJobID() - m1.getJobID();
                    if (diff == 0) {
                        diff = m0.getIndex() - m1.getIndex();
                    }
                }
                return diff;
            }
        };
    }
    
    /**
     * Sorts a virtual machine array in decreasing order, using the comparator produced by
     * {@link #makeDecreasingVirtualMachineComparator()}.
     * @param vms The array of virtual machines.
     */
    private void sortDecreasingVirtualMachineArray(VirtualMachine[] vms) {
        Arrays.sort(vms, makeDecreasingVirtualMachineComparator());
    }
    
    /**
     * Makes the usage information comparator that implements the physical machine selection policy. Must be
     * implemented by sub-classes.
     * @return The usage information comparator.
     */
    protected abstract Comparator<UsageInfo> makeUsageInfoComparator();
    
    @Override
    public void allocate() {
        System.out.println("c WARNING: BinPacking minimizes number of servers, not energy consumption");
        System.out.println("c WARNING: timeout ignored when applying BinPacking");
        if (this.shuffle_vms) {
            PRNG.shuffle(this.vm_array);
        }
        else {
            sortDecreasingVirtualMachineArray(this.vm_array);
        }
        Set<VirtualMachine> mapped_vms = getMappedVirtualMachines(this.mappings);
        // Do First Fit for the not mapped VMs remaining
        for (int i = 0; i < this.vm_array.length; ++i) {
            VirtualMachine vm = this.vm_array[i];
            if (!mapped_vms.contains(vm)) {
                PhysicalMachine chosen_pm = placeVirtualMachine(this.pm_heap, vm);
                if (chosen_pm == null) {
                    VirtualMachineVec leftover_vms = new VirtualMachineVec();
                    for (int j = i; j < this.vm_array.length; ++j) {
                        leftover_vms.push(this.vm_array[j]);
                    }
                    savePartialAllocation(retrieveMappings(this.pm_heap), leftover_vms);
                    System.out.println("c BinPacking failed to find a feasible placement");
                    return;
                }
            }
        }
        // Discard unnecessary PMs
        PhysicalMachineVec reduced_pms = getUsedPhysicalMachines(this.pm_heap);
        MemorySumAggregator mem_agr = new MemorySumAggregator();
        reduced_pms.accept(mem_agr);
        BigInteger reduced_mem_cap = mem_agr.getMemorySum();
        double mig_budget = 
                MathUtils.scalePercentile(this.instance.getTotalMemoryCapacity(),
                                          this.max_mig_percentile,
                                          reduced_mem_cap);
        mig_budget = MathUtils.normalizePercentile(mig_budget);
        // Migrate VMs from smaller PMs
        if (this.max_mig_percentile > 0.0) { // FIXME: could be better
            this.pm_heap = buildPhysicalMachineHeap(reduced_pms, retrieveMappings(this.pm_heap));
            Set<VirtualMachine> to_migrate = new HashSet<VirtualMachine>(mapped_vms);
            UsageInfo[] sorted_infos = getSortedInfos(this.pm_heap); // FIXME: another heap in reverse order would be more efficient
            int i;
            for (i = sorted_infos.length-1; i >= 0; --i) {
                UsageInfo info = sorted_infos[i];
                VirtualMachine[] placed_vms = info.getPlacedVirtualMachines();
                if (this.shuffle_vms) {
                    PRNG.shuffle(placed_vms);
                }
                else {
                    sortDecreasingVirtualMachineArray(placed_vms);
                }
                BigInteger used_mem_cap_to_mig = subsetMemoryRequirementSum(placed_vms, to_migrate);
                double used_percentile = MathUtils.toPercentile(used_mem_cap_to_mig, reduced_mem_cap);
                if (used_percentile <= mig_budget) {
                    PriorityQueue<UsageInfo> tmp_pm_heap = duplicatePhysicalMachineHeap(this.pm_heap);
                    tmp_pm_heap.remove(info);
                    int j;
                    for (j = 0; j < placed_vms.length; ++j) {
                        VirtualMachine vm = placed_vms[j];
                        PhysicalMachine chosen_pm = placeVirtualMachine(tmp_pm_heap, vm);
                        if (chosen_pm == null) {
                            break;
                        }
                    }
                    if (j == placed_vms.length) {
                        for (j = 0; j < placed_vms.length; ++j) {
                            to_migrate.remove(placed_vms[j]);
                        }
                        this.pm_heap = tmp_pm_heap;
                        sorted_infos = getSortedInfos(this.pm_heap); // FIXME: infos for which migration was attempted already could be skipped
                        i = sorted_infos.length;
                        mig_budget -= used_percentile;
                    }
                }
            }
        }
        // Save solution
        MappingVec allocation = retrieveMappings(pm_heap);
        saveSolution(allocation);
        if (foundSolution()) {
            System.out.println("c Solution using " + pm_heap.size() + " PMs found");
        }
        else {
            System.out.println("c Solution found violates constraints");
            savePartialAllocation(allocation, new VirtualMachineVec());
        }
        printElapsedTime();
    }
    
}
