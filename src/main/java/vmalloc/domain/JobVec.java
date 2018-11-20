package vmalloc.domain;

import org.sat4j.specs.IVec;

/**
 * Vector object for containing jobs.
 * @author Miguel Terra-Neves
 */
public class JobVec extends DomainVec<Job> {

    private static final long serialVersionUID = 7523108818202114696L;
    
    /**
     * Creates an instance of a job vector.
     */
    public JobVec() { super(); }
    
    /**
     * Creates an instance of a job vector with the contents of another vector.
     * @param vec A job vector implementing the {@link IVec} interface.
     */
    public JobVec(IVec<Job> vec) { super(vec); }
    
    /**
     * Creates an instance of a job vector with the contents of an array.
     * @param array The array.
     */
    public JobVec(Job[] array) { super(array); }
    
    /**
     * Retrieves the virtual machines in all jobs contained in the vector.
     * @return A vector with all jobs' virtual machines in order.
     */
    public VirtualMachineVec flattenJobs() {
        VirtualMachineVec vms = new VirtualMachineVec();
        for (int i = 0; i < size(); ++i) {
            Job job = get(i);
            for (int j = 0; j < job.nVirtualMachines(); ++j) {
                vms.push(job.getVirtualMachine(j));
            }
        }
        return vms;
    }
    
    /**
     * Accepts a {@link MachineProcessor} visitor object.
     * @param mp A visitor object.
     */
    public void accept(MachineProcessor mp) { mp.processJobs(this); }

}
