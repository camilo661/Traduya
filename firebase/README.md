# Firebase: directorio de números, inicio de sesión y página web

Las llamadas por WiFi usan un proyecto gratuito de Firebase (plan Spark), `traduya-66592`:

- **Authentication**
  - *Anónima*: cada teléfono con Traduya recibe un usuario anónimo que lo identifica como dueño de su número.
  - *Google* y *enlace por correo*: quien llama desde la página web debe iniciar sesión. La app de quien recibe
    comprueba ese inicio de sesión (`accounts:lookup`) y rechaza las llamadas sin una sesión válida.
- **Firestore**: colección `numbers/{+E164}` con `{ owner, peerId }`.
- **Reglas** ([firestore.rules](firestore.rules)): para buscar un número hay que iniciar sesión con un correo
  verificado; nadie puede listar todos; solo el dueño puede cambiarlo o borrarlo.
- **Hosting**: la página para llamar ([`web/`](../web)) en <https://traduya-66592.web.app>.

La app habla con Firebase por su API REST, sin SDKs. La página web usa el SDK web de Firebase para iniciar sesión.

## Crear el proyecto (una vez, unos 5 minutos)

1. Entra en <https://console.firebase.google.com> y pulsa **Crear un proyecto**. Nombre: `traduya`.
   Google Analytics no hace falta.
2. **Compilación › Authentication › Comenzar › Método de acceso** y habilita:
   - **Anónimo**.
   - **Google** (elige tu correo como correo de asistencia).
   - **Correo electrónico/contraseña**, con la opción **Vínculo de correo electrónico (acceso sin contraseña)**.
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

## Publicar la página y las reglas

Desde la raíz del repositorio ([firebase.json](../firebase.json) y [.firebaserc](../.firebaserc)):

```bash
firebase login
firebase deploy --only hosting,firestore:rules
```

La dirección antigua (`camilo661.github.io/Traduya`, carpeta `docs/`) redirige a la nueva, así que los enlaces
y códigos QR que ya se compartieron siguen funcionando.

## Probar en local

Desde la raíz del repositorio:

```bash
firebase emulators:start --only auth,firestore,hosting --project demo-traduya
node --test firebase/test/                                                 # pruebas de las reglas
./gradlew :app:assembleDebug -Ptraduya.firebase.emulatorHost=10.0.2.2     # app contra el emulador
```

La página queda en <http://127.0.0.1:5180/?emulator=1>; con `?emulator=1` usa los emuladores de Firebase
(solo funciona en `localhost`).

## Siguiente paso: verificación por SMS

Hoy el número de quien recibe no se verifica. Para verificarlo, se cambia su usuario anónimo por
**Firebase Phone Auth** (SMS con código) y la regla de creación pasa a exigir
`request.auth.token.phone_number == number`.
