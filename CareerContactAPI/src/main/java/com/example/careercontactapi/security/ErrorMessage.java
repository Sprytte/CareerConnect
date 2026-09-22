package com.example.careercontactapi.security;

import lombok.Generated;
import lombok.Value;

@Generated
@Value
public class ErrorMessage {
    String message;

    public static ErrorMessage from(final String message) {
        return new ErrorMessage(message);
    }
}