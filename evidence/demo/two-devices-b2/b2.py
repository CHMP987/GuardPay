# usage: python b2.py OWNER_SERIAL GUARDIAN_SERIAL   (from `adb devices`)
# B-2 on two devices, against the account with the indexed registry:
#  1. the owner holds 20 USDC to a trusted contact (by choice) and the guardian stops it;
#  2. the owner pays 60 USDC to the same contact, over the cap, so it is held;
#     the guardian lets it run and the owner sends it once it matures.
# Owner on a phone (fingerprint by the human), guardian on the emulator (PIN 1234).
import subprocess, sys, time, re, os
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
S = os.path.dirname(os.path.abspath(__file__))
CONTACT, STOPPED, OVER = "Mamá", "20", "60"
PIN = "1234"
OWN, GUA = sys.argv[1], sys.argv[2]
# "stop": the hold of 20 already exists and the emulator shows its Detalle; resume at "Detener este pago".
# "send": the hold of 60 has matured and the phone shows its "Firmar y enviar"; resume there.
RESUME = sys.argv[3] if len(sys.argv) > 3 else ""
LOG = open(S + "/b2.log", "a", encoding="utf-8")
def log(*a):
    line = time.strftime("%H:%M:%S ", time.gmtime()) + "UTC " + " ".join(str(x) for x in a)
    print(line, flush=True); LOG.write(line + "\n"); LOG.flush()
def adb(d, *a, timeout=60): return subprocess.run([ADB, "-s", d, *a], capture_output=True, timeout=timeout).stdout
def texts(d):
    adb(d, "shell", "uiautomator", "dump", "/sdcard/ui.xml")
    x = adb(d, "shell", "cat", "/sdcard/ui.xml").decode("utf8", "replace")
    return [(m[1] or m[2], m[3]) for m in re.finditer(r'text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="([^"]+)"', x) if m[1] or m[2]]
def center(b):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", b)); return (x1 + x2) // 2, (y1 + y2) // 2
def find(d, label, tries=1, wait=1.0, last=False):
    for _ in range(tries):
        hits = [(t, b) for t, b in texts(d) if label in t]
        if hits: return hits[-1] if last else hits[0]
        time.sleep(wait)
def tap(d, label, tries=8, last=False):
    h = find(d, label, tries, last=last)
    if not h: raise SystemExit(f"{d} not found: {label}\n" + "\n".join(t for t, _ in texts(d)))
    x, y = center(h[1]); adb(d, "shell", "input", "tap", str(x), str(y))
def wait_for(d, label, timeout=90):
    t0 = time.time()
    while time.time() - t0 < timeout:
        if find(d, label): return time.time() - t0
        time.sleep(1)
    raise SystemExit(f"{d} timeout waiting for {label}\n" + "\n".join(t for t, _ in texts(d)))
def shot(d, name): open(f"{S}/{name}.png", "wb").write(adb(d, "exec-out", "screencap", "-p"))
def dump(d, name): open(f"{S}/{name}.txt", "w", encoding="utf-8").write("\n".join(t for t, _ in texts(d)))
def type_into(d, label, value):
    h = find(d, label, tries=3, last=True); x, y = center(h[1])
    adb(d, "shell", "input", "tap", str(x), str(y)); adb(d, "shell", "input", "text", value)
    adb(d, "shell", "input", "keyevent", "4"); time.sleep(1)
def notified(amount, since, timeout=150):
    while time.time() - since < timeout:
        n = adb(GUA, "shell", "dumpsys", "notification", "--noredact").decode("utf8", "replace")
        if f"Pago retenido: {amount} USDC" in n: return time.time()
        time.sleep(1)
    raise SystemExit(f"no notification for {amount} on the emulator within {timeout} s")
def open_notification(amount):
    adb(GUA, "shell", "cmd", "statusbar", "expand-notifications"); time.sleep(2)
    tap(GUA, f"Pago retenido: {amount}")
    wait_for(GUA, "Puedes detenerlo", 30); time.sleep(1)
def enter_pin(d):
    # `input text` before the keypad is up gets lost, and the transaction is only valid for 60 s.
    wait_for(d, "Confirma con tu huella", 20); time.sleep(1)
    for k in PIN: tap(d, k, last=True)
    tap(d, "Enter")
