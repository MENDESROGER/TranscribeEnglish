# TranscribeEnglish TTS

Aplicação Java que lê frases em inglês de um arquivo `.txt` e gera **um único arquivo MP3**, com silêncio configurável entre cada frase, usando Text-to-Speech **local e gratuito**.

## Motor TTS padrão: gTTS (Google Text-to-Speech)

O padrão atual é **gTTS**, usando o mesmo endpoint gratuito do Google Translate TTS
(sem API Key). A voz é bem mais natural que eSpeak.

| Motor | Qualidade | Requisitos |
|-------|-----------|------------|
| **gtts** | Natural (Google) | Internet |
| **windows** | Natural (SAPI) | Windows |
| **espeak** | Robotizada | eSpeak NG local |

Configure em `Config.java`:

```java
public static final String TTS_ENGINE = "gtts"; // "windows" ou "espeak"
public static final String IDIOMA = "en-US";
public static final int GTTS_DELAY_MS = 250;
```

> Observação: o endpoint do Google Translate TTS é gratuito e sem chave, mas é um
> serviço não oficial/estável para automação. Em caso de bloqueio temporário (HTTP 429/503),
> aguarde e tente novamente, ou use `TTS_ENGINE = "windows"`.

---

## Requisitos

- **Java 17 ou superior** (o projeto foi validado com Java 21 via Maven)
- **Maven 3.8+**
- **eSpeak NG** (ou `espeak` como fallback)
- **FFmpeg** (com encoder MP3 `libmp3lame`)

Sistema operacional: **Windows**, **macOS** ou **Linux**.

---

## Dependências do projeto

O `pom.xml` não depende de bibliotecas TTS pagas. A síntese e a montagem do áudio usam programas externos:

- `espeak-ng` / `espeak`
- `ffmpeg`

Plugins Maven usados:

- `maven-compiler-plugin` (Java 17)
- `maven-jar-plugin` (JAR executável)
- `exec-maven-plugin` (execução via Maven)

---

## Instalação do eSpeak NG

### Windows

