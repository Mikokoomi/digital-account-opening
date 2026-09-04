package com.digitalbank.accountopening.integration.corebanking;

import java.util.UUID;
public record CoreBankingCreateAccountRequest(UUID applicationId, String customerId, String productCode) {}
