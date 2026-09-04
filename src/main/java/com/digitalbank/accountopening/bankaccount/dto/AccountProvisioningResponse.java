package com.digitalbank.accountopening.bankaccount.dto;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
public record AccountProvisioningResponse(UUID applicationId, ApplicationStatus applicationStatus,
                                          String accountNumber, String accountStatus,
                                          OffsetDateTime openedAt) {}
