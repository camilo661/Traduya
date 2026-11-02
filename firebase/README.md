# Firebase: directorio de números, inicio de sesión y página web

Las llamadas por WiFi usan un proyecto gratuito de Firebase (plan Spark), `traduya-66592`:

Todo funciona en el **plan gratuito Spark**, sin tarjeta y sin servidor: las reglas de Firestore hacen cumplir el registro.

- **Authentication**
  - *Anónima*: cada teléfono con Traduya recibe un usuario anónimo que lo identifica como dueño de su número.
  - *Google*: quien llama desde la página web se **registra** (cuenta de Google + nombre completo + teléfono con
    país) e **inicia sesión**.
  - *Enlace por correo*: Firebase envía gratis a su Gmail un enlace «Confirmar registro». Al abrirlo, la sesión
    pasa a ser de tipo `password` y solo con ella las reglas dejan crear su ficha.
- **Firestore**:
  - `numbers/{+E164}` con `{ owner, peerId }`: el directorio de números.
  - `pendingCallers/{uid}` con `{ fullName, phone }`: el borrador del registro mientras llega el enlace.
  - `callers/{uid}` con `{ fullName, phone, email, registeredAt }`: la ficha de quien llama.
- **Reglas** ([firestore.rules](firestore.rules)): solo quien tiene ficha puede buscar un número; nadie puede
  listarlos; solo el dueño puede cambiarlo o borrarlo; la ficha solo se crea con la sesión del enlace del correo,
  con el mismo correo de la cuenta, y no se puede cambiar después.
- **Hosting**: la página para llamar ([`web/`](../web)) en <https://traduya-66592.web.app>. Tras iniciar sesión
  muestra un teclado (país + número) para llamar por internet a los números registrados en la app.

La app de quien recibe comprueba el token de quien llama (`accounts:lookup`), lee su ficha y muestra su nombre y
teléfono. Si no tiene ficha (no confirmó el enlace), rechaza la llamada sin sonar.

La app habla con Firebase por su API REST, sin SDKs. La página web usa el SDK web de Firebase.

## Crear el proyecto (una vez, unos 5 minutos)

1. Entra en <https://console.firebase.google.com> y pulsa **Crear un proyecto**. Nombre: `traduya`.
   Google Analytics no hace falta.
2. **Compilación › Authentication › Comenzar › Método de acceso** y habilita:
   - **Anónimo**.
   - **Google** (elige tu correo como correo de asistencia).
   - **Correo electrónico/contraseña**, con **Vínculo de correo electrónico (acceso sin contraseña)**.
3. **Compilación › Firestore Database › Crear base de datos**, en **modo de producción**.
   Elige la ubicación más cercana (por ejemplo `southamerica-east1` o `nam5`).
4. En Firestore, pestaña **Reglas**: borra lo que hay, pega el contenido de `firestore.rules` y pulsa **Publicar**.
5. **Configuración del proyecto (engranaje) › General › Tus apps**, pulsa el icono web `</>` y registra
   la app como `Traduya web` (sin Hosting). Copia `apiKey` y `projectId`.

## Conectar Traduya

Pon los dos valores en:

- `gradle.properties`: `traduya.firebase.projectId` y `traduya.firebase.apiKey`.
- `web/js/config.js`: `FIREBASE_CONFIG`.

La clave web de Firebase es pública por diseño; la seguridad está en las reglas.

## Publicar

Desde la raíz del repositorio ([firebase.json](../firebase.json) y [.firebaserc](../.firebaserc)):

```bash
firebase login
firebase deploy --only firestore:rules,hosting
```

La dirección antigua (`camilo661.github.io/Traduya`, carpeta `docs/`) redirige a la nueva, así que los enlaces
y códigos QR que ya se compartieron siguen funcionando.

## Probar en local

Desde la raíz del repositorio:

```bash
firebase emulators:start --only auth,firestore,hosting --project demo-traduya
node --test firebase/test/                                                 # reglas y registro
./gradlew :app:assembleDebug -Ptraduya.firebase.emulatorHost=10.0.2.2     # app contra el emulador
```

La página queda en <http://127.0.0.1:5180/?emulator=1>; con `?emulator=1` usa los emuladores de Firebase
(solo funciona en `localhost`).

## Siguiente paso: verificación por SMS

Hoy el número de quien recibe no se verifica. Para verificarlo, se cambia su usuario anónimo por
**Firebase Phone Auth** (SMS con código) y la regla de creación pasa a exigir
`request.auth.token.phone_number == number`.
