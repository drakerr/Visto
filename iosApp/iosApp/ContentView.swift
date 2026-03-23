import SwiftUI
import Shared

// MARK: - Root

struct ContentView: View {
    @StateObject private var wrapper = GameViewModelWrapper()
    @State private var isTransitioning = false

    var body: some View {
        ZStack {
            switch wrapper.state.phase {
            case .idle:
                MenuView(state: wrapper.state) {
                    withAnimation(.easeIn(duration: 0.2)) { isTransitioning = true }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                        wrapper.startGame()
                        withAnimation(.easeOut(duration: 0.3)) { isTransitioning = false }
                    }
                }
                .transition(.opacity)

            case .countdown:
                ZStack {
                    GameView(
                        state: wrapper.state,
                        onItemTap: { _ in },
                        onUsePowerUp: { _ in }
                    )
                    .disabled(true)
                    .blur(radius: 4)

                    CountdownView(state: wrapper.state)
                }
                .transition(.opacity)

            case .playing:
                GameView(
                    state: wrapper.state,
                    onItemTap: { wrapper.selectCell(cellId: $0) },
                    onUsePowerUp: { wrapper.usePowerUp(powerUpId: $0) }
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))

            case .finished:
                if let result = wrapper.state.result {
                    ResultView(
                        result: result,
                        onPlayAgain: {
                            withAnimation(.easeIn(duration: 0.2)) { isTransitioning = true }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                wrapper.resetGame()
                                withAnimation(.easeOut(duration: 0.3)) { isTransitioning = false }
                            }
                        }
                    )
                    .transition(.move(edge: .top).combined(with: .opacity))
                }

            default:
                EmptyView()
            }

            if isTransitioning {
                Color.black.ignoresSafeArea()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.35), value: wrapper.state.phase)
        .onAppear { HapticManager.shared.prepare() }
    }
}

// MARK: - Menu

struct MenuView: View {
    let state: GameState
    let onStartGame: () -> Void
    @State private var pulse = false
    @State private var floatOffset: CGFloat = 0

    // Emojis decorativos de fondo
    let bgEmojis = ["🐶","🐱","🦊","🐸","🦋","🐝","🦁","🐼","🦄","🐙",
                    "🦀","🐬","🦅","🌺","⭐","🔥","🌈","🎯","🏆","💎"]

    var body: some View {
        ZStack {
            // Fondo gradiente
            LinearGradient(
                colors: [Color(hex: "0D0D1A"), Color(hex: "1A1A2E"), Color(hex: "0F3460")],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            // Emojis decorativos de fondo
            GeometryReader { geo in
                ForEach(Array(bgEmojis.enumerated()), id: \.offset) { index, emoji in
                    Text(emoji)
                        .font(.system(size: 28))
                        .opacity(0.08)
                        .position(
                            x: CGFloat((index * 137) % Int(geo.size.width)),
                            y: CGFloat((index * 89) % Int(geo.size.height))
                        )
                }
            }

            VStack(spacing: 32) {
                Spacer()

                // Logo
                VStack(spacing: 8) {
                    Text("🔍")
                        .font(.system(size: 72))
                        .offset(y: floatOffset)
                        .animation(
                            .easeInOut(duration: 2).repeatForever(autoreverses: true),
                            value: floatOffset
                        )
                    Text("VISTO")
                        .font(.system(size: 56, weight: .black))
                        .foregroundStyle(.white)
                        .tracking(8)
                    Text("Encuentra · Compite · Gana")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color(hex: "FFD700"))
                        .tracking(2)
                }

                // Stats si hay partida previa
                if state.foundCount > 0 {
                    HStack(spacing: 20) {
                        StatBadge(value: "\(state.score)", label: "puntos", emoji: "⭐")
                        StatBadge(value: "\(state.foundCount)", label: "hallados", emoji: "🎯")
                    }
                    .transition(.scale.combined(with: .opacity))
                }

                Spacer()

                // Botón jugar
                Button(action: onStartGame) {
                    HStack(spacing: 12) {
                        Text("¡JUGAR!")
                            .font(.system(size: 20, weight: .black))
                            .tracking(2)
                        Text("▶")
                            .font(.system(size: 18, weight: .bold))
                    }
                    .foregroundStyle(Color(hex: "1A1A2E"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 58)
                    .background(Color(hex: "FFD700"))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: Color(hex: "FFD700").opacity(0.4), radius: 12, y: 4)
                }
                .padding(.horizontal, 40)
                .scaleEffect(pulse ? 1.02 : 1.0)
                .animation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true), value: pulse)

                Text("Combo x3 · x5 · x7 para ganar power-ups")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "666688"))
                    .padding(.bottom, 40)
            }
        }
        .onAppear {
            pulse = true
            floatOffset = -10
        }
    }
}

