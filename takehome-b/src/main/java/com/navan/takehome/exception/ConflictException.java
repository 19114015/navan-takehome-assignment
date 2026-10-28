package com.navan.takehome.exception;

public class ConflictException extends RuntimeException {
    private final Object existingResource;
    public ConflictException(String message) {
        super(message);
        this.existingResource = null;
    }
    public ConflictException(String message, Object existingResource) {
        super(message);
        this.existingResource = existingResource;
    }
    public Object getExistingResource() { return existingResource; }
}
