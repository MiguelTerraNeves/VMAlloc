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
