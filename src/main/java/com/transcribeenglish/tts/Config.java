package com.transcribeenglish.tts;

/**
 * Constantes centralizadas da aplicação.
 * Altere estes valores para ajustar idioma, intervalo e caminhos.
 */
public final class Config {

    /** Locale/idioma enviado ao Google TTS (ex.: en-US, en-GB, pt-BR). */
    public static final String IDIOMA = "en-US";

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
     * Comando do FFmpeg.
     * No Windows/IntelliJ, prefira o caminho completo se o PATH não for herdado.
     */
    public static final String FFMPEG_COMMAND = "C:\\Users\\roger.carvalho\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-9.0.1-full_build\\bin\\ffmpeg.exe";

    /** Taxa de amostragem padronizada para concatenação estável. */
    public static final int SAMPLE_RATE_HZ = 22050;

    private Config() {
    }
}
