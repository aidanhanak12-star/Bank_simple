import java.math.BigDecimal;

public record Transaction(Type type, BigDecimal amount, BigDecimal balanceAfter) {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST }

    @Override
    public String toString() {
        return String.format("%-13s %10s   balance: %10s", type, amount, balanceAfter);
    }
}
