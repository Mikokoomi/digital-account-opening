package com.digitalbank.accountopening.bankaccount;

import org.springframework.stereotype.Component;

@Component
public class ThreadRetrySleeper implements RetrySleeper {
    @Override public void sleep(long millis) throws InterruptedException { Thread.sleep(millis); }
}
