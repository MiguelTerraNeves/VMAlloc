package vmalloc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.Job;
import vmalloc.domain.PhysicalMachine;
import vmalloc.utils.MathUtils;

/**
 * Class used to store and manage statistics on a problem instance.
 * @author Miguel Terra-Neves
 */
public class ProblemStatistics {

    /**
     * The problem instance.
     */
    private VMCwMProblem instance = null;
    
    /**
     * Stores the sum of the CPU capacities of the physical machines.
     */
    private BigInteger total_cpu_cap = BigInteger.ZERO;
    
    /**
     * Stores the sum of the memory capacities of the physical machines.
     */
    private BigInteger total_mem_cap = BigInteger.ZERO;
    
    /**
     * Stores the minimum CPU capacity among all physical machines.
     */
    private BigInteger min_cpu_cap = BigInteger.ZERO;
    
    /**
     * Stores the minimum memory capacity among all physical machines.
     */
    private BigInteger min_mem_cap = BigInteger.ZERO;
    
    /**
     * Stores the maximum CPU capacity among all physical machines.
     */
    private BigInteger max_cpu_cap = BigInteger.ZERO;
    
    /**
     * Stores the maximum memory capacity among all physical machines.
     */
    private BigInteger max_mem_cap = BigInteger.ZERO;
    
    /**
     * Stores the total number of virtual machines.
     */
    private int total_vms = 0;
    
    /**
     * Stores the minimum number of virtual machines among all jobs.
     */
    private int min_vms = 0;
    
    /**
     * Stores the maximum number of virtual machines among all jobs.
     */
    private int max_vms = 0;
    
    /**
     * Stores the sum of the CPU requirements of the virtual machines.
     */
    private BigInteger total_cpu_req = BigInteger.ZERO;
    
    /**
     * Stores the sum of the memory requirements of the virtual machines.
     */
    private BigInteger total_mem_req = BigInteger.ZERO;
    
    /**
     * Stores the minimum CPU requirement among all virtual machines.
     */
    private BigInteger min_cpu_req = BigInteger.ZERO;
    
    /**
     * Stores the minimum memory requirement among all virtual machines.
     */
    private BigInteger min_mem_req = BigInteger.ZERO;
    
    /**
     * Stores the maximum CPU requirement among all virtual machines.
     */
    private BigInteger max_cpu_req = BigInteger.ZERO;
    
    /**
     * Stores the maximum memory requirement among all virtual machines.
     */
    private BigInteger max_mem_req = BigInteger.ZERO;
    
    /**
     * Stores the sum of the CPU requirements of the virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     */
    private BigInteger total_cpu_map_req = BigInteger.ZERO;
    
    /**
     * Stores the sum of the memory requirements of the virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     */
    private BigInteger total_mem_map_req = BigInteger.ZERO;
    
    /**
     * Stores the minimum CPU requirement among all virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     */
    private BigInteger min_cpu_map_req = BigInteger.ZERO;
    
    /**
     * Stores the minimum memory requirement among all virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     */
    private BigInteger min_mem_map_req = BigInteger.ZERO;
    
    /**
     * Stores the maximum CPU requirement among all virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     */
    private BigInteger max_cpu_map_req = BigInteger.ZERO;
    
    /**
     * Stores the maximum memory requirement among all virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     */
    private BigInteger max_mem_map_req = BigInteger.ZERO;
    
    /**
     * Stores the number of physical machines with some virtual machine mapped to it in the pre-existing
     * allocation.
     */
    private int used_pms = 0;
    
    /**
     * Creates an instance of an object that stores and manages statistics on a given problem instance.
     * @param instance The problem instance.
     */
    public ProblemStatistics(VMCwMProblem instance) {
        this.instance = instance;
        initStatistics();
    }
    
