# TranscribeEnglish TTS

Aplicação Java que lê frases de um arquivo `.txt` e gera **um único arquivo MP3**,
com silêncio configurável entre cada frase, usando **Google Text-to-Speech (gTTS)**.

## Como funciona

1. Lê `texto/frases.txt` (uma frase por linha)
2. Converte cada frase em áudio via **Google Translate TTS** (gratuito, sem API Key)
3. Insere silêncio entre as frases com **FFmpeg**
4. Gera um único `output/frases.mp3`

> Requer **internet** para o Google TTS.

---

## Requisitos

- **Java 17+**
- **Maven 3.8+**
- **FFmpeg** (com `libmp3lame`)
- Conexão com a internet

---

## Instalação do FFmpeg

### Windows

```bash
winget install Gyan.FFmpeg
```

Ou baixe em: https://www.gyan.dev/ffmpeg/builds/

No IntelliJ, se o PATH não for herdado, informe o caminho completo em `Config.FFMPEG_COMMAND`.

### macOS

```bash
brew install ffmpeg
```

### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install ffmpeg
```

Teste:

```bash
ffmpeg -version
```

---

## Estrutura

```text
TranscribeEnglish/
├── pom.xml
├── README.md
├── texto/
│   └── frases.txt
├── output/
└── src/main/java/com/transcribeenglish/tts/
    ├── Main.java
    ├── Config.java
    ├── TextFileReader.java
    ├── TtsService.java
    ├── GttsClient.java
    ├── AudioService.java
    ├── AudioGenerator.java
    └── ExternalCommandRunner.java
```

---

## Arquivo de texto

Edite `texto/frases.txt` (UTF-8, uma frase por linha):

```text
Hello, how are you?
I am learning English.
This is a beautiful day.
```

Linhas vazias são ignoradas.

---

## Configuração

Em `Config.java`:

```java
public static final String IDIOMA = "en-US";
public static final int INTERVALO_ENTRE_FRASES_MS = 1500;
public static final int GTTS_DELAY_MS = 250;
public static final String ARQUIVO_TEXTO = "frases.txt";
public static final String ARQUIVO_OUTPUT = "frases.mp3";
public static final String FFMPEG_COMMAND = "ffmpeg"; // ou caminho completo
```

Idiomas comuns do gTTS: `en`, `en-US`, `en-GB`, `pt-BR`.

---

## Como executar

```bash
mvn clean package
mvn exec:java
```

Ou no IntelliJ: rode a classe `Main`.

Resultado:

```text
output/frases.mp3
```

---

## Problemas comuns

### Sem internet / falha no Google TTS

- Verifique a conexão
- HTTP 429/503: aguarde e tente novamente; aumente `GTTS_DELAY_MS` se necessário

### FFmpeg não encontrado

- Instale o FFmpeg
- No IntelliJ, use o caminho completo em `Config.FFMPEG_COMMAND` e reinicie a IDE

### Arquivo de texto não encontrado

- Confirme `texto/frases.txt`

---

## Observação sobre o gTTS

A biblioteca Python gTTS e esta implementação Java usam o endpoint gratuito do
Google Translate TTS, **sem API Key**. É ótimo para uso pessoal/estudo, mas não é
um serviço oficial de API e pode mudar ou limitar requisições.