struct StatBadge: View {
    let value: String
    let label: String
    let emoji: String

    var body: some View {
        VStack(spacing: 4) {
            Text(emoji).font(.system(size: 20))
            Text(value)
                .font(.system(size: 22, weight: .black))
                .foregroundStyle(.white)
            Text(label)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(Color(hex: "AAAAAA"))
                .tracking(1)
        }
        .frame(width: 90, height: 80)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
    }
}
// MARK: - Game

struct GameView: View {
    let state: GameState
    let onItemTap: (Int) -> Void
    let onUsePowerUp: (String) -> Void

    @State private var lastFoundId: Int? = nil
    @State private var flashOpacity: Double = 0
    @State private var showWrongFlash = false
    @State private var prevFoundCount: Int = 0
    @State private var prevWrongCount: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            HUDView(state: state)
            ZStack {
                BoardGrid(
                    state: state,
                    lastFoundId: lastFoundId,
                    showWrongFlash: showWrongFlash,
                    onItemTap: { itemId in
                        let isTarget = state.activeTargets.contains { $0.id == Int32(itemId) }
                        if isTarget { lastFoundId = itemId }
                        onItemTap(itemId)
                    }
                )
                VStack {
                    ComboOverlay(combo: Int(state.combo))
                        .padding(.top, 8)
                    Spacer()
                }
            }
            PowerUpsBar(state: state, onUsePowerUp: onUsePowerUp)
            TargetsBar(targets: state.activeTargets, foundCount: Int(state.foundCount))
        }
        .background(Color(hex: "FFFBF0"))
        .onChange(of: state.foundCount) { _, newVal in
            if newVal > prevFoundCount {
                prevFoundCount = Int(newVal)
                HapticManager.shared.correctTap()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    lastFoundId = nil
                }
            }
        }
        .onChange(of: state.wrongTapCount) { _, newVal in
            if newVal > prevWrongCount {
                prevWrongCount = Int(newVal)
                HapticManager.shared.wrongTap()
                withAnimation(.easeIn(duration: 0.08)) { showWrongFlash = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
                    withAnimation(.easeOut(duration: 0.25)) { showWrongFlash = false }
                }
            }
        }
    }
}

// MARK: - HUD

struct HUDView: View {
    let state: GameState

    var timerColor: Color {
        switch state.timeRemainingMs {
        case 0..<10_000: return Color(hex: "E53935")
        case 10_000..<20_000: return Color(hex: "FB8C00")
        default: return Color(hex: "43A047")
        }
    }

