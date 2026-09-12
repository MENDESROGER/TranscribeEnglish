# Sintetiza texto em WAV usando System.Speech (SAPI) do Windows.
param(
    [Parameter(Mandatory = $true)][string]$TextFile,
    [Parameter(Mandatory = $true)][string]$OutputWav,
    [Parameter(Mandatory = $true)][string]$Culture,
    [string]$VoiceName = "",
    [int]$Rate = 0
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Speech

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $synth.Rate = [Math]::Max(-10, [Math]::Min(10, $Rate))

    $selected = $false
    if (-not [string]::IsNullOrWhiteSpace($VoiceName)) {
        foreach ($voice in $synth.GetInstalledVoices()) {
            if ($voice.VoiceInfo.Name -eq $VoiceName) {
                $synth.SelectVoice($VoiceName)
                $selected = $true
                break
            }
        }
        if (-not $selected) {
            Write-Error "Voz Windows nao encontrada: $VoiceName"
            exit 2
        }
    }

    if (-not $selected) {
        $cultureInfo = [System.Globalization.CultureInfo]::GetCultureInfo($Culture)
        $synth.SelectVoiceByHints(
            [System.Speech.Synthesis.VoiceGender]::NotSet,
            [System.Speech.Synthesis.VoiceAge]::NotSet,
            0,
            $cultureInfo
        )
    }

    $synth.SetOutputToWaveFile($OutputWav)
    $text = [System.IO.File]::ReadAllText($TextFile, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($text)) {
        Write-Error "Arquivo de texto vazio: $TextFile"
        exit 3
    }
    $synth.Speak($text)
}
finally {
    $synth.Dispose()
}

if (-not (Test-Path -LiteralPath $OutputWav)) {
    Write-Error "WAV nao foi gerado: $OutputWav"
    exit 4
}
