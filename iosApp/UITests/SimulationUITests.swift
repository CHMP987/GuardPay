import XCTest

/// Walks scenes 1, 2 and 3 over the in-memory simulation (SIMULATED: nothing reaches
/// Stellar) and saves a screenshot per step. GP_SHOTS is the host folder for them;
/// xcodebuild passes it in as TEST_RUNNER_GP_SHOTS.
final class SimulationUITests: XCTestCase {
    private let stranger = "GALVMKSSOCH5QOOQHCFI53LBQUIZ2HDED3KYN7MCAAJL7ZQFITADTAXK"
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    func test1_entryShowsTheSimulationBanner() {
        wait("Entrar a mi cuenta")
        wait("Simulación")
        shot("01-entrada")
    }

    func test2_ownerPaysAContactAndGoesBackBySwipe() {
        tap("Entrar a mi cuenta")
        wait("USDC")  // the balance, once the simulated read settles
        shot("02-inicio")
        tap("Pagar")
        tap("Mamá")
        type("10", into: "Monto (USDC)", field: 0)
        tap("Revisar")
        tap("Firmar y enviar")
        wait("Enviado", timeout: 30)
        shot("03-enviado")
        swipeBack()
        wait("Saldo")
        swipeBack()
        wait("Entrar a mi cuenta")
        shot("04-volver-a-entrada")
    }

    func test3_ownerHoldsAndGuardianStops() {
        tap("Entrar a mi cuenta")
        wait("Saldo")
        tap("Pagar")
        tap("Otra cuenta")
        type(stranger, into: "Otra cuenta", field: 0)
        type("150", into: "Monto (USDC)", field: 1)
        tap("Revisar")
        tap("Firmar y retener")
        wait("Retenido", timeout: 30)
        shot("05-retenido")
        swipeBack()
        swipeBack()
        tap("Soy guardián de alguien")
        tap("150 USDC")
        wait("Puedes detenerlo")
        shot("06-guardian-detalle")
        tap("Detener este pago")
        tap("Detener el pago", last: true)
        wait("Detenido", timeout: 30)
        shot("07-detenido")
    }

    // MARK: helpers

    private func element(_ label: String, last: Bool = false) -> XCUIElement {
        let q = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label))
        return last ? q.element(boundBy: max(q.count - 1, 0)) : q.firstMatch
    }

    @discardableResult
    private func wait(_ label: String, timeout: TimeInterval = 15, last: Bool = false) -> XCUIElement {
        let e = element(label, last: last)
        if !e.waitForExistence(timeout: timeout) {
            shot("fail-\(name.split(separator: " ").last ?? "")-\(label.prefix(20))")
            XCTFail("not found: \(label)")
        }
        return e
    }

    private func tap(_ label: String, last: Bool = false) {
        let e = wait(label, last: last)
        // The keyboard can cover the bottom of the screen; scroll up to reach the button.
        for _ in 0..<3 where !e.isHittable { app.swipeUp() }
        e.tap()
    }

    /// GpTextField draws its label as a separate text, so the field itself has no label.
    /// Take the n-th text field on screen; failing that, tap just below the label.
    private func type(_ text: String, into label: String, field index: Int) {
        let fields = app.textFields
        if fields.count > index {
            fields.element(boundBy: index).tap()
        } else {
            let l = wait(label, last: true)
            l.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 1.0)).withOffset(CGVector(dx: 0, dy: 32)).tap()
        }
        // With a hardware keyboard attached the on-screen one never shows; the field can
        // still have focus, so this only waits and typeText reports a missing focus.
        _ = app.keyboards.firstMatch.waitForExistence(timeout: 3)
        // Diagnosis: what the field looks like after the tap, and what XCUITest sees.
        let tag = "\(name.split(separator: " ").last ?? "")-\(label.prefix(12))"
        shot("diag-\(tag)")
        if let dir = ProcessInfo.processInfo.environment["GP_SHOTS"] {
            try? app.debugDescription.write(toFile: "\(dir)/tree-\(tag).txt", atomically: true, encoding: .utf8)
        }
        app.typeText(text)
    }

    /// The iOS edge swipe, which the app maps to back.
    private func swipeBack() {
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5)))
        sleep(1)
    }

    private func shot(_ name: String) {
        let png = XCUIScreen.main.screenshot().pngRepresentation
        let attachment = XCTAttachment(data: png, uniformTypeIdentifier: "public.png")
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
        if let dir = ProcessInfo.processInfo.environment["GP_SHOTS"] {
            try? FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
            try? png.write(to: URL(fileURLWithPath: dir).appendingPathComponent("\(name).png"))
        }
    }
}
