# usage: python two.py OWNER_SERIAL GUARDIAN_SERIAL   (from `adb devices`)
# two devices: owner on a phone (fingerprint by the human), guardian on the emulator (test PIN)
import subprocess, sys, time, re, os
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
S = os.path.dirname(os.path.abspath(__file__))
STRANGER = "GDLCCVSYCCGT3AW2CX5NDB457WYOCNYUY4CCN22KCQDD2LQFRTJLF2NB"
OWN, GUA = sys.argv[1], sys.argv[2]
LOG = open(S + "/two.log", "a", encoding="utf-8")
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

# owner, A54
adb(OWN, "shell", "monkey", "-p", "com.guardpay.android", "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(3)
if find(OWN, "Entrar a mi", tries=3): tap(OWN, "Entrar a mi")
wait_for(OWN, "Saldo", 40); shot(OWN, "a54-01-inicio"); dump(OWN, "a54-01-inicio")
t0 = time.time()
tap(OWN, "Pagar"); tap(OWN, "Otra cuenta"); time.sleep(1)
type_into(OWN, "Otra cuenta", STRANGER); type_into(OWN, "Monto (USDC)", "150"); tap(OWN, "Revisar")
wait_for(OWN, "Firmar y retener", 20); shot(OWN, "a54-02-revisa"); tap(OWN, "Firmar y retener")
log("owner: waiting for the fingerprint on the A54")
dt = wait_for(OWN, "Retenido", 240); queued = time.time()
log("owner: Retenido shown", round(queued - t0, 1), "s after Pagar;", round(dt, 1), "s after Firmar y retener (includes the human's fingerprint)")
time.sleep(1.5); shot(OWN, "a54-03-retenido"); dump(OWN, "a54-03-retenido")

# guardian, emulator
seen = None
while time.time() - queued < 150:
    n = adb(GUA, "shell", "dumpsys", "notification", "--noredact").decode("utf8", "replace")
    if "Pago retenido: 150 USDC" in n: seen = time.time(); break
    time.sleep(1)
if not seen: raise SystemExit("no notification on the emulator within 150 s")
log("guardian: notification", round(seen - queued, 1), "s after Retenido showed on the A54")
adb(GUA, "shell", "cmd", "statusbar", "expand-notifications"); time.sleep(2); shot(GUA, "emu-04-aviso")
tap(GUA, "Pago retenido: 150"); t1 = time.time()
wait_for(GUA, "Puedes detenerlo", 30); time.sleep(1); shot(GUA, "emu-05-detalle"); dump(GUA, "emu-05-detalle")
tap(GUA, "Detener"); time.sleep(1.5)
tap(GUA, "Detener el pago", last=True)
wait_for(GUA, "Confirma con tu huella", 30); time.sleep(0.8)
adb(GUA, "shell", "input", "text", "1234"); adb(GUA, "shell", "input", "keyevent", "66")
dt = wait_for(GUA, "Detenido", 90)
log("guardian: Detenido", round(time.time() - t1, 1), "s after tapping the notification;", round(dt, 1), "s after the PIN")
time.sleep(1.5); shot(GUA, "emu-06-detenido"); dump(GUA, "emu-06-detenido")

# owner sees it: first on the screen it is on, then back on Inicio
t2 = time.time(); ok = False
for _ in range(20):
    if find(OWN, "Detenido"): ok = True; break
    time.sleep(2)
if not ok:
    adb(OWN, "shell", "input", "keyevent", "4"); time.sleep(3)
    for _ in range(20):
        if find(OWN, "Detenido"): ok = True; break
        time.sleep(2)
log("owner: Detenido visible on the A54:", ok, round(time.time() - t2, 1), "s after the guardian's Detenido")
shot(OWN, "a54-07-final"); dump(OWN, "a54-07-final")
