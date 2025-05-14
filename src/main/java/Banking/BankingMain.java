package Banking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BankingMain {
    private static final int INITIAL_BALANCE = 20000;
    private static final int NUM_ACCOUNTS = 4;
    private static final long TIMEOUT_MINUTES = 2;

    public List<BankAccount> calculate() throws InterruptedException {
        System.out.println("Initializing banking system with " + NUM_ACCOUNTS + " accounts...");

        List<BankAccount> accounts = initializeAccounts();
        Thread[] threads = createAndStartThreads(accounts);

        waitForThreadsCompletion(threads);

        System.out.println("\nAll transactions processed successfully!");
        System.out.println("Total transactions processed: " + TransactionProcessor.getTransactionCount());

        return accounts;
    }

    private List<BankAccount> initializeAccounts() {
        List<BankAccount> accounts = new ArrayList<>();
        for (int i = 1; i <= NUM_ACCOUNTS; i++) {
            accounts.add(new BankAccount(i, INITIAL_BALANCE));
            System.out.printf("Account %d initialized with balance: %,d%n", i, INITIAL_BALANCE);
        }
        return accounts;
    }

    private Thread[] createAndStartThreads(List<BankAccount> accounts) {
        Thread[] threads = new Thread[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String fileName = (i + 1) + ".txt";
            threads[i] = new Thread(new TransactionProcessor(accounts.get(i), fileName, accounts));
            threads[i].setName("TransactionProcessor-" + (i + 1));
            threads[i].start();
            System.out.println("Started thread for " + threads[i].getName());
        }
        return threads;
    }

    private void waitForThreadsCompletion(Thread[] threads) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        for (Thread thread : threads) {
            thread.join(TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES));

            if (thread.isAlive()) {
                System.err.println("Warning: Thread " + thread.getName() + " did not complete within timeout!");
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("\nProcessing completed in %.2f seconds%n", duration / 1000.0);
    }

    public static void main(String[] args) {
        try {
            BankingMain main = new BankingMain();
            System.out.println("\nStarting banking simulation...\n");

            List<BankAccount> accounts = main.calculate();

            System.out.println("\nFinal Account Balances:");
            for (BankAccount account : accounts) {
                System.out.printf("Account %d: %,d%n",
                        account.getId(), account.getBalance());
            }

            verifyTotalBalance(accounts, NUM_ACCOUNTS * INITIAL_BALANCE);

        } catch (InterruptedException e) {
            System.err.println("Banking simulation was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error in banking simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void verifyTotalBalance(List<BankAccount> accounts, int expectedTotal) {
        int actualTotal = accounts.stream()
                .mapToInt(BankAccount::getBalance)
                .sum();

        System.out.printf("\nBalance Verification:%nExpected Total: %,d%nActual Total: %,d%n",
                expectedTotal, actualTotal);

        if (actualTotal != expectedTotal) {
            System.err.println("WARNING: Balance discrepancy detected!");
        } else {
            System.out.println("Balance verification successful!");
        }
    }
}