def owner_pay(amount, hold_by_choice, tag):
    tap(OWN, "Pagar"); tap(OWN, CONTACT); time.sleep(1)
    type_into(OWN, "Monto (USDC)", amount)
    if hold_by_choice:
        wait_for(OWN, "Sale ahora", 20); shot(OWN, f"tel-{tag}-sale-ahora"); dump(OWN, f"tel-{tag}-sale-ahora")
        tap(OWN, "Retener este pago", last=True)
        wait_for(OWN, "Elegiste retenerlo", 20)
    else:
        wait_for(OWN, "Supera tu tope", 20)
    shot(OWN, f"tel-{tag}-con-espera"); dump(OWN, f"tel-{tag}-con-espera")
    tap(OWN, "Revisar")
    wait_for(OWN, "Firmar y retener", 20); shot(OWN, f"tel-{tag}-revisa"); dump(OWN, f"tel-{tag}-revisa")
    tap(OWN, "Firmar y retener")
    log(f"owner: waiting for the fingerprint on the phone (hold {amount})")
    dt = wait_for(OWN, "Retenido", 240); t = time.time()
    log(f"owner: Retenido {amount} shown", round(dt, 1), "s after Firmar y retener (includes the human's fingerprint)")
    time.sleep(1.5); shot(OWN, f"tel-{tag}-retenido"); dump(OWN, f"tel-{tag}-retenido")
    return t

# The guardian only polls while its app has started HoldWatchService, so open it first.
adb(GUA, "shell", "monkey", "-p", "com.guardpay.android", "-c", "android.intent.category.LAUNCHER", "1")
adb(OWN, "shell", "monkey", "-p", "com.guardpay.android", "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(3)
if not RESUME:
    if find(OWN, "Entrar a mi", tries=3): tap(OWN, "Entrar a mi")
    wait_for(OWN, "Saldo", 40); time.sleep(4); shot(OWN, "tel-01-inicio"); dump(OWN, "tel-01-inicio")

def send():
    tap(OWN, "Firmar y enviar")
    log("owner: waiting for the fingerprint on the phone (release)")
    dt = wait_for(OWN, "Enviado", 240)
    log("owner: Enviado shown", round(dt, 1), "s after Firmar y enviar (includes the human's fingerprint)")
    time.sleep(1.5); shot(OWN, "tel-07-enviado"); dump(OWN, "tel-07-enviado")
    ok = False
    for _ in range(45):
        if find(GUA, "Enviado"): ok = True; break
        time.sleep(2)
    log("guardian: Enviado on Detalle" if ok else "guardian: Enviado not seen within ~3 min")
    shot(GUA, "emu-06-final"); dump(GUA, "emu-06-final")
    adb(OWN, "shell", "input", "keyevent", "4"); time.sleep(6)
    shot(OWN, "tel-08-inicio"); dump(OWN, "tel-08-inicio")
    log("done")

if RESUME == "send":
    log("resume: the phone shows the matured hold of 60")
    wait_for(OWN, "Firmar y enviar", 20); shot(OWN, "tel-06-listo"); dump(OWN, "tel-06-listo")
    send(); raise SystemExit

# 1. a hold by choice, stopped by the guardian
if not RESUME:
    queued = owner_pay(STOPPED, True, "02")
    seen = notified(STOPPED, queued)
    log("guardian: notification", STOPPED, round(seen - queued, 1), "s after Retenido showed on the phone")
    shot(GUA, "emu-01-aviso"); open_notification(STOPPED)
else:
    log("resume: the guardian retries the stop on the Detalle already open")
shot(GUA, "emu-02-detalle"); dump(GUA, "emu-02-detalle")
tap(GUA, "Detener este pago")
wait_for(GUA, "Detener el pago", 20); time.sleep(1); shot(GUA, "emu-03-confirmar"); dump(GUA, "emu-03-confirmar")
tap(GUA, "Detener el pago", last=True); t0 = time.time()
enter_pin(GUA)
log("guardian: PIN entered", round(time.time() - t0, 1), "s after Detener el pago")
dt = wait_for(GUA, "Detenido", 120)
log("guardian: Detenido shown", round(time.time() - t0, 1), "s after Detener el pago (includes the PIN)")
time.sleep(1); shot(GUA, "emu-04-detenido"); dump(GUA, "emu-04-detenido")
dt = wait_for(OWN, "Detenido", 120)
log("owner: Detenido shown on the phone", round(time.time() - t0, 1), "s after the guardian's Detener el pago")
time.sleep(1); shot(OWN, "tel-03-detenido"); dump(OWN, "tel-03-detenido")

# 2. over the cap: held, matures, sent
adb(OWN, "shell", "input", "keyevent", "4"); time.sleep(4)
wait_for(OWN, "Saldo", 30); shot(OWN, "tel-04-inicio"); dump(OWN, "tel-04-inicio")
queued = owner_pay(OVER, False, "05")
seen = notified(OVER, queued)
log("guardian: notification", OVER, round(seen - queued, 1), "s after Retenido showed on the phone")
adb(GUA, "shell", "input", "keyevent", "3"); time.sleep(1)
open_notification(OVER); shot(GUA, "emu-05-detalle"); dump(GUA, "emu-05-detalle")
log("guardian: Detalle open; not stopping it")
wait_for(OWN, "Firmar y enviar", 400)
log("owner: Firmar y enviar shown", round(time.time() - queued, 1), "s after Retenido")
time.sleep(1); shot(OWN, "tel-06-listo"); dump(OWN, "tel-06-listo")
send()
