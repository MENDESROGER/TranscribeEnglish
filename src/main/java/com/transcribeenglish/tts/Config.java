package com.transcribeenglish.tts;

/**
 * Constantes centralizadas da aplicação.
 * Altere estes valores para ajustar idioma, intervalo, motor TTS e caminhos.
 */
public final class Config {

    /**
     * Motor TTS:
     * - "gtts"    → Google Text-to-Speech (gratuito, via Translate TTS; precisa de internet)
     * - "windows" → vozes nativas do Windows
     * - "espeak"  → eSpeak NG (local, robótico)
     */
    public static final String TTS_ENGINE = "gtts";

    /** Locale/idioma (ex.: en-US, en-GB, pt-BR). */
    public static final String IDIOMA = "en-US";

    /**
     * Nome opcional da voz do Windows.
     * Deixe vazio para escolher automaticamente pelo idioma.
     * Exemplo: "Microsoft Zira Desktop"
     */
    public static final String WINDOWS_VOICE_NAME = "Microsoft Zira Desktop";

    /** Velocidade da fala no Windows: -10 (lento) a 10 (rápido). 0 = normal. */
    public static final int WINDOWS_SPEECH_RATE = -1;

    /** Pausa entre requisições gTTS para reduzir bloqueios (ms). */
    public static final int GTTS_DELAY_MS = 250;

    /** Silêncio real inserido entre cada frase no áudio final. */
    public static final int INTERVALO_ENTRE_FRASES_MS = 1500;

    public static final String PASTA_TEXTO = "texto";
    public static final String ARQUIVO_TEXTO = "frases.txt";

    public static final String PASTA_OUTPUT = "output";
    public static final String ARQUIVO_OUTPUT = "frases.mp3";

    /** Pasta temporária usada durante a geração (removida ao final). */
    public static final String PASTA_TEMP = "output/.tmp-tts";

    /**
     * Comandos externos.
     * No Windows, prefira o caminho completo — o IntelliJ muitas vezes não herda
     * o PATH atualizado após a instalação das ferramentas.
     */
    public static final String ESPEAK_COMMAND = "C:\\Program Files\\eSpeak NG\\espeak-ng.exe";
    public static final String ESPEAK_FALLBACK_COMMAND = "espeak";
    public static final String FFMPEG_COMMAND = "C:\\Users\\roger.carvalho\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-9.0.1-full_build\\bin\\ffmpeg.exe";
    public static final String POWERSHELL_COMMAND = "powershell";

    /** Taxa de amostragem padronizada para concatenação estável. */
    public static final int SAMPLE_RATE_HZ = 22050;

    private Config() {
    }
}
