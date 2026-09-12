package com.transcribeenglish.tts.exception;

/**
 * Exceção de domínio da aplicação, com mensagem amigável para o usuário.
 */
public class AppException extends Exception {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
