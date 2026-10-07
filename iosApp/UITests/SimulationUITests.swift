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
        type("10", into: "Monto (USDC)")
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
        type(stranger, into: "Otra cuenta")
        type("150", into: "Monto (USDC)")
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
        // By coordinate: an element tap asks for scroll-to-visible, which Compose does not answer.
        e.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
    }

    /// Compose exposes GpTextField as a TextView labelled with the field's label
    /// (seen in the CI's accessibility tree), not as a TextField.
    private func type(_ text: String, into label: String) {
        let field = app.textViews.matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
        if !field.waitForExistence(timeout: 10) {
            shot("fail-\(name.split(separator: " ").last ?? "")-campo")
            XCTFail("no text view: \(label)")
        }
        // The keyboard left by the previous field can cover this one, and Compose does not
        // answer XCUITest's scroll-to-visible (kAXErrorCannotComplete). So drag the form up
        // by hand until the field clears the keyboard, then tap and type by coordinate.
        let keyboard = app.keyboards.firstMatch
        for _ in 0..<4 where keyboard.exists && field.frame.maxY > keyboard.frame.minY - 8 {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)))
        }
        field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        _ = keyboard.waitForExistence(timeout: 3)
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
