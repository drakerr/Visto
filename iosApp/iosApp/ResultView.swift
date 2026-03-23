import SwiftUI
import Shared

struct ResultView: View {
    let result: GameResult
    let onPlayAgain: () -> Void

    @State private var showOutcome = false
    @State private var showStats = false
    @State private var showBreakdown = false
    @State private var showButtons = false
    @State private var particleOpacity = 0.0

    var outcomeEmoji: String {
        switch result.outcome {
        case .win:  return "🏆"
        case .lose: return "💀"
        case .draw: return "🤝"
        default:    return "🤝"
        }
    }

    var outcomeText: String {
        switch result.outcome {
        case .win:  return "¡VICTORIA!"
        case .lose: return "DERROTA"
        case .draw: return "EMPATE"
        default:    return "EMPATE"
        }
    }

    var outcomeColor: Color {
        switch result.outcome {
        case .win:  return Color(hex: "FFD700")
        case .lose: return Color(hex: "E53935")
        case .draw: return Color(hex: "78909C")
        default:    return Color(hex: "78909C")
        }
    }

    var bgGradient: [Color] {
        switch result.outcome {
        case .win:  return [Color(hex: "0D1B00"), Color(hex: "1A3300"), Color(hex: "0F3460")]
        case .lose: return [Color(hex: "1A0000"), Color(hex: "2D0000"), Color(hex: "0F3460")]
        case .draw: return [Color(hex: "0D0D1A"), Color(hex: "1A1A2E"), Color(hex: "0F3460")]
        default:    return [Color(hex: "0D0D1A"), Color(hex: "1A1A2E"), Color(hex: "0F3460")]
        }
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: bgGradient,
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            // Partículas decorativas para victoria
            if result.outcome == .win {
                ParticlesView()
                    .opacity(particleOpacity)
                    .ignoresSafeArea()
            }

            ScrollView {
                VStack(spacing: 28) {
                    Spacer().frame(height: 20)

                    // Outcome
                    if showOutcome {
                        VStack(spacing: 8) {
                            Text(outcomeEmoji)
                                .font(.system(size: 80))
                                .transition(.scale.combined(with: .opacity))
                            Text(outcomeText)
                                .font(.system(size: 40, weight: .black))
                                .foregroundStyle(outcomeColor)
                                .tracking(4)
                                .transition(.scale.combined(with: .opacity))
                        }
                    }

                    // Rival info
                    if showStats {
                        RivalCard(result: result)
                            .transition(.move(edge: .leading).combined(with: .opacity))
                    }

                    // Score comparativa
                    if showStats {
                        ScoreComparisonCard(result: result)
                            .transition(.move(edge: .trailing).combined(with: .opacity))
                    }

                    // Desglose de puntos
                    if showBreakdown {
                        BreakdownCard(result: result)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    // Botones
                    if showButtons {
                        VStack(spacing: 12) {
                            Button(action: onPlayAgain) {
                                Text("¡JUGAR DE NUEVO!")
                                    .font(.system(size: 18, weight: .black))
                                    .tracking(1)
                                    .foregroundStyle(Color(hex: "1A1A2E"))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 54)
                                    .background(outcomeColor)
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                        }
                        .padding(.horizontal, 24)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    Spacer().frame(height: 20)
                }
                .padding(.horizontal, 20)
            }
        }
        .onAppear { startAnimations() }
    }

    private func startAnimations() {
        withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
            showOutcome = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                showStats = true
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                showBreakdown = true
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                showButtons = true
            }
        }
        if result.outcome == .win {
            withAnimation(.easeIn(duration: 0.3).delay(0.2)) {
                particleOpacity = 1.0
            }
            HapticManager.shared.comboMilestone()
        } else if result.outcome == .lose {
            HapticManager.shared.wrongTap()
        }
    }
}

// MARK: - Rival Card

struct RivalCard: View {
    let result: GameResult

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color(hex: "0F3460"))
                    .frame(width: 56, height: 56)
                Text(result.rivalAvatar)
                    .font(.system(size: 30))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(result.rivalUsername)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                Text("Ranking #\(result.rivalRank)")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "AAAAAA"))
            }

            Spacer()
        }
        .padding(16)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
    }
}

// MARK: - Score Comparison

struct ScoreComparisonCard: View {
    let result: GameResult

    var body: some View {
        VStack(spacing: 16) {
            Text("RESULTADO")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color(hex: "AAAAAA"))
                .tracking(2)

            HStack(spacing: 0) {
                // Jugador local
                VStack(spacing: 6) {
                    Text("TÚ")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "AAAAAA"))
                        .tracking(1)
                    Text("\(result.localScore)")
                        .font(.system(size: 36, weight: .black))
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                    Text("\(result.localFound) obj")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "4CAF50"))
                }
                .frame(maxWidth: .infinity)

                // VS
                VStack(spacing: 4) {
                    Text("VS")
                        .font(.system(size: 18, weight: .black))
                        .foregroundStyle(Color(hex: "FFD700"))
                }
                .frame(width: 40)

                // Rival
                VStack(spacing: 6) {
                    Text("RIVAL")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "AAAAAA"))
                        .tracking(1)
                    Text("\(result.ghostScore)")
                        .font(.system(size: 36, weight: .black))
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                    Text("\(result.ghostFound) obj")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "E53935"))
                }
                .frame(maxWidth: .infinity)
            }

            // Mejor combo
            HStack(spacing: 8) {
                Text("🔥 Mejor combo:")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "AAAAAA"))
                Text("x\(result.maxCombo)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(result.maxCombo >= 5 ? Color(hex: "FF6B00") : .white)
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
    }
}

