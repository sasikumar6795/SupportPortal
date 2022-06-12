package com.supportportal.exception;

public class UserNameExistException extends RuntimeException{

    public UserNameExistException(String message) {
        super(message);
    }
}
