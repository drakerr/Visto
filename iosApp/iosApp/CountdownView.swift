import SwiftUI
import Shared

struct CountdownView: View {
    let state: GameState
    @State private var count = 3
    @State private var scale: CGFloat = 1.0
    @State private var opacity: Double = 1.0
    @State private var showTargets = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.85).ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()

                VStack(spacing: 8) {
                    Text("¡PREPÁRATE!")
                        .font(.system(size: 28, weight: .black))
                        .foregroundStyle(Color(hex: "FFD700"))
                        .tracking(4)
                        .opacity(showTargets ? 1 : 0)
                        .animation(.easeIn(duration: 0.3), value: showTargets)

                    Text("Encuentra estos objetos")
                        .font(.system(size: 15))
                        .foregroundStyle(Color(hex: "AAAAAA"))
                        .opacity(showTargets ? 1 : 0)
                        .animation(.easeIn(duration: 0.3).delay(0.1), value: showTargets)
                }

                if showTargets {
                    HStack(spacing: 12) {
                        ForEach(state.activeTargets, id: \.id) { target in
                            CountdownTargetCard(item: target)
                        }
                    }
                    .transition(.scale.combined(with: .opacity))
                }

                Spacer()

                // Cuenta atrás
                ZStack {
                    Circle()
                        .stroke(Color(hex: "FFD700").opacity(0.3), lineWidth: 4)
                        .frame(width: 100, height: 100)

                    Circle()
                        .trim(from: 0, to: CGFloat(count) / 3.0)
                        .stroke(Color(hex: "FFD700"), lineWidth: 4)
                        .frame(width: 100, height: 100)
                        .rotationEffect(.degrees(-90))
                        .animation(.linear(duration: 1.0), value: count)

                    Text("\(count)")
                        .font(.system(size: 52, weight: .black))
                        .foregroundStyle(.white)
                        .scaleEffect(scale)
                        .opacity(opacity)
                }

                Text("¡A buscar!")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "666688"))
                    .padding(.bottom, 40)
            }
        }
        .onAppear {
            showTargets = true
            startCountdown()
            HapticManager.shared.prepare()
        }
    }

    private func startCountdown() {
        animateNumber()
        for i in stride(from: 2, through: 0, by: -1) {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(3 - i)) {
                count = i
                if i > 0 { animateNumber() }
                HapticManager.shared.correctTap()
            }
        }
    }

    private func animateNumber() {
        scale = 1.4
        opacity = 1.0
        withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
            scale = 1.0
        }
    }
}

struct CountdownTargetCard: View {
    let item: BoardItem
    @State private var appeared = false

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(hex: "0F3460"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color(hex: "FFD700"), lineWidth: 2)
                    )
                Text(item.imageKey)
                    .font(.system(size: 36))
            }
            .frame(width: 70, height: 70)
            .scaleEffect(appeared ? 1 : 0.5)
            .opacity(appeared ? 1 : 0)
            .animation(
                .spring(response: 0.4, dampingFraction: 0.6)
                .delay(Double(item.id % 4) * 0.1),
                value: appeared
            )
        }
        .onAppear { appeared = true }
    }
}
