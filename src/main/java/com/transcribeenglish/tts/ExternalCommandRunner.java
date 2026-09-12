package com.transcribeenglish.tts;

import com.transcribeenglish.tts.exception.AppException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Executa comandos externos (eSpeak NG, FFmpeg) via ProcessBuilder.
 */
public class ExternalCommandRunner {

    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    public void ensureCommandAvailable(String command, String friendlyName) throws AppException {
        try {
            ProcessBuilder builder = new ProcessBuilder(command, "--version");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            drainQuietly(process);
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AppException(friendlyName + " não respondeu a tempo. Verifique a instalação.");
            }
            if (process.exitValue() != 0 && process.exitValue() != 1) {
                if (!tryHelp(command)) {
                    throw new AppException(
                            friendlyName + " parece instalado, mas retornou erro ao ser executado ("
                                    + "comando: " + command + ").");
                }
            }
        } catch (IOException e) {
            throw new AppException(
                    friendlyName + " não encontrado no PATH (comando: \"" + command + "\").\n"
                            + "Instale a ferramenta e garanta que o executável esteja acessível no terminal. "
                            + "Detalhes: " + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Verificação de " + friendlyName + " foi interrompida.", e);
        }
    }

    public void run(List<String> command, String errorContext) throws AppException {
        run(command, errorContext, null, null, DEFAULT_TIMEOUT_SECONDS);
    }

    public void run(
            List<String> command,
            String errorContext,
            Path workingDirectory,
            Map<String, String> extraEnvironment
    ) throws AppException {
        run(command, errorContext, workingDirectory, extraEnvironment, DEFAULT_TIMEOUT_SECONDS);
    }

    public void run(
            List<String> command,
            String errorContext,
            Path workingDirectory,
            Map<String, String> extraEnvironment,
            long timeoutSeconds
    ) throws AppException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        if (extraEnvironment != null && !extraEnvironment.isEmpty()) {
            builder.environment().putAll(extraEnvironment);
        }

        try {
            Process process = builder.start();
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AppException(errorContext + " excedeu o tempo limite de " + timeoutSeconds + "s.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String details = output.isBlank() ? "(sem saída do processo)" : output.trim();
                throw new AppException(errorContext + " (código " + exitCode + ").\nDetalhes: " + details);
            }
        } catch (IOException e) {
            throw new AppException(errorContext + ". Não foi possível iniciar o processo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(errorContext + " foi interrompido.", e);
        }
    }

    /**
     * Resolve o caminho absoluto de um executável presente no PATH (ou caminho já absoluto).
     */
    public Path resolveExecutablePath(String command) throws AppException {
        Path asPath = Path.of(command);
        if (asPath.isAbsolute() && asPath.toFile().exists()) {
            return asPath.toAbsolutePath().normalize();
        }

        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> lookup = windows
                ? List.of("where.exe", command)
                : List.of("which", command);

        ProcessBuilder builder = new ProcessBuilder(lookup);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AppException("Tempo esgotado ao localizar o executável: " + command);
            }
            if (process.exitValue() != 0 || output.isBlank()) {
                throw new AppException("Não foi possível localizar o executável no PATH: " + command);
            }
            String firstLine = output.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new AppException("Executável não encontrado: " + command));
            return Path.of(firstLine).toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new AppException("Falha ao resolver caminho de \"" + command + "\": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Busca do executável \"" + command + "\" foi interrompida.", e);
        }
    }

    private boolean tryHelp(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command, "-h");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            drainQuietly(process);
            return process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static void drainQuietly(Process process) {
        try {
            readProcessOutput(process);
        } catch (IOException ignored) {
            // Ignorado: apenas evita bloqueio do buffer.
        }
    }

    public static List<String> command(String executable, String... args) {
        List<String> full = new ArrayList<>();
        full.add(executable);
        for (String arg : args) {
            full.add(arg);
        }
        return full;
    }

    public static Map<String, String> env(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
