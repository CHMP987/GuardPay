# Descargas

APK de debug para instalar GuardPay en un Android, sin compilar.

| Archivo | Qué es |
| --- | --- |
| `android-debug.apk` | `com.guardpay.android` 0.1.0 (versionCode 1). minSdk 28, targetSdk 35. Firmado con la clave de depuración de Android. |

- Comando: `./gradlew :app:android:assembleDebug`
- Commit del código: `98504cd8dc47bcb2424931167c00e46a70024ef6` (`main`, 12 oct). Incluye la lectura de mensajes con IA (P7).
- SHA-256: `37567663239e8d155e76ae71d8b8a88168c3d8931dcac6f26569e786106be0f7`
- Tamaño: 68,9 MB. El modelo de IA (Gemma 4 E2B, 2,6 GB) no viene dentro: la app lo descarga de un commit fijado de Hugging Face y comprueba su SHA-256.

```bash
adb install -r -t downloads/android-debug.apk
```

La firma con Keystore pide Android 13+ y bloqueo de pantalla. Cómo conectar el teléfono a testnet está en el README de la raíz.
