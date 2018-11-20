package vmalloc.domain;

/**
 * Representation of a client application. A job is composed by a set of virtual machines.
 * @author Miguel Terra-Neves
 */
public class Job {

    /**
     * Job's unique id.
     */
    private int id;
    
    /**
     * Set of virtual machines that form the job.
     */
    private VirtualMachineVec vms;
    
    /**
     * Creates an instance of a job.
     * @param id The job's id.
     */
    public Job(int id) {
        this.id = id;
        this.vms = new VirtualMachineVec();
    }
    
    /**
     * Creates an instance of a job.
     * @param id The job's id.
     * @param vms The set of virtual machines that form the job.
     */
    public Job(int id, VirtualMachineVec vms) {
        this(id);
        vms.copyTo(this.vms);
    }
    
    /**
     * Retrieves the job's id.
     * @return The job's id.
     */
    public int getID() { return this.id; }
    
    /**
     * Retrieves a vector with the virtual machines in the job.
     * @return The virtual machines in the job.
     */
    public VirtualMachineVec virtualMachinesAsVec() { return new VirtualMachineVec(this.vms); }

    /**
     * Adds a virtual machine to the job.
     * @param vm The virtual machine to be added.
     */
    public void addVirtualMachine(VirtualMachine vm) { this.vms.push(vm); }
    
    /**
     * Retrieves a virtual machine with a given index.
     * @param i The index of the virtual machine.
     * @return The virtual machine with index {@code i}.
     */
    public VirtualMachine getVirtualMachine(int i) { return this.vms.get(i); }
    
    /**
     * Retrieves the set of virtual machines in the job that cannot be colocated.
     * @return A vector with the set of anti-colocatable virtual machines.
     */
    public VirtualMachineVec getAntiColocatableVirtualMachines() {
        VirtualMachineVec anti_coloc_vms = new VirtualMachineVec();
        for (int i = 0; i < nVirtualMachines(); ++i) {
            VirtualMachine vm = getVirtualMachine(i);
            if (vm.isAntiColocatable()) {
                anti_coloc_vms.push(vm);
            }
        }
        return anti_coloc_vms;
    }
    
    /**
     * Retrieves the number of virtual machines in the job.
     * @return The number of virtual machines in the job.
     */
    public int nVirtualMachines() { return this.vms.size(); }
    
    /**
     * Accepts a {@link MachineProcessor} visitor object.
     * @param mp A visitor object.
     */
    public void accept(MachineProcessor mp) { mp.processJob(this); }
    
    /**
     * Produces and returns a string representation of the job.
     * @return The string representation of the job.
     */
    public String toString() {
        return String.format("Job: id = %d, vms = ", getID()) + this.vms;
    }
    
}
