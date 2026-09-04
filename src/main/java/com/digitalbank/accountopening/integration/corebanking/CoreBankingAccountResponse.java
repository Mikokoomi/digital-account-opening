package com.digitalbank.accountopening.integration.corebanking;

import java.time.OffsetDateTime;
import java.util.UUID;
public record CoreBankingAccountResponse(UUID accountId, String accountNumber, UUID applicationId,
                                         String customerId, String productCode, String status,
                                         OffsetDateTime openedAt) {}
