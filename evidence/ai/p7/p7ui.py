# usage: python -I p7ui.py OUT_DIR SERIAL [--download]
# P7 in the real app: optional resumed in-app download, one scam reading timed from the tap,
# and the process RSS on the Pay screen vs after leaving it.
import subprocess, sys, time, re, os
OUT, SERIAL = sys.argv[1], sys.argv[2]
DOWNLOAD = "--download" in sys.argv
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
PKG = "com.guardpay.android"
FILES = f"/sdcard/Android/data/{PKG}/files"
MODEL = "gemma-4-E2B-it.litertlm"
os.makedirs(OUT, exist_ok=True)
LOG = open(os.path.join(OUT, "ui.log"), "a", encoding="utf-8")
sys.stdout.reconfigure(encoding="utf-8")

def log(*a):
    line = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(line, flush=True); LOG.write(line + "\n"); LOG.flush()

def adb(*a, timeout=120):
    return subprocess.run([ADB, "-s", SERIAL, *a], capture_output=True, timeout=timeout).stdout

def sh(cmd, timeout=600):
    return adb("shell", cmd, timeout=timeout).decode("utf8", "replace").strip()

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

def wait_for(label, timeout=90, poll=0.3):
    t0 = time.time()
    while time.time() - t0 < timeout:
        if find(label): return time.time() - t0
        time.sleep(poll)
    raise SystemExit(f"timeout waiting for {label}\n" + "\n".join(t for t, _ in texts()))

def swipe_up():
    adb("shell", "input", "swipe", "540", "1800", "540", "700", "300"); time.sleep(0.8)

def find_scrolling(label, swipes=6):
    for _ in range(swipes):
        if find(label): return True
        swipe_up()
    return bool(find(label))

def shot(name):
    open(os.path.join(OUT, f"{name}.png"), "wb").write(adb("exec-out", "screencap", "-p"))

def rss():
    pid = sh(f"pidof {PKG}")
    if not pid: return "no process"
    st = sh(f"cat /proc/{pid}/status")
    return " ".join(l.split()[0] + l.split()[1] for l in st.splitlines() if l.startswith(("VmRSS", "VmHWM")))

def home():
    adb("shell", "am", "force-stop", PKG); time.sleep(1)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity"); time.sleep(4)
    if find("Entrar a mi", tries=3): tap("Entrar a mi")
    wait_for("Pagar", 40)

if DOWNLOAD:
    # Hide the full model and leave a .part ~10 MB short, so the app must fetch the tail
    # from the pinned Hugging Face URL with a Range request, then check size and SHA-256.
    log("df:", sh(f"df -h {FILES} | tail -1"))
    sh(f"cd {FILES} && mv {MODEL} {MODEL}.bak && dd if={MODEL}.bak of={MODEL}.part bs=1048576 count=2458 2>&1 | tail -1")
    log("part bytes:", sh(f"stat -c %s {FILES}/{MODEL}.part"))

home(); log("home; rss", rss())
tap("Pagar"); time.sleep(1.5)
find_scrolling("Te pidieron este pago")
tap("Te pidieron este pago"); time.sleep(1)
log("pay screen; rss", rss())
WAIT = int(sys.argv[sys.argv.index("--wait") + 1]) if "--wait" in sys.argv else 0
if WAIT:
    time.sleep(WAIT); log("waited", WAIT, "s on Pay; rss", rss())

if DOWNLOAD:
    find_scrolling("Descargar el modelo")
    shot("p7-1-no-descargado")
    t0 = time.time(); tap("Descargar el modelo")
    wait_for("Descargando el modelo", 30, poll=0.2); shot("p7-2-descargando")
    log("downloading shown after", round(time.time() - t0, 1), "s")
    seen = set()
    while time.time() - t0 < 900:
        h = find("Descargando el modelo")
        if not h: break
        seen.add(h[0]); time.sleep(1)
    log("progress texts seen:", sorted(seen))
    gone = time.time() - t0
    log("download+check ended after", round(gone, 1), "s; screen:", [t for t, _ in texts() if "modelo" in t or "descarga" in t.lower()])
    log("files:", sh(f"ls -l {FILES} | grep gemma"))
    shot("p7-3-tras-descarga")

# One scam message, ASCII so `input text` can type it.
msg = "Hola mami, soy yo, se me cayo el cel y este es mi numero nuevo. Ocupo que me haga un SINPE de 85000 ya, es urgente. No le diga a papi."
h = find("Pega aqu", tries=3) or find("Mensaje", tries=3, last=True)
x, y = center(h[1]); adb("shell", "input", "tap", str(x), str(y)); time.sleep(0.5)
adb("shell", "input", "text", msg.replace(" ", "%s").replace(",", "\\,"))
adb("shell", "input", "keyevent", "4"); time.sleep(1)
find_scrolling("Revisar el mensaje")
t0 = time.time(); tap("Revisar el mensaje")
time.sleep(0.4); shot("p7-4-analizando")
log("analizando visible:", bool(find("Analizando")))
while not find("puede equivocarse"):
    if time.time() - t0 > 150: raise SystemExit("no card after 150 s")
    adb("shell", "input", "swipe", "540", "1500", "540", "1100", "150")
log("reading on screen after", round(time.time() - t0, 1), "s (tap to card)")
find_scrolling("No vemos se") or find_scrolling("Te sugerimos retener")
shot("p7-5-resultado")
swipe_up(); shown = [t for t, _ in texts()]
log("card texts:", [t for t in shown if t.startswith("·") or "sugerimos" in t or "señales" in t or "Pide" in t or "pide" in t])
log("forbidden on screen:", [t for t in shown if re.search(r"\b(segur[oa]s?|protegid[oa]s?|verificad[oa]s?)\b", t, re.I)])
log("on pay screen; rss", rss())

adb("shell", "input", "keyevent", "4"); time.sleep(1)
adb("shell", "input", "keyevent", "4"); time.sleep(4)
log("left pay screen; screen has Pagar:", bool(find("Pagar")), "rss", rss())
