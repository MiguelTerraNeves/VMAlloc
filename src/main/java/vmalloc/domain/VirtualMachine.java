package vmalloc.domain;

import java.math.BigInteger;

/**
 * Representation of a virtual machine.
 * @author Miguel Terra-Neves
 */
public class VirtualMachine extends Machine {

    /**
     * The id of the job that contains the virtual machine.
     */
    private int job_id;
    
    /**
     * The unique index of the virtual machine within its job.
     */
    private int vm_idx;
    
    /**
     * Boolean that is true if the virtual machine cannot be colocated with the other virtual
     * machines in its job, false otherwise.
     */
    private boolean anti_coloc;
    
    /**
     * Set of physical machines that cannot host the virtual machine.
     */
    private PhysicalMachineVec unallowed_pms;
    
    /**
     * Creates an instance of a virtual machine.
     * @param job_id The id of the virtual machine's job.
     * @param vm_idx The index of the virtual machine within its job.
     * @param cpu The virtual machine's CPU requirements.
     * @param mem The virtual machine's RAM memory requirements.
     * @param anti_coloc True if the virtual machine cannot be colocated with the other virtual
     * machines in its job, false otherwise.
     */
    public VirtualMachine(int job_id,
                          int vm_idx,
                          BigInteger cpu,
                          BigInteger mem,
                          boolean anti_coloc) {
        super(cpu, mem);
        this.job_id = job_id;
        this.vm_idx = vm_idx;
        this.anti_coloc = anti_coloc;
        this.unallowed_pms = new PhysicalMachineVec();
    }
    
    /**
     * Creates an instance of a virtual machine.
     * @param job_id The id of the virtual machine's job.
     * @param vm_idx The index of the virtual machine within its job.
     * @param cpu The virtual machine's CPU requirements.
     * @param mem The virtual machine's RAM memory requirements.
     * @param anti_coloc True if the virtual machine cannot be colocated with the other virtual
     * machines in its job, false otherwise.
     * @param unallowed_pms Set of physical machines that cannot host the virtual machine.
     */
    public VirtualMachine(int job_id,
                          int vm_idx,
                          BigInteger cpu,
                          BigInteger mem,
                          boolean anti_coloc,
                          PhysicalMachineVec unallowed_pms) {
        this(job_id, vm_idx, cpu, mem, anti_coloc);
        this.unallowed_pms = unallowed_pms;
    }
    
    /**
     * Retrieves the virtual machine's unique id. The id is an aggregation of its job's id and its
     * index.
     * @return The virtual machine's id.
     */
    public String getID() { return this.job_id + "-" + this.vm_idx; }
    
    /**
     * Retrieves the id of the job that contains the virtual machine.
     * @return The virtual machine's job id.
     */
    public int getJobID() { return this.job_id; }
    
    /**
     * Retrieves the index of the virtual machine within its job.
     * @return The virtual machine's index.
     */
    public int getIndex() { return this.vm_idx; }
    
    /**
     * Returns a Boolean that indicates if the virtual machine cannot be colocated with other
     * virtual machines in its job.
     * @return True if the virtual machine cannot be colocated, false otherwise.
     */
    public boolean isAntiColocatable() { return this.anti_coloc; }
    
    /**
     * Retrieves the set of physical machines that cannot host the virtual machine.
     * @return A vector with the set of physical machines that cannot host the virtual machine.
     */
    public PhysicalMachineVec getUnallowedPhysicalMachines() { return this.unallowed_pms; }
    
    /**
     * Sets the colocation Boolean value for the virtual machine. After a call to this method, the
     * virtual machine can be colocated with any virtual machine in its job.
     */
    public void setFullyColocatable() { this.anti_coloc = false; }
    
    /**
     * Clears the set of physical machines that cannot host the virtual machine. After a call to
     * this method, the virtual machine can be hosted by any physical machine.
     */
    public void clearUnallowedPhysicalMachines() { this.unallowed_pms.clear(); }
    
    /**
     * Checks if the virtual machine can be colocated with another given virtual machine.
     * @param other_vm The other virtual machine.
     * @return True if the virtual machine can be colocated with {@code other_vm}, false otherwise.
     */
    public boolean canColocate(VirtualMachine other_vm) {
        return this.job_id != other_vm.getJobID() ||
               !this.anti_coloc ||
               !other_vm.isAntiColocatable();
    }
    
    /**
     * Checks if a given physical machine can host the virtual machine.
     * @param pm The physical machine.
     * @return True if {@code pm} can host the virtual machine, false otherwise.
     */
    // FIXME: not efficient, but should be far from being a bottleneck anyway
    public boolean canRunInPhysicalMachine(PhysicalMachine pm) {
        boolean can_run = true;
        for (int i = 0; i < this.unallowed_pms.size(); ++i) {
            if (this.unallowed_pms.get(i).getID() == pm.getID()) {
                can_run = false;
                break;
            }
        }
        return can_run;
    }
    
    @Override
    public void accept(MachineProcessor mp) { mp.processVirtualMachine(this); }
    
    /**
     * Produces and returns a string representation of the virtual machine.
     * @return The string representation of the virtual machine.
     */
    public String toString() {
        String str = String.format("VM: job_id=%d, vm_idx=%d, CPU=%d, RAM=%d, anti_coloc=%b",
                                   getJobID(),
                                   getIndex(),
                                   getCPU(),
                                   getMemory(),
                                   isAntiColocatable());
        if (this.unallowed_pms.size() > 0) {
            str += ", unallowed_pms=" + getUnallowedPhysicalMachines();
        }
        return str;
    }

}
