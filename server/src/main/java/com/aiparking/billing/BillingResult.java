package com.aiparking.billing;

public record BillingResult(int billedMinutes, int amountCents, boolean freeApplied) {
}
