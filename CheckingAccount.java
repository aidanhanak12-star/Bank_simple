import java.math.BigDecimal;
import java.math.RoundingMode;

/** Everyday account that may dip below zero, up to an overdraft limit. */
public class CheckingAccount extends Account {
    private final BigDecimal overdraftLimit;

    public CheckingAccount(String id, String owner, BigDecimal openingBalance, BigDecimal overdraftLimit) {
        super(id, owner, openingBalance);
        if (overdraftLimit == null || overdraftLimit.signum() < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimit = overdraftLimit.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }

    @Override
    protected BigDecimal minimumAllowedBalance() { return overdraftLimit.negate(); }

    @Override
    public String getType() { return "CHECKING"; }
}
