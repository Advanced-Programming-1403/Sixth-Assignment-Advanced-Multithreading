package Banking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionProcessor implements Runnable {
    private final BankAccount account;
    private final String fileName;
    private final List<BankAccount> allAccounts;
    private final Lock logLock = new ReentrantLock();
    private static int transactionCounter = 0;
    private static final Lock counterLock = new ReentrantLock();

    public TransactionProcessor(BankAccount account, String fileName, List<BankAccount> allAccounts) {
        this.account = account;
        this.fileName = fileName;
        this.allAccounts = allAccounts;
    }

    @Override
    public void run() {
        logWithLock("Starting processing for account " + account.getId());

        try (InputStreamReader is = new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName)));
             BufferedReader reader = new BufferedReader(is)) {

            String line;
            while ((line = reader.readLine()) != null) {
                processTransactionLine(line.trim());
            }

        } catch (Exception e) {
            logWithLock("Error processing file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            logWithLock("Completed processing for account " + account.getId());
        }
    }

    private void processTransactionLine(String line) {
        if (line.isEmpty()) return;

        String[] parts = line.split("\\s+");
        String transactionType = parts[0];

        try {
            switch (transactionType) {
                case "Deposit":
                    int depositAmount = Integer.parseInt(parts[1]);
                    account.deposit(depositAmount);
                    logTransaction(transactionType, depositAmount);
                    break;

                case "Withdraw":
                    int withdrawAmount = Integer.parseInt(parts[1]);
                    account.withdraw(withdrawAmount);
                    logTransaction(transactionType, withdrawAmount);
                    break;

                case "Transfer":
                    int targetIndex = Integer.parseInt(parts[1]) - 1;
                    int transferAmount = Integer.parseInt(parts[2]);
                    BankAccount target = allAccounts.get(targetIndex);
                    account.transfer(target, transferAmount);
                    logTransfer(transferAmount, target);
                    break;

                default:
                    logWithLock("Unknown transaction type: " + transactionType);
            }

            incrementTransactionCounter();

        } catch (NumberFormatException e) {
            logWithLock("Invalid number format in transaction: " + line);
        } catch (IndexOutOfBoundsException e) {
            logWithLock("Invalid target account in transfer: " + line);
        } catch (Exception e) {
            logWithLock("Error processing transaction: " + line + " - " + e.getMessage());
        }
    }

    private void logTransaction(String type, int amount) {
        logWithLock(String.format("Processed %s of %d for account %d. New balance: %d",
                type, amount, account.getId(), account.getBalance()));
    }

    private void logTransfer(int amount, BankAccount target) {
        logWithLock(String.format("Transfer %d from account %d to %d. Balances: %d -> %d",
                amount, account.getId(), target.getId(),
                account.getBalance(), target.getBalance()));
    }

    private void logWithLock(String message) {
        logLock.lock();
        try {
            System.out.println("[Thread-" + Thread.currentThread().getId() + "] " + message);
        } finally {
            logLock.unlock();
        }
    }

    private void incrementTransactionCounter() {
        counterLock.lock();
        try {
            transactionCounter++;
        } finally {
            counterLock.unlock();
        }
    }

    public static int getTransactionCount() {
        counterLock.lock();
        try {
            return transactionCounter;
        } finally {
            counterLock.unlock();
        }
    }
}