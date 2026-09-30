import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public void addAccount(Account account) {
        if (accounts.containsKey(account.getId())) {
            throw new IllegalArgumentException("An account with id " + account.getId() + " already exists.");
        }
        accounts.put(account.getId(), account);
    }

    public Account getAccount(String id) {
        Account account = id == null ? null : accounts.get(id.trim());
        if (account == null) {
            throw new IllegalArgumentException("No account with id " + id + ".");
        }
        return account;
    }

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public void transfer(String fromId, String toId, BigDecimal amount) throws InsufficientFundsException {
        if (fromId != null && fromId.trim().equals(toId == null ? null : toId.trim())) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        Account from = getAccount(fromId);
        Account to = getAccount(toId);
        BigDecimal amt = Account.validateAmount(amount);
        from.debit(Transaction.Type.TRANSFER_OUT, amt);
        to.credit(Transaction.Type.TRANSFER_IN, amt);
    }

    public void save(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Account a : accounts.values()) {
            String overdraft = (a instanceof CheckingAccount c) ? c.getOverdraftLimit().toPlainString() : "";
            lines.add(String.join(",", a.getType(), a.getId(), a.getOwner(),
                    a.getBalance().toPlainString(), overdraft));
        }
        Files.write(file, lines);
    }

    public static Bank load(Path file) throws IOException {
        Bank bank = new Bank();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file)) {
            lineNumber++;
            if (line.isBlank()) continue;
            String[] p = line.split(",", -1);
            if (p.length != 5) {
                throw new IllegalArgumentException("Bad data on line " + lineNumber + ".");
            }
            BigDecimal balance = new BigDecimal(p[3]);
            Account account = switch (p[0]) {
                case "CHECKING" -> new CheckingAccount(p[1], p[2], BigDecimal.ZERO, new BigDecimal(p[4]));
                case "SAVINGS" -> new SavingsAccount(p[1], p[2], BigDecimal.ZERO);
                default -> throw new IllegalArgumentException("Unknown account type on line " + lineNumber + ".");
            };
            account.restoreBalance(balance);
            bank.addAccount(account);
        }
        return bank;
    }
}
