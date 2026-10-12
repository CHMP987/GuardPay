# usage: python -I p7video.py OUT_DIR SERIAL TAG WAIT_S   (p7ui.py next to it)
# Opens Pagar, waits WAIT_S (engine loads), pastes a scam, records the screen while tapping
# "Revisar el mensaje", then finds in the video when the button turns to "Analizando…" and back.
import sys, time, subprocess, os
OUT, SERIAL, TAG, WAIT = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
sys.argv = ["x", OUT, SERIAL]
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "p7ui.py"), encoding="utf-8").read().split("if DOWNLOAD:")[0])
import cv2, numpy as np
home(); tap("Pagar"); time.sleep(1.5)
find_scrolling("Te pidieron este pago"); tap("Te pidieron este pago"); time.sleep(1)
t_open = time.time()
msg = "Hola mami, soy yo, se me cayo el cel y este es mi numero nuevo. Ocupo que me haga un SINPE de 85000 ya, es urgente. No le diga a papi."
h = find("Pega aqu", tries=3); x, y = center(h[1]); adb("shell", "input", "tap", str(x), str(y)); time.sleep(0.5)
adb("shell", "input", "text", msg.replace(" ", "%s").replace(",", "\,")); adb("shell", "input", "keyevent", "4"); time.sleep(1)
find_scrolling("Revisar el mensaje")
b = find("Revisar el mensaje")[1]; x1, y1, x2, y2 = map(int, __import__("re").findall(r"\d+", b))
rest = WAIT - (time.time() - t_open)
if rest > 0: time.sleep(rest)
log(TAG, "tapping", round(time.time() - t_open, 1), "s after opening the message box; rss", rss())
rec = subprocess.Popen([ADB, "-s", SERIAL, "shell", "screenrecord", "--bit-rate", "4000000", "--time-limit", "40", f"/sdcard/{TAG}.mp4"])
time.sleep(2.0)
adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
time.sleep(25)
adb("shell", "pkill", "-INT", "screenrecord"); rec.wait(timeout=30); time.sleep(2)
mp4 = os.path.join(OUT, f"{TAG}.mp4"); adb("pull", f"/sdcard/{TAG}.mp4", mp4); adb("shell", "rm", f"/sdcard/{TAG}.mp4")
# Frame-to-frame change inside the button: the first is the tap ("Analizando…"),
# the next is the reading landing (back to "Revisar el mensaje", card below).
cap = cv2.VideoCapture(mp4); prev = None; changes = []
while True:
    ok, f = cap.read()
    if not ok: break
    t = cap.get(cv2.CAP_PROP_POS_MSEC)
    g = cv2.cvtColor(f[y1:y2, x1:x2], cv2.COLOR_BGR2GRAY).astype(np.int16)
    if prev is not None and float(np.abs(g - prev).mean()) > 3: changes.append(t)
    prev = g
first = changes[0]; later = [t for t in changes if t - first > 1000]
log(TAG, "button changed at", round(first / 1000, 2), "s; changed back at", later and round(later[0] / 1000, 2), "s; reading took",
    later and round((later[0] - first) / 1000, 2), "s")
adb("shell", "input", "keyevent", "4"); time.sleep(4); log(TAG, "after back rss", rss())
