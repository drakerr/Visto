import UIKit

final class HapticManager {
    static let shared = HapticManager()
    private init() {}

    private let impact = UIImpactFeedbackGenerator(style: .medium)
    private let heavy = UIImpactFeedbackGenerator(style: .heavy)
    private let notification = UINotificationFeedbackGenerator()
    private let selection = UISelectionFeedbackGenerator()

    func prepare() {
        impact.prepare()
        heavy.prepare()
        notification.prepare()
        selection.prepare()
    }

    // Tap correcto — satisfactorio
    func correctTap() {
        impact.impactOccurred(intensity: 0.8)
    }

    // Tap incorrecto — error claro
    func wrongTap() {
        notification.notificationOccurred(.error)
    }

    // Power-up ganado — especial
    func powerUpEarned() {
        heavy.impactOccurred(intensity: 1.0)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.impact.impactOccurred(intensity: 0.5)
        }
    }

    // Power-up usado
    func powerUpUsed() {
        impact.impactOccurred(intensity: 0.6)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
            self.impact.impactOccurred(intensity: 0.9)
        }
    }

    // Combo alto — celebración
    func comboMilestone() {
        for i in 0..<3 {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.08) {
                self.impact.impactOccurred(intensity: 0.4 + Double(i) * 0.3)
            }
        }
    }
}
