package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.AppException;
import com.transcribeenglish.tts.exception.AudioException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Operações de áudio com FFmpeg: normalização, silêncio, concatenação e MP3.
 */
public class AudioService {

    private final ExternalCommandRunner commandRunner;

    public AudioService(ExternalCommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public void ensureAvailable() throws AudioException {
        try {
            commandRunner.ensureCommandAvailable(Config.FFMPEG_COMMAND, "FFmpeg");
        } catch (AppException e) {
            throw new AudioException(
                    "FFmpeg não instalado ou inacessível (comando: \"" + Config.FFMPEG_COMMAND + "\").\n"
                            + "O FFmpeg é necessário para inserir silêncio, concatenar áudios e gerar o MP3 final.\n"
                            + "Veja o README para instruções de instalação.\n"
                            + "Detalhe: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Normaliza áudio de entrada (WAV/MP3) para WAV uniforme (mono + sample rate fixo).
     */
    public void normalizeToWav(Path inputAudio, Path outputWav, int phraseIndex) throws AudioException {
        List<String> command = ExternalCommandRunner.command(
                Config.FFMPEG_COMMAND,
                "-y",
                "-i", inputAudio.toAbsolutePath().toString(),
                "-ar", String.valueOf(Config.SAMPLE_RATE_HZ),
                "-ac", "1",
                "-c:a", "pcm_s16le",
                outputWav.toAbsolutePath().toString()
        );

        try {
            commandRunner.run(
                    command,
                    "Erro ao converter áudio da frase [" + phraseIndex + "]");
            validateNonEmpty(outputWav, "Áudio normalizado da frase [" + phraseIndex + "]");
        } catch (AppException e) {
            throw new AudioException(e.getMessage(), e);
        }
    }

    /**
     * Gera um WAV de silêncio com a duração configurada.
     */
    public void createSilenceWav(Path silenceWav, int durationMs) throws AudioException {
        if (durationMs < 0) {
            throw new AudioException("INTERVALO_ENTRE_FRASES_MS não pode ser negativo: " + durationMs);
        }

        double seconds = durationMs / 1000.0;
        String duration = String.format(Locale.US, "%.3f", seconds);

        List<String> command = ExternalCommandRunner.command(
                Config.FFMPEG_COMMAND,
                "-y",
                "-f", "lavfi",
                "-i", "anullsrc=channel_layout=mono:sample_rate=" + Config.SAMPLE_RATE_HZ,
                "-t", duration,
                "-c:a", "pcm_s16le",
                silenceWav.toAbsolutePath().toString()
        );

        try {
            Path parent = silenceWav.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            commandRunner.run(command, "Erro ao gerar silêncio entre frases");
            validateNonEmpty(silenceWav, "Arquivo de silêncio");
        } catch (AppException e) {
            throw new AudioException(e.getMessage(), e);
        } catch (IOException e) {
            throw new AudioException("Não foi possível criar pasta para o silêncio: " + e.getMessage(), e);
        }
    }

    /**
     * Concatena WAVs (frases + silêncios) e exporta um único MP3.
     */
    public void concatenateToMp3(List<Path> wavSegments, Path concatListFile, Path outputMp3)
            throws AudioException {
        if (wavSegments.isEmpty()) {
            throw new AudioException("Nenhum segmento de áudio para concatenar.");
        }

        try {
            Path outputParent = outputMp3.getParent();
            if (outputParent != null) {
                Files.createDirectories(outputParent);
            }

            if (Files.exists(outputMp3) && !Files.isWritable(outputMp3)) {
                throw new AudioException(
                        "Sem permissão de escrita no arquivo de saída: " + outputMp3.toAbsolutePath());
            }
            if (outputParent != null && Files.exists(outputParent) && !Files.isWritable(outputParent)) {
                throw new AudioException(
                        "Sem permissão de escrita na pasta de saída: " + outputParent.toAbsolutePath());
            }

            String listContent = wavSegments.stream()
                    .map(path -> "file '" + toFfmpegPath(path) + "'")
                    .collect(Collectors.joining(System.lineSeparator()));

            Files.writeString(concatListFile, listContent, StandardCharsets.UTF_8);

            List<String> command = ExternalCommandRunner.command(
                    Config.FFMPEG_COMMAND,
                    "-y",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", concatListFile.toAbsolutePath().toString(),
                    "-c:a", "libmp3lame",
                    "-q:a", "2",
                    outputMp3.toAbsolutePath().toString()
            );

            commandRunner.run(command, "Erro ao criar o arquivo MP3 final");
            validateNonEmpty(outputMp3, "Arquivo MP3 final");
        } catch (AudioException e) {
            throw e;
        } catch (AppException e) {
            throw new AudioException(e.getMessage(), e);
        } catch (IOException e) {
            throw new AudioException(
                    "Erro de I/O ao gerar o MP3 (verifique permissões e espaço em disco): "
                            + e.getMessage(),
                    e);
        }
    }

    private static void validateNonEmpty(Path file, String label) throws AudioException {
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                throw new AudioException(label + " não foi gerado corretamente: " + file.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new AudioException("Não foi possível validar " + label + ": " + e.getMessage(), e);
        }
    }

    /**
     * Caminhos absolutos com barra normalizada — necessário para o demuxer concat do FFmpeg no Windows.
     */
    static String toFfmpegPath(Path path) {
        String absolute = path.toAbsolutePath().normalize().toString().replace('\\', '/');
        return absolute.replace("'", "'\\''");
    }
}
