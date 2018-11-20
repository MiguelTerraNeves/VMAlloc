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
