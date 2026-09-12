package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.TtsException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cliente compatível com gTTS: usa o endpoint gratuito do Google Translate TTS.
 * Requer conexão com a internet. Não usa API Key.
 */
public class GttsClient {

    private static final String TTS_URL = "https://translate.google.com/translate_tts";
    private static final int MAX_CHARS = 100;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private final HttpClient httpClient;

    public GttsClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void ensureAvailable() throws TtsException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://translate.google.com/"))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                throw new TtsException(
                        "Não foi possível acessar o Google Translate TTS (HTTP "
                                + response.statusCode() + ").\n"
                                + "Verifique sua conexão com a internet.");
            }
        } catch (TtsException e) {
            throw e;
        } catch (IOException e) {
            throw new TtsException(
                    "gTTS requer internet e não foi possível conectar ao Google TTS.\n"
                            + "Detalhe: " + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TtsException("Verificação de conectividade do gTTS foi interrompida.", e);
        }
    }

    public void synthesizeToMp3(String phrase, Path outputMp3, int phraseIndex) throws TtsException {
        String lang = toGttsLanguage(Config.IDIOMA);
        List<String> chunks = splitIntoChunks(phrase, MAX_CHARS);

        try {
            Path parent = outputMp3.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            ByteArrayOutputStream merged = new ByteArrayOutputStream();
            for (int i = 0; i < chunks.size(); i++) {
                byte[] audio = downloadChunk(chunks.get(i), lang, phraseIndex, i + 1, chunks.size());
                merged.write(audio);
                if (i < chunks.size() - 1 && Config.GTTS_DELAY_MS > 0) {
                    Thread.sleep(Config.GTTS_DELAY_MS);
                }
            }

            Files.write(outputMp3, merged.toByteArray());

            if (!Files.exists(outputMp3) || Files.size(outputMp3) == 0) {
                throw new TtsException(
                        "gTTS não gerou áudio válido para a frase [" + phraseIndex + "]: \""
                                + truncate(phrase) + "\".");
            }
        } catch (TtsException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TtsException("gTTS interrompido na frase [" + phraseIndex + "].", e);
        } catch (Exception e) {
            throw new TtsException(
                    "Falha no gTTS na frase [" + phraseIndex + "]: \"" + truncate(phrase) + "\".\n"
                            + e.getMessage(),
                    e);
        }
    }

    private byte[] downloadChunk(
            String text,
            String lang,
            int phraseIndex,
            int chunkIndex,
            int totalChunks
    ) throws TtsException {
        try {
            String query = "ie=UTF-8"
                    + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&tl=" + URLEncoder.encode(lang, StandardCharsets.UTF_8)
                    + "&client=tw-ob";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_URL + "?" + query))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://translate.google.com/")
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new TtsException(
                        "gTTS retornou HTTP " + response.statusCode()
                                + " na frase [" + phraseIndex + "] (parte " + chunkIndex + "/"
                                + totalChunks + "): \"" + truncate(text) + "\".\n"
                                + "O serviço gratuito do Google pode estar temporariamente indisponível "
                                + "ou bloqueando muitas requisições. Tente novamente em alguns minutos.");
            }

            try (InputStream body = response.body()) {
                byte[] bytes = body.readAllBytes();
                if (bytes.length == 0) {
                    throw new TtsException(
                            "gTTS retornou áudio vazio na frase [" + phraseIndex + "] (parte "
                                    + chunkIndex + "/" + totalChunks + ").");
                }
                return bytes;
            }
        } catch (TtsException e) {
            throw e;
        } catch (IOException e) {
            throw new TtsException(
                    "Erro de rede no gTTS na frase [" + phraseIndex + "]: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TtsException("Download gTTS interrompido na frase [" + phraseIndex + "].", e);
        }
    }

    /**
     * Converte locale (en-US) para código gTTS (en / en-us / pt-br).
     */
    static String toGttsLanguage(String idioma) {
        if (idioma == null || idioma.isBlank()) {
            return "en";
        }
        String normalized = idioma.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        // gTTS aceita "en", "en-us", "pt-br", etc.
        return normalized;
    }

    static List<String> splitIntoChunks(String text, int maxChars) {
        String cleaned = text.trim().replaceAll("\\s+", " ");
        List<String> chunks = new ArrayList<>();
        if (cleaned.length() <= maxChars) {
            chunks.add(cleaned);
            return chunks;
        }

        StringBuilder current = new StringBuilder();
        for (String word : cleaned.split(" ")) {
            if (word.length() > maxChars) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }
                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(start + maxChars, word.length());
                    chunks.add(word.substring(start, end));
                    start = end;
                }
                continue;
            }

            int prospective = current.length() == 0 ? word.length() : current.length() + 1 + word.length();
            if (prospective <= maxChars) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
            } else {
                chunks.add(current.toString().trim());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private static String truncate(String text) {
        if (text.length() <= 80) {
            return text;
        }
        return text.substring(0, 77) + "...";
    }
}
