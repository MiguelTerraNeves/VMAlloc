package vmalloc.domain;

/**
 * Representation of a placement of a virtual machine on a physical machine.
 * @author Miguel Terra-Neves
 */
public class Mapping {

    /**
     * The hosted virtual machine.
     */
    private VirtualMachine vm;
    
    /**
     * The physical machine hosting the virtual machine.
     */
    private PhysicalMachine pm;
    
    /**
     * Creates an instance of a mapping of a virtual machine to a physical machine.
     * @param vm The virtual machine placed on {@code pm}.
     * @param pm The physical machine hosting {@code vm}.
     */
    public Mapping(VirtualMachine vm, PhysicalMachine pm) {
        this.vm = vm;
        this.pm = pm;
    }
    
    /**
     * Retrieves the virtual machine in the mapping.
     * @return The virtual machine.
     */
    public VirtualMachine getVirtualMachine() { return this.vm; }
    
    /**
     * Retrieves the physical machine in the mapping.
     * @return The physical machine.
     */
    public PhysicalMachine getPhysicalMachine() { return this.pm; }
    
    /**
     * Produces and returns a string representation of the mapping.
     * @return The string representation of the mapping.
     */
    public String toString() {
        return String.format("Mapping: job_id=%d, vm_idx=%d, server_id=%d",
                             this.vm.getJobID(),
                             this.vm.getIndex(),
                             this.pm.getID());
    }
    
}
