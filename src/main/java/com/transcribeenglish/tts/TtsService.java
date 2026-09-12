package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.AppException;
import com.transcribeenglish.tts.exception.TtsException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Síntese de voz.
 * Engines: gtts (Google), windows (SAPI) e espeak.
 */
public class TtsService {

    private final ExternalCommandRunner commandRunner;
    private final GttsClient gttsClient;
    private String activeEngine;

    private String resolvedEspeakCommand;
    private Path espeakExecutablePath;
    private Path espeakDataPath;
    private Path espeakWorkingDirectory;
    private Path windowsScriptPath;

    public TtsService(ExternalCommandRunner commandRunner) {
        this.commandRunner = commandRunner;
        this.gttsClient = new GttsClient();
    }

    public String getActiveEngine() {
        return activeEngine;
    }

    /** Extensão do arquivo bruto gerado pelo motor atual (.mp3 ou .wav). */
    public String rawAudioExtension() {
        if ("gtts".equals(activeEngine)) {
            return ".mp3";
        }
        return ".wav";
    }

    public void ensureAvailable() throws TtsException {
        String requested = Config.TTS_ENGINE == null ? "gtts" : Config.TTS_ENGINE.trim().toLowerCase(Locale.ROOT);

        if ("gtts".equals(requested)) {
            gttsClient.ensureAvailable();
            activeEngine = "gtts";
            return;
        }

        if ("windows".equals(requested)) {
            if (!isWindows()) {
                throw new TtsException(
                        "TTS_ENGINE=windows só funciona no Windows.\n"
                                + "Altere Config.TTS_ENGINE para \"gtts\" ou \"espeak\".");
            }
            ensureWindowsAvailable();
            activeEngine = "windows";
            return;
        }

        if ("espeak".equals(requested)) {
            ensureEspeakAvailable();
            activeEngine = "espeak";
            return;
        }

        throw new TtsException(
                "TTS_ENGINE inválido: \"" + Config.TTS_ENGINE + "\".\n"
                        + "Use \"gtts\" (recomendado), \"windows\" ou \"espeak\".");
    }

    /**
     * Gera áudio bruto da frase (MP3 no gTTS, WAV nos demais).
     */
    public void synthesize(String phrase, Path outputAudio, int phraseIndex) throws TtsException {
        if (activeEngine == null) {
            ensureAvailable();
        }

        if ("gtts".equals(activeEngine)) {
            gttsClient.synthesizeToMp3(phrase, outputAudio, phraseIndex);
            return;
        }
        if ("windows".equals(activeEngine)) {
            synthesizeWithWindows(phrase, outputAudio, phraseIndex);
            return;
        }
        synthesizeWithEspeak(phrase, outputAudio, phraseIndex);
    }