    var timeRatio: Double { Double(state.timeRemainingMs) / 60_000.0 }
    var isWinning: Bool { state.score >= state.ghostSnapshot.currentScore }
    var scoreDiff: Int { Int(state.score) - Int(state.ghostSnapshot.currentScore) }

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text("⏱ \(state.timeRemainingMs / 1000)s")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(timerColor)
                    .contentTransition(.numericText())
                    .animation(.default, value: state.timeRemainingMs / 1000)
                Spacer()
                Text("⭐ \(state.score)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.black)
                    .contentTransition(.numericText())
                    .animation(.default, value: state.score)
                Spacer()
                Text("🔥 x\(state.combo)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(state.combo >= 3 ? Color(hex: "FF6B00") : .secondary)
                    .scaleEffect(state.combo >= 2 ? 1.2 : 1.0)
                    .animation(.spring(response: 0.2, dampingFraction: 0.4), value: state.combo)
            }

            HStack {
                HStack(spacing: 6) {
                    Text(isWinning ? "👑" : "💀").font(.system(size: 14))
                    Text("Rival")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(isWinning ? Color(hex: "2E7D32") : Color(hex: "C62828"))
                    Text("· \(state.ghostSnapshot.foundCount) obj")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }
                Spacer()
                HStack(spacing: 4) {
                    Text("⭐ \(state.ghostSnapshot.currentScore)")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(isWinning ? Color(hex: "2E7D32") : Color(hex: "C62828"))
                        .contentTransition(.numericText())
                        .animation(.default, value: state.ghostSnapshot.currentScore)
                    let diffText = scoreDiff >= 0 ? "+\(scoreDiff)" : "\(scoreDiff)"
                    Text("(\(diffText))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(isWinning ? Color(hex: "43A047") : Color(hex: "E53935"))
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(isWinning ? Color(hex: "E8F5E9") : Color(hex: "FFEBEE"))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .animation(.easeInOut(duration: 0.3), value: isWinning)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4).fill(Color(.systemGray5)).frame(height: 5)
                    RoundedRectangle(cornerRadius: 4).fill(timerColor)
                        .frame(width: geo.size.width * timeRatio, height: 5)
                        .animation(.linear(duration: 0.1), value: timeRatio)
                }
            }
            .frame(height: 5)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.white)
    }
}

// MARK: - Board Grid

struct BoardGrid: View {
    let state: GameState
    let lastFoundId: Int?
    let showWrongFlash: Bool
    let onItemTap: (Int) -> Void
    private let cols = 5

    var body: some View {
        GeometryReader { geo in
            let spacing: CGFloat = 4
            let itemCount = state.board.items.count
            let rowCount = Int(ceil(Double(itemCount) / Double(cols)))
            let cellW = (geo.size.width - spacing * CGFloat(cols + 1)) / CGFloat(cols)
            let cellH = (geo.size.height - spacing * CGFloat(rowCount + 1)) / CGFloat(rowCount)

            VStack(spacing: spacing) {
                ForEach(0..<rowCount, id: \.self) { rowIndex in
                    HStack(spacing: spacing) {
                        ForEach(0..<cols, id: \.self) { colIndex in
                            let itemIndex = rowIndex * cols + colIndex
                            if itemIndex < itemCount {
                                let item = state.board.items[itemIndex]
                                BoardItemView(
                                    item: item,
                                    isJustFound: lastFoundId == Int(item.id),
                                    isRevealed: state.revealedItemId.flatMap { Int($0.int32Value) } == Int(item.id),
                                    showWrongFlash: showWrongFlash,
                                    size: CGSize(width: cellW, height: cellH),
                                    onTap: { onItemTap(Int(item.id)) }
                                )
                            } else {
                                Spacer()
                                    .frame(width: cellW, height: cellH)
                            }
                        }
                    }
                }
            }
            .padding(spacing)
        }
        .background(Color(hex: "FFFBF0"))
    }
}

// MARK: - Board Item

struct BoardItemView: View {
    let item: BoardItem
    let isJustFound: Bool
    let isRevealed: Bool
    let showWrongFlash: Bool
    let size: CGSize
    let onTap: () -> Void

    var cellColor: Color {
        if isJustFound    { return Color(hex: "4CAF50") }
        if isRevealed     { return Color(hex: "FFD700") }
        if showWrongFlash { return Color(hex: "FFCDD2") }
        return Color(hex: "F5F0E8")
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(cellColor)
                .animation(.easeOut(duration: 0.2), value: cellColor)

            Text(item.imageKey)
                .font(.system(size: 26))
                .scaleEffect(isJustFound ? 1.12 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.4), value: isJustFound)
        }
        .frame(width: size.width, height: size.height)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(hex: "FFD700"), lineWidth: isRevealed ? 2 : 0)
                .animation(
                    isRevealed
                    ? .easeInOut(duration: 0.4).repeatForever(autoreverses: true)
                    : .easeOut(duration: 0.1),
                    value: isRevealed
                )
        )
        .onTapGesture { onTap() }
    }
}

