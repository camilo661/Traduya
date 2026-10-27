# Traduya

Traductor de llamadas en tiempo real con IA para Android.
Traduya reemplaza la app de teléfono: llamas o contestas como siempre y, mientras hablas,
ves en pantalla la traducción al español de lo que dice la otra persona (inglés o chino)
y la traducción de lo que dices tú.

## Funciones

- **App de teléfono predeterminada**: hace y recibe llamadas con su propia pantalla (`InCallService`).
- **Traducción en vivo**: voz a texto continuo y traducción neuronal en el propio teléfono (Google ML Kit).
- **Detección automática de idioma**: inglés, chino y español (Android 14+ en el reconocedor; ML Kit sobre el texto en cualquier versión).
- **Historial**: cada llamada se guarda con su transcripción completa.
- **Llamadas largas**: el reconocimiento se reinicia solo, frase tras frase, durante toda la llamada.
- Interfaz 100 % en español, tema claro y oscuro.

## Arquitectura

Clean Architecture con módulos Gradle independientes:

```
app/                    Composición: Hilt, navegación y CallSessionCoordinator
core/model              Modelos puros de Kotlin
core/domain             Contratos (repositorios, motor de traducción) y casos de uso
core/data               Room (historial) y DataStore (ajustes)
core/translation        SpeechRecognizer + ML Kit (traducción e identificación de idioma)
core/telecom            InCallService, CallManager, notificaciones y rol de marcador
core/designsystem       Tema azul claro, logo y componentes compartidos
feature/onboarding      Bienvenida, permisos y app predeterminada
feature/dialer          Teclado para marcar
feature/incall          Pantalla de llamada con traducción en vivo
feature/history         Historial y detalle de cada llamada
feature/settings        Idioma, altavoz automático y modelos
```

Flujo de una llamada:

```
Android Telecom → TraduyaInCallService → CallManager (StateFlow)
      → CallSessionCoordinator → SpeechTranscriber → TranslateUtteranceUseCase
      → HistoryRepository (Room) → InCallScreen (Compose)
```

**Tecnologías:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Coroutines/Flow y ML Kit.

## Compilar

```bash
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew :core:domain:test      # pruebas unitarias
```

Requiere JDK 17 o superior y el Android SDK 35. Funciona en Android 10 o superior.

## Limitación importante de Android

Android no permite a apps de terceros grabar directamente el audio de la otra persona en la llamada.
Por eso Traduya activa el **altavoz** al conectar: así el micrófono capta las dos voces.
En algunos fabricantes el sistema silencia el micrófono para otras apps durante una llamada;
en esos teléfonos la traducción puede no recibir audio.
