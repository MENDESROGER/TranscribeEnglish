package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.AppException;
import com.transcribeenglish.tts.exception.AudioException;
import com.transcribeenglish.tts.exception.TextFileException;
import com.transcribeenglish.tts.exception.TtsException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra leitura do texto, TTS, silêncio e geração do MP3 único.
 */
public class AudioGenerator {

    private final TextFileReader textFileReader;
    private final TtsService ttsService;
    private final AudioService audioService;

    public AudioGenerator(TextFileReader textFileReader, TtsService ttsService, AudioService audioService) {
        this.textFileReader = textFileReader;
        this.ttsService = ttsService;
        this.audioService = audioService;
    }

    public Path generate() throws AppException {
        Path textFile = Path.of(Config.PASTA_TEXTO, Config.ARQUIVO_TEXTO);
        Path outputMp3 = Path.of(Config.PASTA_OUTPUT, Config.ARQUIVO_OUTPUT);
        Path tempDir = Path.of(Config.PASTA_TEMP);

        printHeader(textFile);

        List<String> phrases = textFileReader.readPhrases(textFile);
        System.out.println("Frases encontradas: " + phrases.size());
        System.out.println();

        ttsService.ensureAvailable();
        audioService.ensureAvailable();

        try {
            prepareTempDir(tempDir);

            Path silenceWav = tempDir.resolve("silence.wav");
            if (Config.INTERVALO_ENTRE_FRASES_MS > 0 && phrases.size() > 1) {
                audioService.createSilenceWav(silenceWav, Config.INTERVALO_ENTRE_FRASES_MS);
            }

            List<Path> segments = new ArrayList<>();

            for (int i = 0; i < phrases.size(); i++) {
                int phraseIndex = i + 1;
                String phrase = phrases.get(i);

                System.out.println("[" + phraseIndex + "/" + phrases.size() + "] Gerando áudio...");

                Path rawAudio = tempDir.resolve(String.format("phrase_%03d_raw.mp3", phraseIndex));
                Path normalizedWav = tempDir.resolve(String.format("phrase_%03d.wav", phraseIndex));

                try {
                    ttsService.synthesize(phrase, rawAudio, phraseIndex);
                    audioService.normalizeToWav(rawAudio, normalizedWav, phraseIndex);
                } catch (TtsException | AudioException e) {
                    throw e;
                } catch (Exception e) {
                    throw new AppException(
                            "Falha ao processar a frase [" + phraseIndex + "]: \"" + phrase + "\". "
                                    + e.getMessage(),
                            e);
                }

                segments.add(normalizedWav);

                boolean isLast = phraseIndex == phrases.size();
                if (!isLast && Config.INTERVALO_ENTRE_FRASES_MS > 0) {
                    segments.add(silenceWav);
                }
            }

            System.out.println();
            System.out.println("Finalizando arquivo MP3...");

            Path concatList = tempDir.resolve("concat.txt");
            audioService.concatenateToMp3(segments, concatList, outputMp3);

            System.out.println();
            System.out.println("Áudio gerado com sucesso!");
            System.out.println();
            System.out.println("Arquivo: " + outputMp3.toString().replace('\\', '/'));

            return outputMp3;
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private static void printHeader(Path textFile) {
        System.out.println("========================================");
        System.out.println("      GERADOR DE AUDIO TTS");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Motor TTS: Google TTS (gTTS)");
        System.out.println("Idioma: " + Config.IDIOMA);
        System.out.println("Intervalo: " + Config.INTERVALO_ENTRE_FRASES_MS + " ms");
        System.out.println();
        System.out.println("Arquivo: " + textFile.toString().replace('\\', '/'));
        System.out.println();
    }

    private static void prepareTempDir(Path tempDir) throws AppException {
        try {
            if (Files.exists(tempDir)) {
                deleteRecursively(tempDir);
            }
            Files.createDirectories(tempDir);

            Path outputDir = Path.of(Config.PASTA_OUTPUT);
            Files.createDirectories(outputDir);

            if (!Files.isWritable(outputDir)) {
                throw new AppException(
                        "Sem permissão de escrita na pasta de saída: " + outputDir.toAbsolutePath());
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(
                    "Não foi possível preparar pastas temporárias/saída: " + e.getMessage(), e);
        }
    }

    private static void cleanupTempDir(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                deleteRecursively(tempDir);
            }
        } catch (IOException e) {
            System.err.println("Aviso: não foi possível remover arquivos temporários em "
                    + tempDir + " (" + e.getMessage() + ").");
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