// MARK: - Power-Ups Bar

struct PowerUpsBar: View {
    let state: GameState
    let onUsePowerUp: (String) -> Void

    let allPowerUps: [(id: String, emoji: String, label: String)] = [
        ("reveal", "🔍", "Revelar"),
        ("double_points", "⭐", "x2"),
        ("freeze_time", "❄️", "Congelar"),
        ("shuffle", "🔀", "Mezclar"),
        ("combo_shield", "🛡️", "Escudo")
    ]

    var availablePowerUps: [(id: String, emoji: String, label: String)] {
        allPowerUps.filter { state.chargesFor(powerUpId: $0.id) > 0 }
    }

    var body: some View {
        if availablePowerUps.isEmpty { return AnyView(EmptyView()) }

        return AnyView(
            HStack(spacing: 8) {
                ForEach(availablePowerUps, id: \.id) { pu in
                    let charges = Int(state.chargesFor(powerUpId: pu.id))
                    let isActive = state.activePowerUps.contains { $0.powerUpId == pu.id } ||
                    (pu.id == "combo_shield" && state.hasComboShield)

                    PowerUpButton(
                        emoji: pu.emoji,
                        label: pu.label,
                        charges: charges,
                        isActive: isActive,
                        onTap: { onUsePowerUp(pu.id) }
                    )
                }
            }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(hex: "F0F0F0"))
        )
    }
}

struct PowerUpButton: View {
    let emoji: String
    let label: String
    let charges: Int
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 2) {
                Text(emoji).font(.system(size: 22))
                Text(label)
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(.white)
                HStack(spacing: 2) {
                    ForEach(0..<charges, id: \.self) { _ in
                        Circle()
                            .fill(Color(hex: "FFD700"))
                            .frame(width: 5, height: 5)
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isActive ? Color(hex: "1A1A2E") : Color(hex: "2A2A3E"))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isActive ? Color(hex: "FFD700") : Color.clear, lineWidth: 2)
            )
            .scaleEffect(isActive ? 1.08 : 1.0)
            .animation(.spring(response: 0.2, dampingFraction: 0.4), value: isActive)
        }
    }
}

// MARK: - Targets Bar

struct TargetsBar: View {
    let targets: [BoardItem]
    let foundCount: Int
    @State private var animatedTargets: [BoardItem] = []

    var body: some View {
        VStack(spacing: 0) {
            // Separador superior
            Rectangle()
                .fill(Color(hex: "FFD700").opacity(0.3))
                .frame(height: 1)

            HStack(spacing: 0) {
                // Contador izquierda
                VStack(spacing: 2) {
                    Text("\(foundCount)")
                        .font(.system(size: 24, weight: .black))
                        .foregroundStyle(Color(hex: "FFD700"))
                        .contentTransition(.numericText())
                        .animation(.default, value: foundCount)
                    Text("hallados")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(Color(hex: "AAAAAA"))
                        .tracking(0.5)
                }
                .frame(width: 64)

                // Separador
                Rectangle()
                    .fill(Color(hex: "FFFFFF").opacity(0.1))
                    .frame(width: 1, height: 50)

                // Objetivos
                HStack(spacing: 10) {
                    ForEach(targets, id: \.id) { target in
                        TargetCard(item: target)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 12)
            }
            .padding(.vertical, 8)
        }
        .background(Color(hex: "1A1A2E"))
    }
}

struct TargetCard: View {
    let item: BoardItem
    @State private var appeared = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14)
                .fill(Color(hex: "0F3460"))
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color(hex: "FFD700"), lineWidth: 1.5)
            Text(item.imageKey)
                .font(.system(size: 28))
        }
        .frame(width: 52, height: 52)
        .scaleEffect(appeared ? 1 : 0.5)
        .opacity(appeared ? 1 : 0)
        .onAppear {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                appeared = true
            }
        }
    }
}

// MARK: - Color extension

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB,
                  red: Double(r) / 255,
                  green: Double(g) / 255,
                  blue: Double(b) / 255,
                  opacity: Double(a) / 255)
    }
}
