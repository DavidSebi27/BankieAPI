package com.bankie.bankie_api.exception;

public class BsnAlreadyExistsException extends RuntimeException {
    public BsnAlreadyExistsException() {
        super("BSN already registered");
    }
}