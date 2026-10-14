# EduDudas LAN Server (prototipo independiente)

Este proyecto es un **servidor provisional independiente** para Android. Incluye una API JSON básica para que, después de adaptar el cliente, varios dispositivos puedan consultar y registrar preguntas, respuestas y recursos en un único teléfono de la red local.

## Compilar en la nube con GitHub Actions (sin Android Studio local)

1. Entra en https://github.com/new e inicia sesión. Crea un repositorio llamado `edududas-lan-server`. Para una primera prueba puede ser **privado**.
2. Descomprime este ZIP en tu computadora o teléfono. Sube **el contenido** de esta carpeta al repositorio, manteniendo la carpeta `.github/workflows/`.
3. En GitHub abre el repositorio → **Actions** → **Build Android APK** → **Run workflow**. Si no aparece el flujo, confirma que `.github/workflows/android-build.yml` está en la rama principal.
4. Espera a que la ejecución termine con marca verde y abre esa ejecución.
5. En **Artifacts**, descarga `edududas-lan-server-debug-apk`. Descomprime el ZIP descargado: dentro estará `app-debug.apk`.
6. Pasa `app-debug.apk` al teléfono que actuará como servidor e instálalo. Si Android lo solicita, permite instalar aplicaciones desde el navegador o gestor de archivos usado.

El flujo compila con Java 17 y Gradle 8.9 en los servidores de GitHub; no requiere Android Studio instalado localmente. La compilación aún debe verificarse ejecutando el flujo en GitHub.

## Ejecutar

1. Conecta el teléfono servidor y los dispositivos clientes a la misma red Wi-Fi.
2. Abre **EduDudas LAN Server** y concede el permiso de notificaciones si lo solicita.
3. Pulsa **Iniciar servidor**. La API escucha en el puerto `8765`.
4. Desde otro dispositivo de la red, abre `http://IP-DEL-TELEFONO:8765/health`. Debe responder un JSON con `"ok":true`.

## API provisional

- `GET /health` no requiere clave.
- `GET /api/questions`, `POST /api/questions`
- `GET /api/answers`, `POST /api/answers`
- `GET /api/resources`, `POST /api/resources`

Las rutas `/api/*` requieren la cabecera `X-API-Key: cambia-esta-clave-lan`. Antes de pruebas reales, cambia `API_KEY` en `app/src/main/java/com/edududas/lanserver/LanServerService.kt` y vuelve a ejecutar el flujo de GitHub Actions.

Ejemplo:

```bash
curl -H "X-API-Key: cambia-esta-clave-lan" http://192.168.1.50:8765/api/questions
curl -X POST -H "X-API-Key: cambia-esta-clave-lan" -H "Content-Type: application/json" \
  -d '{"title":"¿Qué es una célula?","body":"Ejemplo de pregunta"}' \
  http://192.168.1.50:8765/api/questions
```

## Límite importante: todavía no integra el APK EduDudas original

Este APK auxiliar es una API independiente; instalarlo **no modifica** el APK original ni hace que EduDudas empiece automáticamente a usar el servidor. Como solo se dispone del APK original, el siguiente paso será inspeccionarlo/decompilarlo para identificar cómo guarda y sincroniza preguntas, respuestas, usuarios y recursos, y determinar si es viable adaptar el cliente. Puede que la integración requiera reconstruir y firmar una versión modificada del APK; no se puede garantizar sin analizar sus componentes y dependencias.

Este prototipo usa HTTP sin cifrar y una clave compartida, no tiene cuentas ni roles, y está pensado solo para una red local de confianza. No lo expongas a Internet ni uses datos sensibles. Android puede detener el servicio por batería; para pruebas, permite actividad en segundo plano.
