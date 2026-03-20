import SwiftUI
import Shared

// MARK: - Root

struct ContentView: View {
    @StateObject private var wrapper = GameViewModelWrapper()

    var body: some View {
        switch wrapper.state.phase {
        case .countdown, .finished:
            MenuView(state: wrapper.state, onStartGame: { wrapper.startGame() })
        case .playing:
            GameView(state: wrapper.state, onItemTap: { wrapper.selectCell(cellId: $0) })
        default:
            EmptyView()
        }
    }
}

// MARK: - Menu

struct MenuView: View {
    let state: GameState
    let onStartGame: () -> Void
    @State private var pulse = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: "1A1A2E"), Color(hex: "16213E"), Color(hex: "0F3460")],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 24) {
                Text("🔍 VISTO")
                    .font(.system(size: 52, weight: .black))
                    .foregroundStyle(.white)
                    .scaleEffect(pulse ? 1.05 : 1.0)
                    .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true), value: pulse)
                    .onAppear { pulse = true }

                Text("Encuentra los objetos\nantes que tu rival")
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "AAAAAA"))
                    .multilineTextAlignment(.center)

                if state.foundCount > 0 {
                    VStack(spacing: 4) {
                        Text("\(state.score) pts")
                            .font(.system(size: 28, weight: .black))
                            .foregroundStyle(.white)
                        Text("\(state.foundCount) encontrados")
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "AAAAAA"))
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(Color.white.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                Button(action: onStartGame) {
                    Text(state.foundCount > 0 ? "Jugar de nuevo" : "¡Jugar!")
                        .font(.system(size: 18, weight: .bold))
                        .frame(width: 200, height: 52)
                        .background(Color(hex: "E94560"))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
        }
    }
}

// MARK: - Game

struct GameView: View {
    let state: GameState
    let onItemTap: (Int) -> Void

    @State private var lastFoundId: Int? = nil
    @State private var showWrongFlash = false
    @State private var prevFoundCount: Int = 0
    @State private var prevWrongCount: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            HUDView(state: state)
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
            TargetsBar(targets: state.activeTargets, foundCount: Int(state.foundCount))
        }
        .background(Color(hex: "FFFBF0"))
        .onChange(of: state.foundCount) { _, newVal in
            if newVal > prevFoundCount {
                prevFoundCount = Int(newVal)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    lastFoundId = nil
                }
            }
        }
        .onChange(of: state.wrongTapCount) { _, newVal in
            if newVal > prevWrongCount {
                prevWrongCount = Int(newVal)
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

    var timeRatio: Double {
        Double(state.timeRemainingMs) / 60_000.0
    }

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
                    .contentTransition(.numericText())
                    .animation(.default, value: state.score)
                Spacer()
                Text("🔥 x\(state.combo)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(state.combo >= 3 ? Color(hex: "FF6B00") : .secondary)
                    .scaleEffect(state.combo >= 2 ? 1.2 : 1.0)
                    .animation(.spring(response: 0.2, dampingFraction: 0.4), value: state.combo)
            }
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
            let rows = stride(from: 0, to: state.board.items.count, by: cols).map {
                Array(state.board.items[$0..<min($0 + cols, state.board.items.count)])
            }
            let spacing: CGFloat = 4
            let cellW = (geo.size.width - spacing * CGFloat(cols + 1)) / CGFloat(cols)
            let cellH = (geo.size.height - spacing * CGFloat(rows.count + 1)) / CGFloat(rows.count)

            VStack(spacing: spacing) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, rowItems in
                    HStack(spacing: spacing) {
                        ForEach(rowItems, id: \.id) { item in
                            BoardItemView(
                                item: item,
                                isJustFound: lastFoundId == Int(item.id),
                                showWrongFlash: showWrongFlash,
                                size: CGSize(width: cellW, height: cellH),
                                onTap: { onItemTap(Int(item.id)) }
                            )
                        }
                        if rowItems.count < cols {
                            Spacer()
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
    let showWrongFlash: Bool
    let size: CGSize
    let onTap: () -> Void

    var cellColor: Color {
        if isJustFound { return Color(hex: "4CAF50") }
        if showWrongFlash { return Color(hex: "FFCDD2") }
        return Color(hex: "F5F0E8")
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(cellColor)
                .animation(.easeOut(duration: 0.2), value: cellColor == Color(hex: "FFCDD2"))

            Text(item.imageKey)
                .font(.system(size: 26))
                .scaleEffect(isJustFound ? 1.12 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.4), value: isJustFound)
        }
        .frame(width: size.width, height: size.height)
        .onTapGesture { onTap() }
    }
}

// MARK: - Targets Bar

struct TargetsBar: View {
    let targets: [BoardItem]
    let foundCount: Int

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text("🎯 BUSCA:")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "AAAAAA"))
                    .tracking(1)
                Spacer()
                Text("✅ \(foundCount) encontrados")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "4CAF50"))
            }
            HStack(spacing: 8) {
                ForEach(targets, id: \.id) { target in
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: "0F3460"))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(hex: "FFD700"), lineWidth: 1.5)
                            )
                        Text(target.imageKey)
                            .font(.system(size: 30))
                    }
                    .frame(width: 56, height: 56)
                }
                Spacer()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(hex: "1A1A2E"))
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
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