1. Baixe o instalador em: [https://github.com/espeak-ng/espeak-ng/releases](https://github.com/espeak-ng/espeak-ng/releases)
2. Instale o pacote (ex.: `espeak-ng.msi`) — pode exigir permissão de administrador
3. Garanta que `espeak-ng.exe` esteja no **PATH**
4. Abra um **novo** terminal e teste:

```bash
espeak-ng --version
espeak-ng -v en-us -w teste.wav "Hello world"
```

Alternativa via Winget:

```bash
winget install --id eSpeak-NG.eSpeak-NG -e
```

Se preferir não instalar no sistema, extraia o MSI em uma pasta e:

- adicione a pasta do `espeak-ng.exe` ao PATH; ou
- informe o caminho completo em `Config.ESPEAK_COMMAND`

A aplicação detecta automaticamente a pasta `espeak-ng-data` ao lado do executável e configura `ESPEAK_DATA_PATH` (necessário no Windows quando o eSpeak não está instalado no caminho padrão).

### macOS

```bash
brew install espeak-ng
espeak-ng --version
```

### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install espeak-ng
espeak-ng --version
```

### Linux (Fedora)

```bash
sudo dnf install espeak-ng
```

---

## Instalação do FFmpeg

### Windows

1. Baixe em: [https://www.gyan.dev/ffmpeg/builds/](https://www.gyan.dev/ffmpeg/builds/) (build “full” ou “essentials”)
2. Extraia e adicione a pasta `bin` ao **PATH**
3. Teste:

```bash
ffmpeg -version
```

Alternativas:

```bash
choco install ffmpeg
winget install Gyan.FFmpeg
```

### macOS

```bash
brew install ffmpeg
ffmpeg -version
```

### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install ffmpeg
ffmpeg -version
```

> Confirme que o FFmpeg inclui `libmp3lame` (a maioria dos builds padrão inclui).

---

## Estrutura do projeto

```text
TranscribeEnglish/
├── pom.xml
├── README.md
├── texto/
│   └── frases.txt
├── output/
│   └── .gitkeep
└── src/
    └── main/
        └── java/
            └── com/transcribeenglish/tts/
                ├── Main.java
                ├── Config.java
                ├── TextFileReader.java
                ├── TtsService.java
                ├── AudioService.java
                ├── AudioGenerator.java
                ├── ExternalCommandRunner.java
                └── exception/
                    ├── AppException.java
                    ├── TextFileException.java
                    ├── TtsException.java
                    └── AudioException.java
```

---

## Como colocar o arquivo de texto

1. Crie/edite: `texto/frases.txt`
2. Uma frase por linha
3. Encoding: **UTF-8**
4. Linhas vazias são ignoradas
5. Espaços no início/fim de cada linha são removidos

Exemplo:

```text
Hello, how are you?
I am learning English.
This is a beautiful day.
I would like to improve my English.
```

---

## Como alterar idioma e intervalo

Edite a classe `Config`:

```java
public static final String IDIOMA = "en-US";
public static final int INTERVALO_ENTRE_FRASES_MS = 1500;
public static final String PASTA_TEXTO = "texto";
public static final String ARQUIVO_TEXTO = "frases.txt";
public static final String PASTA_OUTPUT = "output";
public static final String ARQUIVO_OUTPUT = "frases.mp3";
```

### Idioma

O valor de `IDIOMA` é convertido para a voz do eSpeak NG (`en-US` → `en-us`).

Exemplos comuns:

- `en-US` → inglês americano
- `en-GB` → inglês britânico
- `pt-BR` → português do Brasil (se a voz estiver instalada)

Liste vozes disponíveis:

```bash
espeak-ng --voices
espeak-ng --voices=en
```

### Intervalo

`INTERVALO_ENTRE_FRASES_MS` define o **silêncio real** entre frases no MP3 (não é apenas um `sleep` no Java).

Exemplo: `1500` = 1,5 segundos de silêncio entre frase 1 e 2, entre 2 e 3, etc.

---

## Como executar

No diretório raiz do projeto:

### 1. Compilar

```bash
mvn clean package
```

### 2. Executar

Opção A (Maven):

```bash
mvn exec:java
```

Opção B (JAR):

```bash
java -jar target/transcribe-english-tts-1.0.0.jar
```

### 3. Resultado

O MP3 será gerado em:

```text
output/frases.mp3
```

Conteúdo:

```text
[Frase 1] → silêncio → [Frase 2] → silêncio → [Frase 3] → ...
```

---

## Exemplo de saída no console

```text
========================================
      GERADOR DE AUDIO TTS
========================================

Idioma: en-US
Intervalo: 1500 ms

Arquivo: texto/frases.txt

Frases encontradas: 4

[1/4] Gerando áudio...
[2/4] Gerando áudio...
[3/4] Gerando áudio...
[4/4] Gerando áudio...

Finalizando arquivo MP3...

Áudio gerado com sucesso!

Arquivo: output/frases.mp3
```

---

## Arquitetura

| Classe | Responsabilidade |
|--------|------------------|
| `Main` | Bootstrap e tratamento de erros de alto nível |
| `Config` | Constantes editáveis |
| `TextFileReader` | Leitura UTF-8, trim e filtro de linhas vazias |
| `TtsService` | Chamada ao eSpeak NG e geração de WAV por frase |
| `AudioService` | Silêncio, normalização, concatenação e exportação MP3 via FFmpeg |
| `AudioGenerator` | Orquestração do fluxo completo e limpeza de temporários |
| `ExternalCommandRunner` | Execução segura de processos externos |

Arquivos temporários ficam em `output/.tmp-tts` e são **removidos ao final** da execução.

---

## Problemas comuns e soluções

### `eSpeak NG não encontrado no PATH`

- Instale o eSpeak NG
- Feche e reabra o terminal após alterar o PATH
- Teste `espeak-ng --version`
- Se necessário, coloque o caminho completo em `Config.ESPEAK_COMMAND`

### Crash / código estranho no Windows ao gerar WAV

- Confirme que a pasta `espeak-ng-data` existe ao lado de `espeak-ng.exe`
- A aplicação tenta configurar `ESPEAK_DATA_PATH` automaticamente
- Instale o [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist) se o instalador do eSpeak exigir
- Prefira a instalação oficial via MSI em vez de copiar só o `.exe`

### `FFmpeg não instalado`

- Instale o FFmpeg e confirme `ffmpeg -version`
- Em builds mínimos sem MP3, use uma distribuição completa com `libmp3lame`

### `Arquivo de texto não encontrado`

- Confirme o caminho `texto/frases.txt`
- Ou altere `PASTA_TEXTO` / `ARQUIVO_TEXTO` em `Config`

### `Nenhuma frase válida encontrada`

- O arquivo só tem linhas em branco
- Adicione pelo menos uma frase com conteúdo

### Erro de voz/idioma

- A voz pode não estar instalada
- Liste com `espeak-ng --voices`
- Ajuste `Config.IDIOMA` (ex.: `en` ou `en-us`)

### `Sem permissão de escrita`

- Verifique permissões da pasta `output/`
- Feche o MP3 se estiver aberto em um player

### Java 8 no `java -version`, mas Maven usa 21

Isso é comum no Windows. O Maven pode estar apontando para outro JDK.

Use:

```bash
mvn -version
mvn clean package
mvn exec:java
```

Ou execute o JAR com um JDK 17+:

```bash
"C:\caminho\para\jdk-21\bin\java.exe" -jar target/transcribe-english-tts-1.0.0.jar
```

### Particularidades por SO

- **Windows:** caminhos com espaços e PATH costumam ser a causa mais comum de falha; reinicie o terminal após instalar ferramentas
- **macOS:** Homebrew é o caminho mais simples (`espeak-ng` e `ffmpeg`)
- **Linux:** pacotes da distribuição normalmente bastam

---

## Fluxo interno (resumo)

1. Lê `texto/frases.txt`
2. Para cada frase, gera WAV com eSpeak NG
3. Normaliza o WAV (mono, 22050 Hz) com FFmpeg
4. Insere WAV de silêncio entre as frases
5. Concatena tudo e exporta `output/frases.mp3`
6. Remove arquivos temporários

---

## Licença das ferramentas externas

- eSpeak NG: GPL
- FFmpeg: LGPL/GPL conforme o build

Use de acordo com as licenças dos respectivos projetos.
