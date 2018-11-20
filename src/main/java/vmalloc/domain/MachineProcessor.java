package vmalloc.domain;

/**
 * Abstract superclass for visitor objects that perform computation on machine objects.
 * @author Miguel Terra-Neves
 */
public abstract class MachineProcessor {

    /**
     * Visitor method for machines. Does nothing by default; this way, sub-classes do not have to
     * re-implement this method even if not needed.
     * @param m The machine
     */
    public void processMachine(Machine m) { }
    
    /**
     * Visitor method for physical machines. By default, calls {@link #processMachine(Machine)}.
     * @param pm The physical machine.
     */
    public void processPhysicalMachine(PhysicalMachine pm) { processMachine(pm); }
    
    /**
     * Visitor method for virtual machines. By default, calls {@link #processMachine(Machine)}.
     * @param vm The virtual machine.
     */
    public void processVirtualMachine(VirtualMachine vm) { processMachine(vm); }
    
    /**
     * Calls the {@link PhysicalMachine#accept(MachineProcessor)} method for all physical machines
     * in a vector, in order.
     * @param pms The vector of physical machines.
     */
    public void processPhysicalMachines(PhysicalMachineVec pms) {
        for (int i = 0; i < pms.size(); ++i) {
            pms.get(i).accept(this);
        }
    }
    
    /**
     * Calls the {@link VirtualMachine#accept(MachineProcessor)} method for all virtual machines
     * in a vector, in order.
     * @param vms The vector of virtual machines.
     */
    public void processVirtualMachines(VirtualMachineVec vms) {
        for (int i = 0; i < vms.size(); ++i) {
            vms.get(i).accept(this);
        }
    }
    
    /**
     * Calls the {@link VirtualMachine#accept(MachineProcessor)} method for all virtual machines in
     * a job, in order.
     * @param job The job.
     */
    public void processJob(Job job) { processVirtualMachines(job.virtualMachinesAsVec()); }
    
    /**
     * Calls the {@link VirtualMachine#accept(MachineProcessor)} method for all jobs in a set of jobs, in
     * order.
     * @param jobs A vector with the set of jobs.
     */
    public void processJobs(JobVec jobs) {
        for (int i = 0; i < jobs.size(); ++i) {
            processJob(jobs.get(i));
        }
    }
    
}
