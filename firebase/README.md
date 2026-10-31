# Directorio de números (Firebase)

Las llamadas por WiFi con número usan un proyecto gratuito de Firebase (plan Spark):

- **Authentication anónima**: cada teléfono recibe un usuario anónimo que lo identifica como dueño de su número.
- **Firestore**: colección `numbers/{+E164}` con `{ owner, peerId }`.
- **Reglas** ([firestore.rules](firestore.rules)): cualquiera puede buscar un número concreto, nadie puede listar
  todos, y solo el dueño puede cambiarlo o borrarlo.

La app y la página web hablan con Firebase por su API REST, sin SDKs.

## Crear el proyecto (una vez, unos 5 minutos)

1. Entra en <https://console.firebase.google.com> y pulsa **Crear un proyecto**. Nombre: `traduya`.
   Google Analytics no hace falta.
2. **Compilación › Authentication › Comenzar › Método de acceso › Anónimo › Habilitar › Guardar.**
3. **Compilación › Firestore Database › Crear base de datos**, en **modo de producción**.
   Elige la ubicación más cercana (por ejemplo `southamerica-east1` o `nam5`).
4. En Firestore, pestaña **Reglas**: borra lo que hay, pega el contenido de `firestore.rules` y pulsa **Publicar**.
5. **Configuración del proyecto (engranaje) › General › Tus apps**, pulsa el icono web `</>` y registra
   la app como `Traduya web` (sin Hosting). Copia `apiKey` y `projectId`.

## Conectar Traduya

Pon los dos valores en:

- `gradle.properties`: `traduya.firebase.projectId` y `traduya.firebase.apiKey`.
- `docs/index.html`: `const DIRECTORY = { projectId: '…', apiKey: '…' }`.

La clave web de Firebase es pública por diseño; la seguridad está en las reglas.

## Probar en local

```bash
firebase emulators:start --only auth,firestore --project demo-traduya   # en esta carpeta
node --test test/                                                         # pruebas de las reglas
./gradlew :app:assembleDebug -Ptraduya.firebase.emulatorHost=10.0.2.2     # app contra el emulador
```

La página web acepta `?directory=http://127.0.0.1:8080/v1/projects/demo-traduya/databases/(default)/documents`
para buscar números en el emulador.

## Siguiente paso: verificación por SMS

Hoy el número no se verifica. Para verificarlo, se cambia el usuario anónimo por **Firebase Phone Auth**
(SMS con código) y la regla de creación pasa a exigir `request.auth.token.phone_number == number`.
