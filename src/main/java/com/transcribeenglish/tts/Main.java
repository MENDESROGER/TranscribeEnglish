package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.AppException;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada da aplicação.
 */
public final class Main {

    public static void main(String[] args) {
        configureConsoleUtf8();

        ExternalCommandRunner commandRunner = new ExternalCommandRunner();
        TextFileReader textFileReader = new TextFileReader();
        TtsService ttsService = new TtsService(commandRunner);
        AudioService audioService = new AudioService(commandRunner);
        AudioGenerator generator = new AudioGenerator(textFileReader, ttsService, audioService);

        try {
            generator.generate();
        } catch (AppException e) {
            System.err.println();
            System.err.println("ERRO: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("ERRO inesperado: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void configureConsoleUtf8() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    private Main() {
    }
}
