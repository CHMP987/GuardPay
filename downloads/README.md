# Descargas

APK de debug para instalar GuardPay en un Android, sin compilar.

| Archivo | Qué es |
| --- | --- |
| `android-debug.apk` | `com.guardpay.android` 0.1.0 (versionCode 1). minSdk 28, targetSdk 35. Firmado con la clave de depuración de Android. |

- Comando: `./gradlew :app:android:assembleDebug`
- Commit del código: `c1ef0ebedcec4960cadadfb4061225278677fc03` (`Merge main into ios`). Incluye el arreglo del doble toque al firmar.
- SHA-256: `bbfa5dadf52e09af20a2c193fb351bb194d8b3308ba3321c6b8acc1bbcd3f4ec`

```bash
adb install -r -t downloads/android-debug.apk
```

La firma con Keystore pide Android 13+ y bloqueo de pantalla. Cómo conectar el teléfono a testnet está en el README de la raíz.
