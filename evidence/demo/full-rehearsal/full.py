# usage: python full.py OWNER_SERIAL GUARDIAN_SERIAL CLASSPATH_FILE [RUNS]
# The six scenes of plan §13 in one sitting, RUNS times in a row (default 2), timed.
# Owner on a phone (fingerprint by the human), guardian on the emulator (test PIN),
# terminal scenes from DemoTerminal (its own throwaway testnet account, keys in memory).
# Before: both apps open on the account in testnet.json; the guardian's HoldWatchService running.
import subprocess, sys, time, re, os, threading, queue
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
JAVA = r"C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe"
S = os.path.dirname(os.path.abspath(__file__))
STRANGER = "GDLCCVSYCCGT3AW2CX5NDB457WYOCNYUY4CCN22KCQDD2LQFRTJLF2NB"
OWN, GUA, CPFILE = sys.argv[1], sys.argv[2], sys.argv[3]
RUNS = int(sys.argv[4]) if len(sys.argv) > 4 else 2
LOG = open(S + "/full.log", "a", encoding="utf-8")
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
def type_into(d, label, value):
    h = find(d, label, tries=3, last=True); x, y = center(h[1])
    adb(d, "shell", "input", "tap", str(x), str(y)); adb(d, "shell", "input", "text", value)
    adb(d, "shell", "input", "keyevent", "4"); time.sleep(1)
def home(d):
    for _ in range(6):
        if find(d, "Saldo"): return
        if find(d, "Entrar a mi"): tap(d, "Entrar a mi"); wait_for(d, "Saldo", 30); return
        adb(d, "shell", "input", "keyevent", "4"); time.sleep(1.5)
    raise SystemExit(f"{d}: Inicio not reached")
def held_alerts():
    return adb(GUA, "shell", "dumpsys", "notification", "--noredact").decode("utf8", "replace").count("Pago retenido: 150 USDC")

# terminal: DemoTerminal over stdin/stdout, every line logged
cp = open(CPFILE, encoding="utf-8").read().strip()
term = subprocess.Popen([JAVA, "-Xmx384m", "-Dstdout.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-cp", cp,
                         "com.guardpay.shared.stellar.DemoTerminalKt"],
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
lines = queue.Queue()
def pump():
    for raw in term.stdout: lines.put(raw.decode("utf-8", "replace").rstrip())
threading.Thread(target=pump, daemon=True).start()
def until_ready(timeout=600):
    t0 = time.time()
    while True:
        line = lines.get(timeout=max(1, timeout - (time.time() - t0)))
        if "SLF4J" in line: continue
        log("  terminal |", line)
        if line == "> listo": return time.time() - t0
def terminal(scene):
    term.stdin.write(f"{scene}\n".encode()); term.stdin.flush()
    dt = until_ready(180); log(f"terminal scene {scene}:", round(dt, 1), "s"); return dt

log("== setup (untimed): terminal provisions its own account")
until_ready()

for run in range(1, RUNS + 1):
    log(f"== run {run}: start")
    home(OWN); start = time.time()

    # 1. owner pays 10 to a contact
    t = time.time()
    tap(OWN, "Pagar"); tap(OWN, "Mam"); type_into(OWN, "Monto (USDC)", "10"); tap(OWN, "Revisar")
    wait_for(OWN, "Firmar y enviar", 20); tap(OWN, "Firmar y enviar")
    log("scene 1: FINGERPRINT on the phone")
    dt = wait_for(OWN, "Enviado", 240)
    log("scene 1: Enviado", round(time.time() - t, 1), "s;", round(dt, 1), "s after Firmar y enviar")
    time.sleep(1); shot(OWN, f"r{run}-1-enviado")

    # 2. owner holds 150 for a new account; the terminal tries to send a held payment at once
    t = time.time(); home(OWN); alerts = held_alerts()
    tap(OWN, "Pagar"); tap(OWN, "Otra cuenta"); time.sleep(1)
    type_into(OWN, "Otra cuenta", STRANGER); type_into(OWN, "Monto (USDC)", "150"); tap(OWN, "Revisar")
    wait_for(OWN, "Firmar y retener", 20); tap(OWN, "Firmar y retener")
    log("scene 2: FINGERPRINT on the phone")
    dt = wait_for(OWN, "Retenido", 240); queued = time.time()
    log("scene 2: Retenido", round(queued - t, 1), "s;", round(dt, 1), "s after Firmar y retener")
    time.sleep(1); shot(OWN, f"r{run}-2-retenido")
    terminal(2)
    log("scene 2 total", round(time.time() - t, 1), "s")

    # 3. guardian stops it from the notification
    t = time.time()
    while held_alerts() <= alerts:
        if time.time() - queued > 150: raise SystemExit("no notification on the emulator within 150 s")
        time.sleep(1)
    log("scene 3: notification", round(time.time() - queued, 1), "s after Retenido on the phone")
    adb(GUA, "shell", "cmd", "statusbar", "expand-notifications"); time.sleep(1.5); shot(GUA, f"r{run}-3-aviso")
    tap(GUA, "Pago retenido: 150")
    wait_for(GUA, "Puedes detenerlo", 30); tap(GUA, "Detener"); time.sleep(1.5)
    tap(GUA, "Detener el pago", last=True)
    wait_for(GUA, "Confirma con tu huella", 30); time.sleep(0.8)
    adb(GUA, "shell", "input", "text", "1234"); adb(GUA, "shell", "input", "keyevent", "66")
    dt = wait_for(GUA, "Detenido", 90)
    log("scene 3: Detenido on the emulator", round(time.time() - t, 1), "s;", round(dt, 1), "s after the PIN")
    time.sleep(1); shot(GUA, f"r{run}-3-detenido")
    seen = None
    for _ in range(10):
        if find(OWN, "Detenido"): seen = time.time(); break
        time.sleep(2)
    log("scene 3: Detenido on the phone:", seen is not None)
    if seen: shot(OWN, f"r{run}-3-telefono")

    # 4 and 5. terminal
    terminal(4)
    terminal(5)

    # 6. terminal approve + add_context_rule; the phone shows the account's rules
    t = time.time()
    terminal(6)
    home(OWN)
    for _ in range(3):
        if find(OWN, "Reglas de esta cuenta"): break
        adb(OWN, "shell", "input", "swipe", "540", "1900", "540", "700", "400"); time.sleep(1.5)
    tap(OWN, "Reglas de esta cuenta"); time.sleep(2)
    adb(OWN, "shell", "input", "swipe", "540", "1700", "540", "500", "400"); time.sleep(1.5)
    wait_for(OWN, "GuardianHold", 30)
    shot(OWN, f"r{run}-6-reglas")
    rule = [x for x, _ in texts(OWN) if "regla" in x]
    log("scene 6:", round(time.time() - t, 1), "s;", rule)

    total = time.time() - start
    log(f"== run {run}: six scenes in {total:.1f} s ({total / 60:.2f} min)", "UNDER 5 MIN" if total < 300 else "OVER 5 MIN")

term.stdin.write(b"q\n"); term.stdin.flush(); term.wait(30)