// MARK: - Breakdown Card

struct BreakdownCard: View {
    let result: GameResult

    var accuracy: Int {
        let total = Int(result.localFound) + Int(result.wrongTapCount)
        guard total > 0 else { return 0 }
        return Int(Float(result.localFound) / Float(total) * 100)
    }

    var body: some View {
        VStack(spacing: 10) {
            Text("ESTADÍSTICAS")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color(hex: "AAAAAA"))
                .tracking(2)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack {
                Spacer()
                Text("TÚ")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Color(hex: "4CAF50"))
                    .frame(width: 64, alignment: .trailing)
                Text("RIVAL")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Color(hex: "E53935"))
                    .frame(width: 64, alignment: .trailing)
            }

            Divider().background(Color.white.opacity(0.1))

            BreakdownRow(
                label: "Objetos hallados",
                emoji: "🎯",
                localValue: "\(result.localFound)",
                rivalValue: "\(result.ghostFound)"
            )
            Divider().background(Color.white.opacity(0.1))
            BreakdownRow(
                label: "Puntos acumulados",
                emoji: "⭐",
                localValue: "\(result.basePoints)",
                rivalValue: "\(result.ghostScore)"
            )
            Divider().background(Color.white.opacity(0.1))
            BreakdownRow(
                label: "Mejor racha",
                emoji: "🔥",
                localValue: "x\(result.maxCombo)",
                rivalValue: "x\(result.ghostMaxCombo)"
            )
            Divider().background(Color.white.opacity(0.1))

            // Precisión expandida
            PrecisionRow(
                accuracy: accuracy,
                hits: Int(result.localFound),
                misses: Int(result.wrongTapCount),
                rivalHits: Int(result.ghostFound),
                rivalMisses: Int(result.ghostWrongTaps),
                rivalAccuracy: Int(result.ghostAccuracy)
            )

            Divider().background(Color.white.opacity(0.2))

            HStack(spacing: 0) {
                Text("PUNTUACIÓN")
                    .font(.system(size: 13, weight: .black))
                    .foregroundStyle(.white)
                    .tracking(1)
                Spacer()
                Text("\(Int(result.localScore))")
                    .font(.system(size: 15, weight: .black))
                    .foregroundStyle(Color(hex: "FFD700"))
                    .frame(width: 64, alignment: .trailing)
                Text("\(Int(result.ghostScore))")
                    .font(.system(size: 15, weight: .black))
                    .foregroundStyle(Color(hex: "AAAAAA"))
                    .frame(width: 64, alignment: .trailing)
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
    }
}

// MARK: - Precision Row

struct PrecisionRow: View {
    let accuracy: Int
    let hits: Int
    let misses: Int
    let rivalHits: Int
    let rivalMisses: Int
    let rivalAccuracy: Int

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 0) {
                Text("🎯")
                    .font(.system(size: 13))
                Text(" Precisión")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "AAAAAA"))
                Spacer()
                Text("\(accuracy)%")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 64, alignment: .trailing)
                Text("\(rivalAccuracy)%")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "666688"))
                    .frame(width: 64, alignment: .trailing)
            }
            // Aciertos
            HStack(spacing: 0) {
                Text("     ↳ Aciertos")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "666688"))
                Spacer()
                Text("\(hits)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "4CAF50"))
                    .frame(width: 64, alignment: .trailing)
                Text("\(rivalHits)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "E53935"))
                    .frame(width: 64, alignment: .trailing)
            }
            // Fallos
            HStack(spacing: 0) {
                Text("     ↳ Fallos")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "666688"))
                Spacer()
                Text("\(misses)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "E53935"))
                    .frame(width: 64, alignment: .trailing)
                Text("\(rivalMisses)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "666688"))
                    .frame(width: 64, alignment: .trailing)
            }
        }
    }
}
struct BreakdownRow: View {
    let label: String
    let emoji: String
    let localValue: String
    let rivalValue: String

    var body: some View {
        HStack(spacing: 0) {
            Text(emoji).font(.system(size: 13))
            Text(" \(label)")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "AAAAAA"))
                .lineLimit(1)
            Spacer()
            Text(localValue)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 64, alignment: .trailing)
            Text(rivalValue)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Color(hex: "666688"))
                .frame(width: 64, alignment: .trailing)
        }
    }
}

// MARK: - Particles (victoria)

struct ParticlesView: View {
    let emojis = ["⭐","🏆","✨","🎉","🎊","💫","🌟"]
    @State private var particles: [(id: Int, x: CGFloat, y: CGFloat, emoji: String, scale: CGFloat, opacity: Double)] = []

    var body: some View {
        GeometryReader { geo in
            ZStack {
                ForEach(particles, id: \.id) { p in
                    Text(p.emoji)
                        .font(.system(size: 20))
                        .scaleEffect(p.scale)
                        .opacity(p.opacity)
                        .position(x: p.x, y: p.y)
                }
            }
            .onAppear {
                for i in 0..<20 {
                    let x = CGFloat.random(in: 0...geo.size.width)
                    let emoji = emojis.randomElement()!
                    particles.append((id: i, x: x, y: -20, emoji: emoji, scale: 1, opacity: 1))
                    DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.1) {
                        withAnimation(.easeIn(duration: Double.random(in: 1.5...3.0))) {
                            if let idx = particles.firstIndex(where: { $0.id == i }) {
                                particles[idx].y = geo.size.height + 40
                                particles[idx].opacity = 0
                            }
                        }
                    }
                }
            }
        }
    }
}
