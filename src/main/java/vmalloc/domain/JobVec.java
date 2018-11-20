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
