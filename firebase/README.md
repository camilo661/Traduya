# Firebase: directorio de números, inicio de sesión y página web

Las llamadas por WiFi usan un proyecto gratuito de Firebase (plan Spark), `traduya-66592`:

- **Authentication**
  - *Anónima*: cada teléfono con Traduya recibe un usuario anónimo que lo identifica como dueño de su número.
  - *Google*: quien llama desde la página web se **registra** (cuenta de Google + nombre completo + teléfono con
    país + código de 6 dígitos que llega a su Gmail) y después **inicia sesión**.
- **Functions** ([`functions/`](../functions)):
  - `sendRegistrationCode`: guarda nombre y teléfono y envía el código al Gmail (vence en 10 minutos,
    1 por minuto, 5 al día).
  - `confirmRegistration`: comprueba el código (5 intentos), crea la ficha en `callers/{uid}` y marca la cuenta
    con `registered: true`.
- **Firestore**:
  - `numbers/{+E164}` con `{ owner, peerId }`: el directorio de números.
  - `callers/{uid}` con `{ fullName, phone, email }`: la ficha de quien llama (solo la escriben las funciones).
  - `registrations/{uid}`: los códigos pendientes (solo las funciones; se guarda la huella, no el código).
- **Reglas** ([firestore.rules](firestore.rules)): solo quien se registró puede buscar un número; nadie puede
  listarlos; solo el dueño puede cambiarlo o borrarlo; nadie puede escribir fichas ni ver códigos.
- **Hosting**: la página para llamar ([`web/`](../web)) en <https://traduya-66592.web.app>. Tras iniciar sesión
  muestra un teclado (país + número) para llamar por internet a los números registrados en la app.

La app de quien recibe comprueba el token de quien llama (`accounts:lookup`), exige la marca `registered`, lee
su ficha y muestra su nombre y teléfono. Rechaza sin sonar las llamadas de cuentas sin registro.

La app habla con Firebase por su API REST, sin SDKs. La página web usa el SDK web de Firebase.

## Crear el proyecto (una vez, unos 5 minutos)

1. Entra en <https://console.firebase.google.com> y pulsa **Crear un proyecto**. Nombre: `traduya`.
   Google Analytics no hace falta.
2. **Compilación › Authentication › Comenzar › Método de acceso** y habilita:
   - **Anónimo**.
   - **Google** (elige tu correo como correo de asistencia).
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

## Correo que envía los códigos

Las funciones necesitan el **plan Blaze** (pago por uso con tarjeta; con este uso el costo esperado es 0 USD)
y una cuenta de Gmail con **verificación en dos pasos** y una **contraseña de aplicación**
(<https://myaccount.google.com/apppasswords>). Se guardan en Secret Manager, nunca en el código:

```bash
firebase functions:secrets:set GMAIL_USER           # el Gmail que envía, por ejemplo traduya.codigos@gmail.com
firebase functions:secrets:set GMAIL_APP_PASSWORD   # la contraseña de aplicación de 16 letras
```

En el emulador se usan los valores de `functions/.secret.local` (no se sube a git) y el código no se envía:
la función lo devuelve como `debugCode` para las pruebas.

## Publicar

Desde la raíz del repositorio ([firebase.json](../firebase.json) y [.firebaserc](../.firebaserc)):

```bash
firebase login
(cd functions && npm install)
firebase deploy --only functions,firestore:rules,hosting
```

La dirección antigua (`camilo661.github.io/Traduya`, carpeta `docs/`) redirige a la nueva, así que los enlaces
y códigos QR que ya se compartieron siguen funcionando.

## Probar en local

Desde la raíz del repositorio:

```bash
firebase emulators:start --only auth,firestore,functions,hosting --project demo-traduya
node --test firebase/test/                                                 # reglas y registro
./gradlew :app:assembleDebug -Ptraduya.firebase.emulatorHost=10.0.2.2     # app contra el emulador
```

La página queda en <http://127.0.0.1:5180/?emulator=1>; con `?emulator=1` usa los emuladores de Firebase
(solo funciona en `localhost`).

## Siguiente paso: verificación por SMS

Hoy el número de quien recibe no se verifica. Para verificarlo, se cambia su usuario anónimo por
**Firebase Phone Auth** (SMS con código) y la regla de creación pasa a exigir
`request.auth.token.phone_number == number`.
