package vmalloc.exception;

/**
 * Exception thrown when a user specifies an invalid encoding (e.g. for solution variable values).
 * @author Miguel Terra-Neves
 */
public class InvalidEncodingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidEncodingException() { super(); }
    
    public InvalidEncodingException(String string) { super(string); }
    
}
