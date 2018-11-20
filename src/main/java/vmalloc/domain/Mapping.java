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
