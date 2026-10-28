# Traduya

Traductor de llamadas en tiempo real con IA para Android.
Traduya reemplaza la app de teléfono: llamas o contestas como siempre y, mientras hablas,
ves en pantalla la traducción al español de lo que dice la otra persona (inglés o chino)
y la traducción de lo que dices tú.

## Funciones

- **Llamada traducida por internet (recomendado)**: compartes tu enlace o código QR, la otra persona lo abre en su
  navegador (iPhone, Android o computadora, sin instalar nada) y te llama. Su voz llega a Traduya por WebRTC y se
  traduce al español en vivo. Funciona porque Android no puede bloquear este audio, como sí bloquea el de las llamadas
  telefónicas.

- **App de teléfono predeterminada**: hace y recibe llamadas con su propia pantalla (`InCallService`).
- **Traducción en vivo estilo karaoke**: solo en español, en letra grande, palabra a palabra mientras la otra persona habla.
- **Reconocimiento de voz sin internet** con [Vosk](https://alphacephei.com/vosk/) y **traducción neuronal** con Google ML Kit, ambos dentro del teléfono.
- **Detección automática de idioma**: un reconocedor por idioma escucha a la vez y gana el de mayor confianza. Tras tres frases seguidas se fija el idioma para ir más rápido.
- **Tu voz no se muestra**: un modelo de español reconoce lo que dices tú y lo descarta.
- **Grabación de la llamada**: el mismo audio que se traduce se guarda en WAV y se reproduce desde el historial.
- **Historial**: cada llamada con su grabación, duración, idioma y transcripción.
- Interfaz 100 % en español, tema claro y oscuro.

## Arquitectura

Clean Architecture con módulos Gradle independientes:

```
app/                    Composición: Hilt, navegación y CallSessionCoordinator
core/model              Modelos puros de Kotlin
core/domain             Contratos (repositorios, motor de traducción) y casos de uso
core/data               Room (historial) y DataStore (ajustes)
core/audio              Captura del micrófono y grabación WAV
core/translation        Vosk (voz a texto) + ML Kit (traducción) y paquetes de idioma
core/telecom            InCallService, CallManager, notificaciones y rol de marcador
core/session            Motor de traducción en vivo de una llamada (audio → IA → historial)
core/callui             Pantalla de llamada compartida (karaoke, controles)
core/designsystem       Tema azul claro, logo y componentes compartidos
feature/onboarding      Bienvenida, permisos y app predeterminada
feature/dialer          Teclado para marcar
feature/incall          Llamadas telefónicas
feature/internetcall    Llamadas por internet (WebRTC + PeerJS en un WebView) con enlace y QR
docs/                   Página web para llamar (GitHub Pages): https://camilo661.github.io/Traduya/
feature/history         Historial y detalle de cada llamada
feature/settings        Idioma, altavoz automático y modelos
```

Flujo de una llamada:

```
Android Telecom → TraduyaInCallService → CallManager (StateFlow)
      → CallSessionCoordinator → AudioCapture (micrófono, 100 ms)
            ├─ CallRecordingStore (WAV)
            └─ VoskSpeechTranscriber → LanguageArbiter → ML Kit → karaoke en pantalla
      → HistoryRepository (Room)
```

**Tecnologías:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Coroutines/Flow, Vosk y ML Kit.

## Compilar

```bash
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew :core:domain:test      # pruebas unitarias
./gradlew :core:translation:connectedDebugAndroidTest   # voz real → traducción (emulador o teléfono)
```

Requiere JDK 17 o superior y el Android SDK 35. Funciona en Android 10 o superior.

## Limitación importante de Android

Desde Android 10, el sistema **silencia el micrófono de cualquier app durante una llamada telefónica normal**,
aunque la app sea la de teléfono predeterminada y aunque tenga activado un servicio de accesibilidad.
Comprobado en Android 14 con `dumpsys audio`: la captura de Traduya aparece como `silenced:true`
en las fuentes MIC, VOICE_RECOGNITION, CAMCORDER y UNPROCESSED.

Solo las apps del sistema con el permiso `CAPTURE_AUDIO_OUTPUT` pueden oír una llamada. Por eso los
traductores de llamadas que funcionan son funciones del propio fabricante (Galaxy AI en Samsung,
Traducción en tiempo real en Google Pixel).

Traduya detecta este bloqueo, lo avisa en pantalla y no guarda grabaciones vacías. El reconocimiento y la
traducción funcionan (ver la prueba `LiveTranslationPipelineTest`) cuando el audio llega a la app.
