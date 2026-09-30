import java.math.BigDecimal;
import java.math.RoundingMode;

public class SavingsAccount extends Account {

    public SavingsAccount(String id, String owner, BigDecimal openingBalance) {
        super(id, owner, openingBalance);
    }


    public void addInterest(BigDecimal periodRate) {
        if (periodRate == null || periodRate.signum() < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        }
        BigDecimal interest = getBalance().multiply(periodRate).setScale(2, RoundingMode.HALF_UP);
        if (interest.signum() > 0) {
            credit(Transaction.Type.INTEREST, interest);
        }
    }

    @Override
    protected BigDecimal minimumAllowedBalance() { return BigDecimal.ZERO; }

    @Override
    public String getType() { return "SAVINGS"; }
}
