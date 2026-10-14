# Análisis inicial de `edududas-debug.apk`

Inspección estática del contenido del APK (no se ejecutó en un dispositivo).

## Hallazgos
- Es un APK Android con múltiples archivos DEX (`classes.dex`, `classes2.dex`, etc.), por lo que contiene código compilado.
- Los nombres conservados en DEX incluyen `com.example.data.repository.EduDudasRepository`, `com.example.ui.EduDudasViewModel` y `EduDudasApp`.
- Se encuentran referencias a Firebase Authentication y Cloud Firestore, además de AndroidX Room/SQLite.
- También aparecen funciones con nombres como `createQuestion`, `postAnswer`, `shareResource`, `observeQuestions`, `observeAnswers`, `observeResources`, `observeNotifications` y `observeLeaderboard`.

## Qué significa
Es posible que la app ya tenga un mecanismo de sincronización con Firebase y un almacenamiento local de caché. La presencia de bibliotecas no prueba que la configuración de Firebase esté completa ni que el modo sin conexión comparta datos entre teléfonos. Room por sí solo guarda datos locales de cada instalación; Firestore, si está configurado correctamente, puede sincronizar datos por Internet.

Para que el teléfono Android sea el servidor y los otros teléfonos trabajen en la misma red local, habrá que adaptar el repositorio de datos para usar una API LAN. La aplicación auxiliar incluida es un prototipo independiente, no un parche automático del APK.

## Recuperación de código
La descompilación puede recuperar nombres, estructuras y parte de la lógica, pero no garantiza recuperar los archivos Kotlin originales, comentarios, nombres locales, proyecto Gradle ni configuración secreta. No debe intentarse recuperar ni publicar claves privadas o credenciales incluidas por error en el APK.

### Flujo recomendado en PC
1. Instala JADX GUI desde su distribución oficial.
2. Abre `edududas-debug.apk` en JADX y exporta el código fuente decompilado a una carpeta.
3. Revisa primero `com.example.data.repository.EduDudasRepository`, el modelo de datos y las llamadas a Firestore/Room.
4. No compartas `google-services.json`, claves, tokens ni datos personales. Si no tienes el archivo de configuración de Firebase, el proyecto puede no compilar o no sincronizar hasta reconfigurarlo.
5. Reconstruye un proyecto Android Studio limpio y adapta el repositorio para alternar entre modo Firebase y modo servidor LAN.

La app descompilada no debe reemplazar el APK original hasta que el nuevo proyecto compile y se haya probado con copias de los datos.