    /**
     * Initializes all statistic attributes.
     */
    private void initStatistics() {
        for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
            BigInteger cpu_cap = this.instance.getPhysicalMachines().get(i).getCPU();
            BigInteger mem_cap = this.instance.getPhysicalMachines().get(i).getMemory();
            this.total_cpu_cap = this.total_cpu_cap.add(cpu_cap);
            this.total_mem_cap = this.total_mem_cap.add(mem_cap);
            if (this.min_cpu_cap.compareTo(cpu_cap) > 0 ||
                this.min_cpu_cap.equals(BigInteger.ZERO)) {
                this.min_cpu_cap = cpu_cap;
            }
            if (this.max_cpu_cap.compareTo(cpu_cap) < 0 ||
                this.max_cpu_cap.equals(BigInteger.ZERO)) {
                this.max_cpu_cap = cpu_cap;
            }
            if (this.min_mem_cap.compareTo(mem_cap) > 0 ||
                this.min_mem_cap.equals(BigInteger.ZERO)) {
                this.min_mem_cap = mem_cap;
            }
            if (this.max_mem_cap.compareTo(mem_cap) < 0 ||
                this.max_mem_cap.equals(BigInteger.ZERO)) {
                this.max_mem_cap = mem_cap;
            }
        }
        for (int i = 0; i < this.instance.getJobs().size(); ++i) {
            Job job = this.instance.getJobs().get(i);
            this.total_vms += job.nVirtualMachines();
            if (this.min_vms > job.nVirtualMachines() || this.min_vms == 0) {
                this.min_vms = job.nVirtualMachines();
            }
            if (this.max_vms < job.nVirtualMachines() || this.max_vms == 0) {
                this.max_vms = job.nVirtualMachines();
            }
            for (int j = 0; j < job.nVirtualMachines(); ++j) {
                BigInteger cpu_req = job.getVirtualMachine(j).getCPU();
                BigInteger mem_req = job.getVirtualMachine(j).getMemory();
                this.total_cpu_req = this.total_cpu_req.add(cpu_req);
                this.total_mem_req = this.total_mem_req.add(mem_req);
                if (this.min_cpu_req.compareTo(cpu_req) > 0 ||
                    this.min_cpu_req.equals(BigInteger.ZERO)) {
                    this.min_cpu_req = cpu_req;
                }
                if (this.max_cpu_req.compareTo(cpu_req) < 0 ||
                    this.max_cpu_req.equals(BigInteger.ZERO)) {
                    this.max_cpu_req = cpu_req;
                }
                if (this.min_mem_req.compareTo(mem_req) > 0 ||
                    this.min_mem_req.equals(BigInteger.ZERO)) {
                    this.min_mem_req = mem_req;
                }
                if (this.max_mem_req.compareTo(mem_req) < 0 ||
                    this.max_mem_req.equals(BigInteger.ZERO)) {
                    this.max_mem_req = mem_req;
                }
            }
        }
        Set<PhysicalMachine> used_pms = new HashSet<PhysicalMachine>();
        for (int i = 0; i < this.instance.getMappings().size(); ++i) {
            BigInteger cpu_req = this.instance.getMappings().get(i).getVirtualMachine().getCPU();
            BigInteger mem_req = this.instance.getMappings().get(i).getVirtualMachine().getMemory();
            this.total_cpu_map_req = this.total_cpu_map_req.add(cpu_req);
            this.total_mem_map_req = this.total_mem_map_req.add(mem_req);
            if (this.min_cpu_map_req.compareTo(cpu_req) > 0 ||
                this.min_cpu_map_req.equals(BigInteger.ZERO)) {
                this.min_cpu_map_req = cpu_req;
            }
            if (this.max_cpu_map_req.compareTo(cpu_req) < 0 ||
                this.max_cpu_map_req.equals(BigInteger.ZERO)) {
                this.max_cpu_map_req = cpu_req;
            }
            if (this.min_mem_map_req.compareTo(mem_req) > 0 ||
                this.min_mem_map_req.equals(BigInteger.ZERO)) {
                this.min_mem_map_req = mem_req;
            }
            if (this.max_mem_map_req.compareTo(mem_req) < 0 ||
                this.max_mem_map_req.equals(BigInteger.ZERO)) {
                this.max_mem_map_req = mem_req;
            }
            used_pms.add(this.instance.getMappings().get(i).getPhysicalMachine());
        }
        this.used_pms = used_pms.size();
    }
    
    /**
     * Method that facilitates the division of two big integers.
     * @param int1 A big integer.
     * @param int2 Another big integer.
     * @return The division of {@code int1} by {@code int2}.
     */
    private BigDecimal divideBigIntegers(BigInteger int1, BigInteger int2) {
        return MathUtils.divideBigIntegers(int1, int2, RoundingMode.HALF_UP);
    }
    
    /**
     * Retrieves the number of physical machines in the problem instance.
     * @return The number of physical machines.
     */
    public int nPhysicalMachines() { return this.instance.getPhysicalMachines().size(); }
    
    /**
     * Retrieves the number of jobs in the problem instance.
     * @return The number of jobs.
     */
    public int nJobs() { return this.instance.getJobs().size(); }
    
    /**
     * Retrieves the number of virtual machines in the problem instance.
     * @return The number of virtual machines.
     */
    public int nVirtualMachines() { return this.total_vms; }
    
    /**
     * Retrieves the number of virtual machines mapped to some physical machine in the pre-existing allocation
     * in the problem instance.
     * @return The number of mappings in the pre-existing allocation.
     */
    public int nMappings() { return this.instance.getMappings().size(); }
    
    /**
     * Retrieves the number of physical machines with some virtual machine mapped to it in the pre-existing
     * allocation in the problem instance.
     * @return The number of physical machines used in the pre-existing allocation.
     */
    public int nUsedPhysicalMachines() { return this.used_pms; }
    
    /**
     * Retrieves the migration budget percentile, i.e., the percentile of the total memory capacity that can
     * be used for migrations.
     * @return The migration budget percentile.
     */
    public double getAllowedMigrationPercentile() { return this.instance.getMaxMigrationPercentile(); }
    
    /**
     * Retrieves the migration budget, i.e., the total memory capacity that can be used for migrations.
     * @return The migration budget.
     */
    public BigInteger getMigrationCapacityBudget() {
        BigDecimal budget =
                new BigDecimal(this.total_mem_cap).multiply(
                        new BigDecimal(this.instance.getMaxMigrationPercentile()));
        return budget.toBigInteger();
    }
    
    /**
     * Retrieves the sum of the CPU capacities of all physical machines.
     * @return The CPU capacity sum.
     */
    public BigInteger getTotalCPUCapacity() { return this.total_cpu_cap; }
    
    /**
     * Retrieves the sum of the memory capacities of all physical machines.
     * @return The memory capacity sum.
     */
    public BigInteger getTotalMemoryCapacity() { return this.total_mem_cap; }
    
    /**
     * Retrieves the minimum CPU capacity among all physical machines.
     * @return The minimum CPU capacity.
     */
    public BigInteger getMinimumCPUCapacity() { return this.min_cpu_cap; }
    
    /**
     * Retrieves the minimum memory capacity among all physical machines.
     * @return The minimum memory capacity.
     */
    public BigInteger getMinimumMemoryCapacity() { return this.min_mem_cap; }
    
    /**
     * Retrieves the maximum CPU capacity among all physical machines.
     * @return The maximum CPU capacity.
     */
    public BigInteger getMaximumCPUCapacity() { return this.max_cpu_cap; }
    
    /**
     * Retrieves the maximum memory capacity among all physical machines.
     * @return The maximum memory capacity.
     */
    public BigInteger getMaximumMemoryCapacity() { return this.max_mem_cap; }
    
    /**
     * Retrieves the average CPU capacity among all physical machines.
     * @return The average CPU capacity.
     */
    public BigDecimal getAverageCPUCapacity() {
        return divideBigIntegers(this.total_cpu_cap, BigInteger.valueOf(nPhysicalMachines()));
    }
    
    /**
     * Retrieves the average memory capacity among all physical machines.
     * @return The average memory capacity.
     */
    public BigDecimal getAverageMemoryCapacity() {
        return divideBigIntegers(this.total_mem_cap, BigInteger.valueOf(nPhysicalMachines()));
    }
    
    /**
     * Retrieves the minimum number of virtual machines in a job among all jobs.
     * @return The minimum number of virtual machines in a job.
     */
    public int getMinimumVirtualMachinesPerJob() { return this.min_vms; }
    
    /**
     * Retrieves the maximum number of virtual machines in a job among all jobs.
     * @return The maximum number of virtual machines in a job.
     */
    public int getMaximumVirtualMachinesPerJob() { return this.max_vms; }
    
    /**
     * Retrieves the average number of virtual machines in a job among all jobs.
     * @return The average number of virtual machines in a job.
     */
    public double getAverageVirtualMachinesPerJob() { return nVirtualMachines()/nJobs(); }
    
    /**
     * Retrieves the sum of the CPU requirements of all virtual machines.
     * @return The CPU requirement sum.
     */
    public BigInteger getTotalCPURequirements() { return this.total_cpu_req; }
    
    /**
     * Retrieves the sum of the memory requirements of all virtual machines.
     * @return The memory requirement sum.
     */
    public BigInteger getTotalMemoryRequirements() { return this.total_mem_req; }
    
    /**
     * Retrieves the minimum CPU requirement among all virtual machines.
     * @return The minimum CPU requirement.
     */
    public BigInteger getMinimumCPURequirements() { return this.min_cpu_req; }
    
    /**
     * Retrieves the minimum memory requirement among all virtual machines.
     * @return The minimum memory requirement.
     */
    public BigInteger getMinimumMemoryRequirements() { return this.min_mem_req; }
    
    /**
     * Retrieves the maximum CPU requirement among all virtual machines.
     * @return The maximum CPU requirement.
     */
    public BigInteger getMaximumCPURequirements() { return this.max_cpu_req; }
    
    /**
     * Retrieves the maximum memory requirement among all virtual machines.
     * @return The maximum memory requirement.
     */
    public BigInteger getMaximumMemoryRequirements() { return this.max_mem_req; }
    
    /**
     * Retrieves the average CPU requirement among all virtual machines.
     * @return The average CPU requirement.
     */
    public BigDecimal getAverageCPURequirements() {
        return divideBigIntegers(this.total_cpu_req, BigInteger.valueOf(nVirtualMachines()));
    }
    
    /**
     * Retrieves the average memory requirement among all virtual machines.
     * @return The average memory requirement.
     */
    public BigDecimal getAverageMemoryRequirements() {
        return divideBigIntegers(this.total_mem_req, BigInteger.valueOf(nVirtualMachines()));
    }
    
    /**
     * Retrieves the sum of the CPU requirements of the virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The sum of the CPU requirements of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingTotalCPURequirements() { return this.total_cpu_map_req; }
    
    /**
     * Retrieves the sum of the memory requirements of the virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The sum of the memory requirements of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingTotalMemoryRequirements() { return this.total_mem_map_req; }
    
    /**
     * Retrieves the minimum CPU requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The minimum CPU requirement of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingMinimumCPURequirements() { return this.min_cpu_map_req; }
    
    /**
     * Retrieves the minimum memory requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The minimum memory requirement of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingMinimumMemoryRequirements() { return this.min_mem_map_req; }
    
    /**
     * Retrieves the maximum CPU requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The maximum CPU requirement of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingMaximumCPURequirements() { return this.max_cpu_map_req; }
    
    /**
     * Retrieves the maximum memory requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The maximum memory requirement of the virtual machines in the pre-existing allocation.
     */
    public BigInteger getMappingMaximumMemoryRequirements() { return this.max_mem_map_req; }
    
    /**
     * Retrieves the average CPU requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The average CPU requirement of the virtual machines in the pre-existing allocation.
     */
    public BigDecimal getMappingAverageCPURequirements() {
        return divideBigIntegers(this.total_cpu_map_req, BigInteger.valueOf(nUsedPhysicalMachines()));
    }
    
    /**
     * Retrieves the average memory requirement among all virtual machines mapped to some physical machine in
     * the pre-existing allocation.
     * @return The average memory requirement of the virtual machines in the pre-existing allocation.
     */
    public BigDecimal getMappingAverageMemoryRequirements() {
        return divideBigIntegers(this.total_mem_map_req, BigInteger.valueOf(nUsedPhysicalMachines()));
    }
    
    /**
     * Retrieves the total CPU requirements divided by the total CPU capacity.
     * @return The CPU usage percentile.
     */
    public double getCPUUsagePercentile() {
        return divideBigIntegers(this.total_cpu_req, this.total_cpu_cap).doubleValue();
    }
    
    /**
     * Retrieves the total memory requirements divided by the total memory capacity.
     * @return The memory usage percentile.
     */
    public double getMemoryUsagePercentile() {
        return divideBigIntegers(this.total_mem_req, this.total_mem_cap).doubleValue();
    }
    
    /**
     * Retrieves the total CPU requirements of the virtual machines in the pre-existing allocation divided by
     * the total CPU capacity.
     * @return The CPU usage percentile of virtual machines in the pre-existing allocation.
     */
    public double getMappingCPUUsagePercentile() {
        return divideBigIntegers(this.total_cpu_map_req, this.total_cpu_cap).doubleValue();
    }
    
    /**
     * Retrieves the total memory requirements of the virtual machines in the pre-existing allocation divided
     * by the total memory capacity.
     * @return The memory usage percentile of virtual machines in the pre-existing allocation.
     */
    public double getMappingMemoryUsagePercentile() {
        return divideBigIntegers(this.total_mem_map_req, this.total_mem_cap).doubleValue();
    }
    
    /**
     * Pretty-prints several statistics of the problem instance to the standard output.
     */
    public void printStatistics() {
        System.out.println("c =========================== Problem Statistics ===========================");
        System.out.println("c          | PMs        | Jobs       | VMs        | Mappings   | Used PMs");
        System.out.println("c  Number  | " +
                           String.format("%10d", nPhysicalMachines()) + " | " + 
                           String.format("%10d", nJobs()) + " | " +
                           String.format("%10d", nVirtualMachines()) + " | " +
                           String.format("%10d", nMappings()) + " | " +
                           String.format("%10d", nUsedPhysicalMachines()));
        System.out.println("c  -------------------------------------------------------------------------");
        System.out.println("c  -------------------------------------------------------------------------");
        System.out.println("c                               | CPU                | RAM");
        System.out.println("c  -------------------------------------------------------------------------");
        System.out.println("c  Total Capacity               | " +
                           String.format("%18d", getTotalCPUCapacity()) + " | " +
                           String.format("%18d", getTotalMemoryCapacity()));
        System.out.println("c  Minimum Capacity             | " +
                              String.format("%18d", getMinimumCPUCapacity()) + " | " +
                              String.format("%18d", getMinimumMemoryCapacity()));
        System.out.println("c  Maximum Capacity             | " +
                              String.format("%18d", getMaximumCPUCapacity()) + " | " +
                              String.format("%18d", getMaximumMemoryCapacity()));
        System.out.println("c  Average Capacity             | " +
                              String.format("%18.2f", getAverageCPUCapacity()) + " | " +
                              String.format("%18.2f", getAverageMemoryCapacity()));
        System.out.println("c  -------------------------------------------------------------------------");
        System.out.println("c  Total Requirements           | " +
                           String.format("%18d", getTotalCPURequirements()) + " | " +
                           String.format("%18d", getTotalMemoryRequirements()));
        System.out.println("c  Minimum Requirements         | " +
                              String.format("%18d", getMinimumCPURequirements()) + " | " +
                              String.format("%18d", getMinimumMemoryRequirements()));
        System.out.println("c  Maximum Requirements         | " +
                              String.format("%18d", getMaximumCPURequirements()) + " | " +
                              String.format("%18d", getMaximumMemoryRequirements()));
        System.out.println("c  Average Requirements         | " +
                              String.format("%18.2f", getAverageCPURequirements()) + " | " +
                              String.format("%18.2f", getAverageMemoryRequirements()));
        System.out.println("c  Usage Percentile             | " +
                              String.format("%18.16f", getCPUUsagePercentile()) + " | " +
                              String.format("%18.16f", getMemoryUsagePercentile()));
        if (nMappings() > 0) {
            System.out.println("c  -------------------------------------------------------------------------");
            System.out.println("c  Total Mapping Requirements   | " +
                    String.format("%18d", getMappingTotalCPURequirements()) + " | " +
                    String.format("%18d", getMappingTotalMemoryRequirements()));
            System.out.println("c  Minimum Mapping Requirements | " +
                    String.format("%18d", getMappingMinimumCPURequirements()) + " | " +
                    String.format("%18d", getMappingMinimumMemoryRequirements()));
            System.out.println("c  Maximum Mapping Requirements | " +
                    String.format("%18d", getMappingMaximumCPURequirements()) + " | " +
                    String.format("%18d", getMappingMaximumMemoryRequirements()));
            System.out.println("c  Average Mapping Requirements | " +
                    String.format("%18.2f", getMappingAverageCPURequirements()) + " | " +
                    String.format("%18.2f", getMappingAverageMemoryRequirements()));
            System.out.println("c  Mapping Usage Percentile     | " +
                    String.format("%18.16f", getMappingCPUUsagePercentile()) + " | " +
                    String.format("%18.16f", getMappingMemoryUsagePercentile()));
            System.out.println("c  Migration Budget Percentile  |                  - | " +
                    String.format("%18.16f", getAllowedMigrationPercentile()));
            System.out.println("c  Migration Capacity Budget    |                  - | " +
                    String.format("%18d", getMigrationCapacityBudget()));
        }
        System.out.println("c ==========================================================================");
    }
    
}
