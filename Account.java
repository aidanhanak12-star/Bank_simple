import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class Account {
    private final String id;
    private final String owner;
    private BigDecimal balance;
    private final List<Transaction> history = new ArrayList<>();

    protected Account(String id, String owner, BigDecimal openingBalance) {
        if (id == null || id.isBlank() || id.contains(",")) {
            throw new IllegalArgumentException("Account id must be non-empty and contain no commas.");
        }
        if (owner == null || owner.isBlank() || owner.contains(",")) {
            throw new IllegalArgumentException("Owner name must be non-empty and contain no commas.");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.id = id.trim();
        this.owner = owner.trim();
        this.balance = openingBalance.setScale(2, RoundingMode.HALF_UP);
    }


    protected abstract BigDecimal minimumAllowedBalance();


    public abstract String getType();

    public void deposit(BigDecimal amount) {
        credit(Transaction.Type.DEPOSIT, validateAmount(amount));
    }

    public void withdraw(BigDecimal amount) throws InsufficientFundsException {
        debit(Transaction.Type.WITHDRAWAL, validateAmount(amount));
    }

    void credit(Transaction.Type type, BigDecimal amount) {
        balance = balance.add(amount);
        history.add(new Transaction(type, amount, balance));
    }
    void debit(Transaction.Type type, BigDecimal amount) throws InsufficientFundsException {
        if (balance.subtract(amount).compareTo(minimumAllowedBalance()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in " + id + ": balance " + balance + ", requested " + amount + ".");
        }
        balance = balance.subtract(amount);
        history.add(new Transaction(type, amount, balance));
    }
    void restoreBalance(BigDecimal saved) {
        BigDecimal restored = saved.setScale(2, RoundingMode.HALF_UP);
        if (restored.compareTo(minimumAllowedBalance()) < 0) {
            throw new IllegalArgumentException("Saved balance for " + id + " is below the allowed minimum.");
        }
        balance = restored;
    }

    static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be at least 0.01.");
        }
        return rounded;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public String toString() {
        return String.format("%-8s %-10s %-20s %12s", getType(), id, owner, balance);
    }
}
