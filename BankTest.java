import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;


public class BankTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        depositAndWithdraw();
        savingsCannotOverdraw();
        checkingOverdraftLimit();
        invalidAmountsRejected();
        transferMovesMoneyAndLogsBothSides();
        failedTransferChangesNothing();
        interestIsAddedAndRounded();
        duplicateIdRejected();
        historyCannotBeModified();
        saveAndLoadRoundTrip();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---- tests ----

    static void depositAndWithdraw() throws Exception {
        Account a = new SavingsAccount("S1", "Sam", money("100"));
        a.deposit(money("50.50"));
        a.withdraw(money("20.25"));
        checkEquals("deposit then withdraw", money("130.25"), a.getBalance());
        checkEquals("two history entries", 2, a.getHistory().size());
    }

    static void savingsCannotOverdraw() {
        Account a = new SavingsAccount("S1", "Sam", money("10"));
        checkThrows("savings overdraw blocked", InsufficientFundsException.class, () -> a.withdraw(money("10.01")));
        checkEquals("balance unchanged after failure", money("10"), a.getBalance());
    }

    static void checkingOverdraftLimit() throws Exception {
        Account a = new CheckingAccount("C1", "Cy", money("20"), money("50"));
        a.withdraw(money("70")); // down to -50, exactly the limit
        checkEquals("can reach overdraft limit", money("-50"), a.getBalance());
        checkThrows("cannot pass overdraft limit", InsufficientFundsException.class, () -> a.withdraw(money("0.01")));
    }

    static void invalidAmountsRejected() {
        Account a = new SavingsAccount("S1", "Sam", money("10"));
        checkThrows("zero deposit", IllegalArgumentException.class, () -> a.deposit(money("0")));
        checkThrows("negative deposit", IllegalArgumentException.class, () -> a.deposit(money("-5")));
        checkThrows("null deposit", IllegalArgumentException.class, () -> a.deposit(null));
        checkThrows("negative opening balance", IllegalArgumentException.class,
                () -> new SavingsAccount("S2", "Sam", money("-1")));
        checkThrows("comma in owner", IllegalArgumentException.class,
                () -> new SavingsAccount("S3", "Sam, Jr", money("1")));
    }

    static void transferMovesMoneyAndLogsBothSides() throws Exception {
        Bank bank = sampleBank();
        bank.transfer("S1", "C1", money("40"));
        checkEquals("source debited", money("60.00"), bank.getAccount("S1").getBalance());
        checkEquals("destination credited", money("140.00"), bank.getAccount("C1").getBalance());
        checkEquals("source logged TRANSFER_OUT", Transaction.Type.TRANSFER_OUT,
                bank.getAccount("S1").getHistory().get(0).type());
        checkEquals("destination logged TRANSFER_IN", Transaction.Type.TRANSFER_IN,
                bank.getAccount("C1").getHistory().get(0).type());
    }

    static void failedTransferChangesNothing() {
        Bank bank = sampleBank();
        checkThrows("transfer too large", InsufficientFundsException.class,
                () -> bank.transfer("S1", "C1", money("500")));
        checkEquals("source untouched", money("100.00"), bank.getAccount("S1").getBalance());
        checkEquals("destination untouched", money("100.00"), bank.getAccount("C1").getBalance());
        checkThrows("same-account transfer", IllegalArgumentException.class,
                () -> bank.transfer("S1", "S1", money("1")));
        checkThrows("unknown account", IllegalArgumentException.class,
                () -> bank.transfer("S1", "NOPE", money("1")));
    }

    static void interestIsAddedAndRounded() {
        SavingsAccount s = new SavingsAccount("S1", "Sam", money("1000"));
        s.addInterest(new BigDecimal("0.005"));
        checkEquals("0.5% interest on 1000", money("1005.00"), s.getBalance());
        SavingsAccount t = new SavingsAccount("S2", "Tia", money("0.99"));
        t.addInterest(new BigDecimal("0.005")); // 0.00495 rounds to 0.00 -> no entry
        checkEquals("tiny interest adds nothing", 0, t.getHistory().size());
        checkThrows("negative rate", IllegalArgumentException.class, () -> s.addInterest(new BigDecimal("-0.1")));
    }

    static void duplicateIdRejected() {
        Bank bank = sampleBank();
        checkThrows("duplicate id", IllegalArgumentException.class,
                () -> bank.addAccount(new SavingsAccount("S1", "Other", money("1"))));
    }

    static void historyCannotBeModified() {
        Account a = new SavingsAccount("S1", "Sam", money("10"));
        a.deposit(money("1"));
        checkThrows("history is read-only", UnsupportedOperationException.class, () -> a.getHistory().clear());
    }

    static void saveAndLoadRoundTrip() throws IOException, InsufficientFundsException {
        Bank bank = sampleBank();
        bank.getAccount("C1").withdraw(money("130")); // overdrawn to -30
        Path file = Files.createTempFile("bank-test", ".csv");
        try {
            bank.save(file);
            Bank loaded = Bank.load(file);
            checkEquals("account count", 2, loaded.getAccounts().size());
            checkEquals("savings balance", money("100.00"), loaded.getAccount("S1").getBalance());
            checkEquals("overdrawn checking balance", money("-30.00"), loaded.getAccount("C1").getBalance());
            checkEquals("overdraft limit kept", money("50.00"),
                    ((CheckingAccount) loaded.getAccount("C1")).getOverdraftLimit());
            checkEquals("type kept", "CHECKING", loaded.getAccount("C1").getType());
        } finally {
            Files.deleteIfExists(file);
        }
        Path bad = Files.createTempFile("bank-bad", ".csv");
        try {
            Files.writeString(bad, "SAVINGS,S1,Sam\n");
            checkThrows("malformed file rejected", IllegalArgumentException.class, () -> Bank.load(bad));
        } finally {
            Files.deleteIfExists(bad);
        }
    }

    // ---- helpers ----

    static Bank sampleBank() {
        Bank bank = new Bank();
        bank.addAccount(new SavingsAccount("S1", "Sam", money("100")));
        bank.addAccount(new CheckingAccount("C1", "Cy", money("100"), money("50")));
        return bank;
    }

    static BigDecimal money(String s) {
        return new BigDecimal(s).setScale(2);
    }

    interface Action { void run() throws Exception; }

    static void checkThrows(String name, Class<? extends Exception> expected, Action action) {
        try {
            action.run();
            fail(name, "expected " + expected.getSimpleName() + " but nothing was thrown");
        } catch (Exception e) {
            if (expected.isInstance(e)) pass(name);
            else fail(name, "expected " + expected.getSimpleName() + " but got " + e.getClass().getSimpleName());
        }
    }

    static void checkEquals(String name, Object expected, Object actual) {
        boolean same = (expected instanceof BigDecimal e && actual instanceof BigDecimal a)
                ? e.compareTo(a) == 0
                : expected.equals(actual);
        if (same) pass(name);
        else fail(name, "expected " + expected + " but got " + actual);
    }

    static void pass(String name) { passed++; System.out.println("PASS  " + name); }
    static void fail(String name, String why) { failed++; System.out.println("FAIL  " + name + " -- " + why); }
}
