package vmalloc.domain;

import org.sat4j.specs.IVec;

/**
 * Vector object for containing virtual machines.
 * @author Miguel Terra-Neves
 */
public class VirtualMachineVec extends MachineVec<VirtualMachine> {

    private static final long serialVersionUID = 4402085886608260910L;
    
    /**
     * Creates an instance of a virtual machine vector.
     */
    public VirtualMachineVec() { super(); }
    
    /**
     * Creates an instance of a virtual machine vector with the contents of another vector.
     * @param vec A virtual machine vector implementing the {@link IVec} interface.
     */
    public VirtualMachineVec(IVec<VirtualMachine> vec) { super(vec); }
    
    /**
     * Creates an instance of a virtual machine vector with the contents of an array.
     * @param array The array.
     */
    public VirtualMachineVec(VirtualMachine[] array) { super(array); }
    
    @Override
    public void accept(MachineProcessor mp) { mp.processVirtualMachines(this); }

}