    private void ensureWindowsAvailable() throws TtsException {
        try {
            commandRunner.ensureCommandAvailable(Config.POWERSHELL_COMMAND, "PowerShell");
            windowsScriptPath = extractWindowsScript();
        } catch (AppException e) {
            throw new TtsException(
                    "Não foi possível preparar o TTS do Windows (System.Speech).\n"
                            + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new TtsException(
                    "Falha ao extrair o script de síntese do Windows: " + e.getMessage(), e);
        }
    }

    private void ensureEspeakAvailable() throws TtsException {
        try {
            commandRunner.ensureCommandAvailable(Config.ESPEAK_COMMAND, "eSpeak NG");
            resolvedEspeakCommand = Config.ESPEAK_COMMAND;
        } catch (AppException primary) {
            try {
                commandRunner.ensureCommandAvailable(Config.ESPEAK_FALLBACK_COMMAND, "eSpeak");
                resolvedEspeakCommand = Config.ESPEAK_FALLBACK_COMMAND;
            } catch (AppException fallback) {
                throw new TtsException(
                        "Ferramenta TTS não instalada ou inacessível.\n"
                                + "Tentou \"" + Config.ESPEAK_COMMAND + "\" e \""
                                + Config.ESPEAK_FALLBACK_COMMAND + "\".\n"
                                + "Ou use TTS_ENGINE=\"gtts\" / \"windows\".\n"
                                + "Detalhe: " + primary.getMessage(),
                        primary);
            }
        }

        try {
            espeakExecutablePath = commandRunner.resolveExecutablePath(resolvedEspeakCommand);
            espeakWorkingDirectory = espeakExecutablePath.getParent();
            if (espeakWorkingDirectory != null) {
                Path candidate = espeakWorkingDirectory.resolve("espeak-ng-data");
                if (Files.isDirectory(candidate)) {
                    espeakDataPath = candidate;
                }
            }
        } catch (AppException e) {
            throw new TtsException(
                    "eSpeak encontrado, mas não foi possível resolver o caminho do executável.\n"
                            + e.getMessage(),
                    e);
        }
    }

    private void synthesizeWithWindows(String phrase, Path outputWav, int phraseIndex) throws TtsException {
        Path phraseTextFile = outputWav.resolveSibling(String.format("phrase_%03d.txt", phraseIndex));

        try {
            Path parent = outputWav.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(phraseTextFile, phrase, StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>();
            command.add(Config.POWERSHELL_COMMAND);
            command.add("-NoProfile");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(windowsScriptPath.toAbsolutePath().toString());
            command.add("-TextFile");
            command.add(phraseTextFile.toAbsolutePath().toString());
            command.add("-OutputWav");
            command.add(outputWav.toAbsolutePath().toString());
            command.add("-Culture");
            command.add(Config.IDIOMA);
            command.add("-VoiceName");
            command.add(Config.WINDOWS_VOICE_NAME == null ? "" : Config.WINDOWS_VOICE_NAME);
            command.add("-Rate");
            command.add(String.valueOf(Config.WINDOWS_SPEECH_RATE));

            commandRunner.run(
                    command,
                    "Erro no TTS Windows ao processar a frase [" + phraseIndex + "]: \""
                            + truncate(phrase) + "\"");

            validateAudio(outputWav, phrase, phraseIndex, Config.WINDOWS_VOICE_NAME);
        } catch (TtsException e) {
            throw e;
        } catch (AppException e) {
            throw new TtsException(
                    "Falha no TTS Windows na frase [" + phraseIndex + "]: \"" + truncate(phrase) + "\".\n"
                            + e.getMessage(),
                    e);
        } catch (Exception e) {
            throw new TtsException(
                    "Erro inesperado no TTS Windows na frase [" + phraseIndex + "]: \""
                            + truncate(phrase) + "\". " + e.getMessage(),
                    e);
        }
    }

    private void synthesizeWithEspeak(String phrase, Path outputWav, int phraseIndex) throws TtsException {
        String voice = toEspeakVoice(Config.IDIOMA);
        Path phraseTextFile = outputWav.resolveSibling(String.format("phrase_%03d.txt", phraseIndex));

        try {
            Path parent = outputWav.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(phraseTextFile, phrase, StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>();
            command.add(espeakExecutablePath != null ? espeakExecutablePath.toString() : resolvedEspeakCommand);
            command.add("-v");
            command.add(voice);
            command.add("-w");
            command.add(outputWav.toAbsolutePath().toString());
            command.add("-f");
            command.add(phraseTextFile.toAbsolutePath().toString());

            Map<String, String> env = null;
            if (espeakDataPath != null) {
                env = ExternalCommandRunner.env("ESPEAK_DATA_PATH", espeakDataPath.toAbsolutePath().toString());
            }

            commandRunner.run(
                    command,
                    "Erro no TTS ao processar a frase [" + phraseIndex + "]: \"" + truncate(phrase) + "\"",
                    espeakWorkingDirectory,
                    env);

            validateAudio(outputWav, phrase, phraseIndex, voice);
        } catch (TtsException e) {
            throw e;
        } catch (AppException e) {
            throw new TtsException(
                    "Falha no TTS na frase [" + phraseIndex + "]: \"" + truncate(phrase) + "\".\n"
                            + e.getMessage(),
                    e);
        } catch (Exception e) {
            throw new TtsException(
                    "Erro inesperado ao gerar áudio da frase [" + phraseIndex + "]: \""
                            + truncate(phrase) + "\". " + e.getMessage(),
                    e);
        }
    }

    private Path extractWindowsScript() throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "transcribe-english-tts");
        Files.createDirectories(dir);
        Path script = dir.resolve("windows-sapi-synth.ps1");

        try (InputStream in = TtsService.class.getResourceAsStream("/tts/windows-sapi-synth.ps1")) {
            if (in == null) {
                throw new IOException("Resource /tts/windows-sapi-synth.ps1 não encontrado no JAR/classpath.");
            }
            Files.write(script, in.readAllBytes());
        }
        return script;
    }

    private static void validateAudio(Path output, String phrase, int phraseIndex, String voiceLabel)
            throws TtsException, IOException {
        if (!Files.exists(output) || Files.size(output) == 0) {
            throw new TtsException(
                    "O TTS não gerou áudio válido para a frase [" + phraseIndex + "]: \""
                            + truncate(phrase) + "\".\n"
                            + "Voz/idioma: " + voiceLabel);
        }
    }

    static String toEspeakVoice(String idioma) {
        if (idioma == null || idioma.isBlank()) {
            return "en";
        }
        return idioma.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String truncate(String text) {
        if (text.length() <= 80) {
            return text;
        }
        return text.substring(0, 77) + "...";
    }
}
