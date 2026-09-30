/** Thrown when a withdrawal or transfer would take an account below its minimum balance. */
public class InsufficientFundsException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
