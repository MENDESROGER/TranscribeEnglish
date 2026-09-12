package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.TextFileException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lê frases de um arquivo .txt (uma frase por linha).
 */
public class TextFileReader {

    public List<String> readPhrases(Path textFile) throws TextFileException {
        Path parent = textFile.getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            throw new TextFileException(
                    "Pasta de texto inexistente: " + parent.toAbsolutePath() + "\n"
                            + "Crie a pasta \"" + Config.PASTA_TEXTO + "\" e coloque o arquivo \""
                            + Config.ARQUIVO_TEXTO + "\" dentro dela.");
        }

        if (!Files.exists(textFile)) {
            throw new TextFileException(
                    "Arquivo de texto não encontrado: " + textFile.toAbsolutePath() + "\n"
                            + "Coloque suas frases (uma por linha) em "
                            + Config.PASTA_TEXTO + "/" + Config.ARQUIVO_TEXTO + ".");
        }

        if (!Files.isRegularFile(textFile)) {
            throw new TextFileException("O caminho não é um arquivo válido: " + textFile.toAbsolutePath());
        }

        if (!Files.isReadable(textFile)) {
            throw new TextFileException(
                    "Sem permissão de leitura para o arquivo: " + textFile.toAbsolutePath());
        }

        try {
            if (Files.size(textFile) == 0) {
                throw new TextFileException(
                        "O arquivo de texto está vazio: " + textFile.toAbsolutePath() + "\n"
                                + "Adicione pelo menos uma frase (uma por linha).");
            }
        } catch (IOException e) {
            throw new TextFileException("Não foi possível verificar o tamanho do arquivo: " + e.getMessage(), e);
        }

        List<String> phrases = new ArrayList<>();
        try (Stream<String> lines = Files.lines(textFile, StandardCharsets.UTF_8)) {
            lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .forEach(phrases::add);
        } catch (IOException e) {
            throw new TextFileException(
                    "Erro ao ler o arquivo de texto (verifique encoding UTF-8): " + e.getMessage(), e);
        }

        if (phrases.isEmpty()) {
            throw new TextFileException(
                    "Nenhuma frase válida encontrada em " + textFile.toAbsolutePath() + ".\n"
                            + "Linhas em branco são ignoradas; adicione pelo menos uma frase com conteúdo.");
        }

        return phrases;
    }
}
