package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.TtsException;

import java.nio.file.Path;

/**
 * Síntese de voz via Google Text-to-Speech (gTTS).
 */
public class TtsService {

    private final GttsClient gttsClient;
    private boolean ready;

    public TtsService() {
        this.gttsClient = new GttsClient();
    }

    public void ensureAvailable() throws TtsException {
        gttsClient.ensureAvailable();
        ready = true;
    }

    /**
     * Gera o áudio bruto da frase em MP3.
     */
    public void synthesize(String phrase, Path outputMp3, int phraseIndex) throws TtsException {
        if (!ready) {
            ensureAvailable();
        }
        gttsClient.synthesizeToMp3(phrase, outputMp3, phraseIndex);
    }
}
