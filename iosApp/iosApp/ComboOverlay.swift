import SwiftUI

struct ComboOverlay: View {
    let combo: Int
    @State private var show = false
    @State private var offset: CGFloat = 0
    @State private var opacity: Double = 0

    var comboText: String {
        switch combo {
        case 3:  return "🛡️ ¡Combo x3!"
        case 5:  return "❄️ ¡Combo x5!"
        case 7:  return "🔍 ¡Combo x7!"
        case 10: return "⭐ ¡Combo x10!"
        default: return "🔥 ¡Combo x\(combo)!"
        }
    }

    var comboColor: Color {
        switch combo {
        case 3:  return Color(hex: "4CAF50")
        case 5:  return Color(hex: "2196F3")
        case 7:  return Color(hex: "FF9800")
        case 10: return Color(hex: "FFD700")
        default: return Color(hex: "FF6B00")
        }
    }

    var body: some View {
        Text(comboText)
            .font(.system(size: 22, weight: .black))
            .foregroundStyle(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(comboColor)
            .clipShape(Capsule())
            .shadow(color: comboColor.opacity(0.5), radius: 8)
            .offset(y: offset)
            .opacity(opacity)
            .onChange(of: combo) { _, newCombo in
                guard newCombo >= 3 else { return }
                animate()
                HapticManager.shared.comboMilestone()
            }
    }

    private func animate() {
        offset = 20
        opacity = 0
        withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
            offset = -10
            opacity = 1
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            withAnimation(.easeOut(duration: 0.3)) {
                offset = -40
                opacity = 0
            }
        }
    }
}
