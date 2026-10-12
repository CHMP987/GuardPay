# Descargas

APK de debug para instalar GuardPay en un Android, sin compilar.

| Archivo | Qué es |
| --- | --- |
| `android-debug.apk` | `com.guardpay.android` 0.1.0 (versionCode 1). minSdk 28, targetSdk 35. Firmado con la clave de depuración de Android. |

- Comando: `./gradlew :app:android:assembleDebug`
- Commit del código: `dabe7f59446c2b47f6bcda5a8030e570c4ba7e64` (`Day 8: app scenes 1, 2, 3 and 6 on the physical Galaxy A54`)
- SHA-256: `55910b4ea7a08865269e01c2b9f686b40088debcdb4123d21ac1f130fd547f05`

```bash
adb install -r -t downloads/android-debug.apk
```

La firma con Keystore pide Android 13+ y bloqueo de pantalla. Cómo conectar el teléfono a testnet está en el README de la raíz.
