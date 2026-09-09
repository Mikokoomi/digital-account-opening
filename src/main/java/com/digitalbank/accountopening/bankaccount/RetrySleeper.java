package com.digitalbank.accountopening.bankaccount;

public interface RetrySleeper {
    void sleep(long millis) throws InterruptedException;
}
