# usage: python f01.py OWNER_SERIAL GUARDIAN_SERIAL   (from `adb devices`)
# F-01 on two devices: the owner holds a payment to a trusted contact within the cap,
# the guardian sees it and lets it run, and the owner sends it once it matures.
# Owner on a phone (fingerprint by the human), guardian on the emulator.
import subprocess, sys, time, re, os
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
S = os.path.dirname(os.path.abspath(__file__))
CONTACT, AMOUNT = "Mamá", "10"
OWN, GUA = sys.argv[1], sys.argv[2]
LOG = open(S + "/f01.log", "a", encoding="utf-8")
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

# owner, phone: hold a payment to a trusted contact, within the cap
adb(OWN, "shell", "monkey", "-p", "com.guardpay.android", "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(3)
if find(OWN, "Entrar a mi", tries=3): tap(OWN, "Entrar a mi")
wait_for(OWN, "Saldo", 40); time.sleep(4); shot(OWN, "tel-01-inicio"); dump(OWN, "tel-01-inicio")
tap(OWN, "Pagar"); tap(OWN, CONTACT); time.sleep(1)
type_into(OWN, "Monto (USDC)", AMOUNT)
wait_for(OWN, "Sale ahora", 20); shot(OWN, "tel-02-sale-ahora"); dump(OWN, "tel-02-sale-ahora")
tap(OWN, "Retener este pago", last=True)
wait_for(OWN, "Elegiste retenerlo", 20); shot(OWN, "tel-03-elegiste"); dump(OWN, "tel-03-elegiste")
tap(OWN, "Revisar")
wait_for(OWN, "Firmar y retener", 20); shot(OWN, "tel-04-revisa"); dump(OWN, "tel-04-revisa")
tap(OWN, "Firmar y retener")
log("owner: waiting for the fingerprint on the phone (hold)")
dt = wait_for(OWN, "Retenido", 240); queued = time.time()
log("owner: Retenido shown", round(dt, 1), "s after Firmar y retener (includes the human's fingerprint)")
time.sleep(1.5); shot(OWN, "tel-05-retenido"); dump(OWN, "tel-05-retenido")

# guardian, emulator: sees it and lets it run
seen = None
while time.time() - queued < 150:
    n = adb(GUA, "shell", "dumpsys", "notification", "--noredact").decode("utf8", "replace")
    if f"Pago retenido: {AMOUNT} USDC" in n: seen = time.time(); break
    time.sleep(1)
if not seen: raise SystemExit("no notification on the emulator within 150 s")
log("guardian: notification", round(seen - queued, 1), "s after Retenido showed on the phone")
adb(GUA, "shell", "cmd", "statusbar", "expand-notifications"); time.sleep(2); shot(GUA, "emu-01-aviso")
tap(GUA, f"Pago retenido: {AMOUNT}")
wait_for(GUA, "Puedes detenerlo", 30); time.sleep(1); shot(GUA, "emu-02-detalle"); dump(GUA, "emu-02-detalle")
log("guardian: Detalle open; not stopping it")

# owner, phone: wait for maturity, then send with a second signature
dt = wait_for(OWN, "Firmar y enviar", 400)
log("owner: Firmar y enviar shown", round(time.time() - queued, 1), "s after Retenido")
time.sleep(1); shot(OWN, "tel-06-listo"); dump(OWN, "tel-06-listo")
tap(OWN, "Firmar y enviar")
log("owner: waiting for the fingerprint on the phone (release)")
dt = wait_for(OWN, "Enviado", 240)
log("owner: Enviado shown", round(dt, 1), "s after Firmar y enviar (includes the human's fingerprint)")
time.sleep(1.5); shot(OWN, "tel-07-enviado"); dump(OWN, "tel-07-enviado")

# guardian sees the outcome on the Detalle that is open
ok = False
for _ in range(45):
    if find(GUA, "Enviado"): ok = True; break
    time.sleep(2)
log("guardian: Enviado on Detalle" if ok else "guardian: Enviado not seen within ~3 min")
shot(GUA, "emu-03-final"); dump(GUA, "emu-03-final")

# owner back on Inicio: the cap after the release
adb(OWN, "shell", "input", "keyevent", "4"); time.sleep(6)
shot(OWN, "tel-08-inicio"); dump(OWN, "tel-08-inicio")
log("done")
