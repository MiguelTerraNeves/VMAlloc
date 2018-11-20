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
 * Vector object for containing virtual machine to physical machine mappings.
 * @author Miguel Terra-Neves
 */
public class MappingVec extends DomainVec<Mapping> {

    private static final long serialVersionUID = 3970714069557629243L;

    /**
     * Creates an instance of a mapping vector.
     */
    public MappingVec() { super(); }
    
    /**
     * Creates an instance of a mapping vector with the contents of another vector.
     * @param vec A mapping vector implementing the {@link IVec} interface.
     */
    public MappingVec(IVec<Mapping> vec) { super(vec); }
    
    /**
     * Creates an instance of a mapping vector with the contents of an array.
     * @param array The array.
     */
    public MappingVec(Mapping[] array) { super(array); }
    
}
