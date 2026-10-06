# usage: python rehearse.py RUN_TAG [scenes...]   scenes: 1 2 3 6
import subprocess, sys, time, re, os, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importlib.util
spec = importlib.util.spec_from_file_location("ui", os.path.join(os.path.dirname(os.path.abspath(__file__)), "ui.py"))
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
S = os.path.dirname(os.path.abspath(__file__))
STRANGER = "GALVMKSSOCH5QOOQHCFI53LBQUIZ2HDED3KYN7MCAAJL7ZQFITADTAXK"
TAG = sys.argv[1]
LOG = open(os.path.join(S, f"rehearsal-{TAG}.log"), "a", encoding="utf-8")

def log(*a):
    line = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(line, flush=True); LOG.write(line + "\n"); LOG.flush()

def adb(*a, timeout=60):
    return subprocess.run([ADB, *a], capture_output=True, timeout=timeout).stdout

def texts():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    x = adb("shell", "cat", "/sdcard/ui.xml").decode("utf8", "replace")
    return [(m[1] or m[2], m[3]) for m in re.finditer(r'text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="([^"]+)"', x) if m[1] or m[2]]

def center(b):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", b)); return (x1 + x2) // 2, (y1 + y2) // 2

def find(label, tries=1, wait=1.0, last=False):
    for _ in range(tries):
        hits = [(t, b) for t, b in texts() if label in t]
        if hits: return hits[-1] if last else hits[0]
        time.sleep(wait)
    return None

def tap(label, tries=8, last=False):
    h = find(label, tries, last=last)
    if not h: raise SystemExit(f"not found: {label}\n" + "\n".join(t for t, _ in texts()))
    x, y = center(h[1]); adb("shell", "input", "tap", str(x), str(y)); return h[0]

def wait_for(label, timeout=90):
    t0 = time.time()
    while time.time() - t0 < timeout:
        h = find(label)
        if h: return time.time() - t0
        time.sleep(1)
    raise SystemExit(f"timeout waiting for {label}\n" + "\n".join(t for t, _ in texts()))

def pin():
    wait_for("Confirma con tu huella", 30); time.sleep(0.8)
    adb("shell", "input", "text", "1234"); adb("shell", "input", "keyevent", "66")

def shot(name):
    open(os.path.join(S, f"{TAG}-{name}.png"), "wb").write(adb("exec-out", "screencap", "-p"))

def screen_dump(name):
    with open(os.path.join(S, f"{TAG}-{name}.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(t for t, _ in texts()))

class Rec:
    def __init__(self, name): self.name = name
    def __enter__(self):
        self.p = subprocess.Popen([ADB, "shell", "screenrecord", "--bit-rate", "1500000", "--time-limit", "180", f"/sdcard/{TAG}-{self.name}.mp4"])
        time.sleep(1.5); return self
    def __exit__(self, *a):
        time.sleep(2)
        adb("shell", "pkill", "-INT", "screenrecord"); self.p.wait(timeout=20); time.sleep(2)
        adb("pull", f"/sdcard/{TAG}-{self.name}.mp4", os.path.join(S, f"{TAG}-{self.name}.mp4"))

def home():
    adb("shell", "am", "start", "-n", "com.guardpay.android/.MainActivity")
    time.sleep(3)
    if find("Entrar a mi", tries=3):
        tap("Entrar a mi"); wait_for("Saldo", 30)
    else:
        for _ in range(4):
            if find("Saldo"): break
            adb("shell", "input", "keyevent", "4"); time.sleep(1.5)
            if find("Entrar a mi"): tap("Entrar a mi"); wait_for("Saldo", 30); break

def fill_amount(amount):
    h = find("Monto (USDC)", tries=3, last=True); x, y = center(h[1])
    adb("shell", "input", "tap", str(x), str(y)); adb("shell", "input", "text", amount)
    adb("shell", "input", "keyevent", "4"); time.sleep(1)

def scene1():
    home(); t0 = time.time()
    tap("Pagar"); tap("Mam"); fill_amount("10"); tap("Revisar")
    wait_for("Firmar y enviar", 20); tap("Firmar y enviar"); pin()
    dt = wait_for("Enviado", 90); total = time.time() - t0
    time.sleep(1.5); shot("s1-enviado"); screen_dump("s1-enviado")
    log("scene1 total", round(total, 1), "s; after PIN", round(dt, 1), "s")

def scene2():
    home(); t0 = time.time()
    tap("Pagar"); tap("Otra cuenta"); time.sleep(1)
    h = find("Otra cuenta", tries=3, last=True); x, y = center(h[1])
    adb("shell", "input", "tap", str(x), str(y)); adb("shell", "input", "text", STRANGER)
    adb("shell", "input", "keyevent", "4"); time.sleep(1)
    fill_amount("150"); tap("Revisar")
    wait_for("Firmar y retener", 20); shot("s2-revisa"); tap("Firmar y retener"); pin()
    dt = wait_for("Retenido", 90); total = time.time() - t0
    time.sleep(1.5); shot("s2-retenido"); screen_dump("s2-retenido")
    log("scene2 total", round(total, 1), "s; after PIN", round(dt, 1), "s")
    return time.time()

def scene3(queued_at):
    t0 = time.time(); seen = None
    while time.time() - t0 < 120:
        n = adb("shell", "dumpsys", "notification", "--noredact").decode("utf8", "replace")
        if "Pago retenido: 150 USDC" in n: seen = time.time(); break
        time.sleep(1)
    if not seen: raise SystemExit("no notification within 120 s")
    log("scene3 notification", round(seen - queued_at, 1), "s after Retenido showed")
    adb("shell", "cmd", "statusbar", "expand-notifications"); time.sleep(2)
    shot("s3-aviso")
    tap("Pago retenido: 150"); t1 = time.time()
    wait_for("Puedes detenerlo", 30); time.sleep(1); shot("s3-detalle"); screen_dump("s3-detalle")
    tap("Detener", last=False); time.sleep(1.5); shot("s3-confirmar")
    tap("Detener el pago", last=True); pin()
    dt = wait_for("Detenido", 90); total = time.time() - t1
    time.sleep(1.5); shot("s3-detenido"); screen_dump("s3-detenido")
    log("scene3 from tap on notification", round(total, 1), "s; after PIN", round(dt, 1), "s")

def scene6():
    home(); t0 = time.time()
    for _ in range(3):
        if find("Reglas de esta cuenta"): break
        adb("shell", "input", "swipe", "540", "1900", "540", "700", "400"); time.sleep(1.5)
    tap("Reglas de esta cuenta"); time.sleep(2)
    adb("shell", "input", "swipe", "540", "1700", "540", "500", "400"); time.sleep(1.5)
    wait_for("GuardianHold", 30)
    shot("s6-reglas"); screen_dump("s6-reglas")
    log("scene6 total", round(time.time() - t0, 1), "s")

scenes = sys.argv[2:] or ["1", "2", "3", "6"]
for s in scenes:
    log("== run", TAG, "scene", s)
    if s == "1":
        with Rec("s1"): scene1()
    elif s == "23":
        with Rec("s23"):
            q = scene2(); scene3(q)
    elif s == "6":
        with Rec("s6"): scene6()
