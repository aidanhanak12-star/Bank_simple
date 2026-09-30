import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/** Command-line menu for the bank. Accounts are saved to accounts.csv on exit. */
public class Main {
    private static final Path DATA_FILE = Path.of("accounts.csv");
    private static final Scanner IN = new Scanner(System.in);

    public static void main(String[] args) {
        Bank bank = new Bank();
        if (Files.exists(DATA_FILE)) {
            try {
                bank = Bank.load(DATA_FILE);
                System.out.println("Loaded " + bank.getAccounts().size() + " account(s) from " + DATA_FILE + ".");
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Could not load saved accounts: " + e.getMessage());
            }
        }

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Choose an option: ");
            if (!IN.hasNextLine()) break; // input ended
            String choice = IN.nextLine();
            try {
                switch (choice.trim()) {
                    case "1" -> createChecking(bank);
                    case "2" -> createSavings(bank);
                    case "3" -> deposit(bank);
                    case "4" -> withdraw(bank);
                    case "5" -> transfer(bank);
                    case "6" -> showAccount(bank);
                    case "7" -> listAccounts(bank);
                    case "8" -> addInterest(bank);
                    case "0" -> running = false;
                    default -> System.out.println("Unknown option.");
                }
            } catch (IllegalArgumentException | InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        try {
            bank.save(DATA_FILE);
            System.out.println("Saved to " + DATA_FILE + ". Goodbye.");
        } catch (IOException e) {
            System.out.println("Could not save accounts: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("""

                1) Open checking account     5) Transfer
                2) Open savings account      6) Show account and history
                3) Deposit                   7) List all accounts
                4) Withdraw                  8) Add interest (savings)
                0) Save and exit""");
    }

    private static void createChecking(Bank bank) {
        String id = prompt("New account id: ");
        String owner = prompt("Owner name: ");
        BigDecimal opening = readMoney("Opening balance: ");
        BigDecimal overdraft = readMoney("Overdraft limit (0 for none): ");
        bank.addAccount(new CheckingAccount(id, owner, opening, overdraft));
        System.out.println("Checking account opened.");
    }

    private static void createSavings(Bank bank) {
        String id = prompt("New account id: ");
        String owner = prompt("Owner name: ");
        BigDecimal opening = readMoney("Opening balance: ");
        bank.addAccount(new SavingsAccount(id, owner, opening));
        System.out.println("Savings account opened.");
    }

    private static void deposit(Bank bank) {
        Account a = bank.getAccount(prompt("Account id: "));
        a.deposit(readMoney("Amount: "));
        System.out.println("New balance: " + a.getBalance());
    }

    private static void withdraw(Bank bank) throws InsufficientFundsException {
        Account a = bank.getAccount(prompt("Account id: "));
        a.withdraw(readMoney("Amount: "));
        System.out.println("New balance: " + a.getBalance());
    }

    private static void transfer(Bank bank) throws InsufficientFundsException {
        String from = prompt("From account id: ");
        String to = prompt("To account id: ");
        bank.transfer(from, to, readMoney("Amount: "));
        System.out.println("Transfer complete.");
    }

    private static void showAccount(Bank bank) {
        Account a = bank.getAccount(prompt("Account id: "));
        System.out.println(a);
        if (a.getHistory().isEmpty()) {
            System.out.println("  (no transactions yet)");
        }
        a.getHistory().forEach(t -> System.out.println("  " + t));
    }

    private static void listAccounts(Bank bank) {
        if (bank.getAccounts().isEmpty()) {
            System.out.println("No accounts yet.");
        }
        bank.getAccounts().forEach(System.out::println);
    }

    private static void addInterest(Bank bank) {
        Account a = bank.getAccount(prompt("Account id: "));
        if (!(a instanceof SavingsAccount savings)) {
            throw new IllegalArgumentException("Interest only applies to savings accounts.");
        }
        savings.addInterest(readRate("Interest rate for this period (e.g. 0.005 for 0.5%): "));
        System.out.println("New balance: " + savings.getBalance());
    }

    // ---- input helpers ----

    /** Returns the typed line, or throws if input has ended. */
    private static String prompt(String message) {
        System.out.print(message);
        if (!IN.hasNextLine()) {
            throw new IllegalArgumentException("Input ended.");
        }
        return IN.nextLine();
    }

    private static BigDecimal readMoney(String message) {
        return parseNumber(prompt(message));
    }

    private static BigDecimal readRate(String message) {
        return parseNumber(prompt(message));
    }

    private static BigDecimal parseNumber(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number.");
        }
    }
}
