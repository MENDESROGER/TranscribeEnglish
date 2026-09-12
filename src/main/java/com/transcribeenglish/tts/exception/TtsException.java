package com.transcribeenglish.tts.exception;

public class TtsException extends AppException {

    public TtsException(String message) {
        super(message);
    }

    public TtsException(String message, Throwable cause) {
        super(message, cause);
    }
}
