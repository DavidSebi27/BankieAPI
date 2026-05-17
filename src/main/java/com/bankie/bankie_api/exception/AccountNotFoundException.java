package com.bankie.bankie_api.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String iban) {
        super("Account not found: " + iban);
    }
}