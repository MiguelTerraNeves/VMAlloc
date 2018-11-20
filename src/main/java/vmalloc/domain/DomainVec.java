package vmalloc.domain;

import java.util.Comparator;
import java.util.Iterator;

import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

/**
 * Abstract superclass for vectors that contain domain objects.
 * @author Miguel Terra-Neves
 * @param <Type> The type of object contained in the vector.
 */
public abstract class DomainVec<Type> implements IVec<Type> {

    private static final long serialVersionUID = -1183845827636061971L;
    
    /**
     * The actual vector object used to contain domain objects.
     */
    private IVec<Type> vec = new Vec<Type>();
    
    /**
     * Creates an instance of a domain object vector.
     */
    public DomainVec() { this(new Vec<Type>()); }
    
    /**
     * Creates an instance of a domain object vector with the contents of another vector.
     * @param vec A vector implementing the {@link IVec} interface.
     */
    public DomainVec(IVec<Type> vec) { vec.copyTo(this.vec); }
    
    /**
     * Creates an instance of domain object vector with the contents of an array.
     * @param array The array.
     */
    public DomainVec(Type[] array) { this.vec = new Vec<Type>(array); }
    
    /* 
     * Implementation of all methods in the IVec interface. All calls redirected to the actual Vec
     * object. Vec is a final class, so extending Vec was not possible.
     */
    public int size() { return this.vec.size(); }
    public void shrink(int nofelems) { this.vec.shrink(nofelems); }
    public void shrinkTo(int newsize) { this.vec.shrinkTo(newsize); }
    public void pop() { this.vec.pop(); }
    public void growTo(int newsize, Type pad) { this.vec.growTo(newsize, pad); }
    public void ensure(int nsize) { this.vec.ensure(nsize); }
    public IVec<Type> push(Type elem) { return this.vec.push(elem); }
    public void unsafePush(Type elem) { this.vec.unsafePush(elem); }
    public void insertFirst(Type elem) { this.vec.insertFirst(elem); }
    public void insertFirstWithShifting(Type elem) { this.vec.insertFirstWithShifting(elem); }
    public void clear() { this.vec.clear(); }
    public Type last() { return this.vec.last(); }
    public Type get(int i) { return this.vec.get(i); }
    public void set(int i, Type o) { this.vec.set(i, o); }
    public void remove(Type elem) { this.vec.remove(elem); }
    public void removeFromLast(Type elem) { this.vec.removeFromLast(elem); }
    public Type delete(int i) { return this.vec.delete(i); }
    public void copyTo(IVec<Type> copy) { this.vec.copyTo(copy); }
    public <E> void copyTo(E[] dest) { this.vec.copyTo(dest); }
    public Type[] toArray() { return this.vec.toArray(); }
    public void moveTo(IVec<Type> dest) { this.vec.moveTo(dest); }
    public void moveTo(int dest, int source) { this.vec.moveTo(dest, source); }
    public void sort(Comparator<Type> comparator) { this.vec.sort(comparator); }
    public void sortUnique(Comparator<Type> comparator) { this.vec.sortUnique(comparator); }
    public boolean isEmpty() { return this.vec.isEmpty(); }
    public Iterator<Type> iterator() { return this.vec.iterator(); }
    public boolean contains(Type element) { return this.vec.contains(element); }
    public int indexOf(Type element) { return this.vec.indexOf(element); }
    public IVec<Type> clone() { return this.vec.clone(); }
}
