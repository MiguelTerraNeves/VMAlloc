package vmalloc.exception;

/**
 * Exception thrown when using functionality that has not been implemented yet.
 * @author Miguel Terra-Neves
 */
public class NotImplementedException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public NotImplementedException() { super(); }
    
    public NotImplementedException(String string) { super(string); }
    
}
