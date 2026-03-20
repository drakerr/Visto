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
            GameView(state: wrapper.state, onCellTap: { wrapper.selectCell(cellId: $0) })
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
        VStack(spacing: 24) {
            Text("🔍 VISTO")
                .font(.system(size: 52, weight: .black))
                .scaleEffect(pulse ? 1.05 : 1.0)
                .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true), value: pulse)
                .onAppear { pulse = true }

            if state.localPlayer.score > 0 {
                VStack(spacing: 6) {
                    Text("Puntuación: \(state.localPlayer.score)")
                        .font(.title2.bold())
                    Text("Parejas encontradas: \(state.localPlayer.matchesFound)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding()
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            }

            Button(action: onStartGame) {
                Text(state.localPlayer.score > 0 ? "Jugar de nuevo" : "¡Jugar!")
                    .font(.title3.bold())
                    .frame(maxWidth: 200)
                    .padding(.vertical, 14)
                    .background(Color.blue)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
        .padding()
    }
}

// MARK: - Game

struct GameView: View {
    let state: GameState
    let onCellTap: (Int) -> Void

    let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 4)

    var body: some View {
        VStack(spacing: 16) {
            HUDView(state: state)
                .padding(.horizontal)
                .padding(.top)

            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(state.board.cells, id: \.id) { cell in
                    CardView(cell: cell)
                        .onTapGesture {
                            if !cell.isMatched { onCellTap(Int(cell.id)) }
                        }
                }
            }
            .padding(.horizontal)

            Spacer()
        }
    }
}

// MARK: - HUD

struct HUDView: View {
    let state: GameState

    private var timeRatio: Double {
        Double(state.timeRemainingMs) / 60_000.0
    }

    private var timerColor: Color {
        switch state.timeRemainingMs {
        case 0..<10_000: return .red
        case 10_000..<20_000: return .orange
        default: return .green
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                // Timer
                HStack(spacing: 6) {
                    Image(systemName: "timer")
                        .foregroundStyle(timerColor)
                    Text("\(state.timeRemainingMs / 1000)s")
                        .font(.title3.bold())
                        .foregroundStyle(timerColor)
                        .contentTransition(.numericText())
                        .animation(.default, value: state.timeRemainingMs / 1000)
                }

                Spacer()

                // Score
                HStack(spacing: 6) {
                    Image(systemName: "star.fill")
                        .foregroundStyle(.yellow)
                    Text("\(state.localPlayer.score)")
                        .font(.title3.bold())
                        .contentTransition(.numericText())
                        .animation(.default, value: state.localPlayer.score)
                }

                Spacer()

                // Combo
                ComboView(combo: Int(state.localPlayer.combo))
            }

            // Barra de tiempo
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(.systemGray5))
                        .frame(height: 6)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(timerColor)
                        .frame(width: geo.size.width * timeRatio, height: 6)
                        .animation(.linear(duration: 0.1), value: timeRatio)
                }
            }
            .frame(height: 6)
        }
    }
}

// MARK: - Combo

struct ComboView: View {
    let combo: Int
    @State private var animateCombo = false

    var body: some View {
        HStack(spacing: 4) {
            Text("🔥")
            Text("x\(combo)")
                .font(.title3.bold())
                .foregroundStyle(combo >= 3 ? .orange : .secondary)
                .scaleEffect(animateCombo ? 1.4 : 1.0)
        }
        .onChange(of: combo) { _, newVal in
            guard newVal > 1 else { return }
            withAnimation(.spring(response: 0.2, dampingFraction: 0.4)) {
                animateCombo = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                animateCombo = false
            }
        }
    }
}

// MARK: - Card

struct CardView: View {
    let cell: BoardCell
    @State private var rotation: Double = 0
    @State private var showFront = false
    @State private var shakeOffset: Double = 0
    @State private var matchScale = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(.systemGray4))
                .overlay(
                    Text("?")
                        .font(.title.bold())
                        .foregroundStyle(.white)
                )
                .opacity(showFront ? 0 : 1)

            RoundedRectangle(cornerRadius: 10)
                .fill(cell.isMatched ? Color.green.opacity(0.85) : Color.blue.opacity(0.85))
                .overlay(
                    Text(cell.imageKey)
                        .font(.system(size: 32))
                )
                .opacity(showFront ? 1 : 0)
        }
        .aspectRatio(1, contentMode: .fit)
        .scaleEffect(matchScale ? 1.08 : 1.0)
        .offset(x: shakeOffset)
        .rotation3DEffect(.degrees(rotation), axis: (x: 0, y: 1, z: 0), perspective: 0.5)
        .onChange(of: cell.isSelected) { _, selected in
            if selected && !cell.isMatched {
                flipTo(front: true)
            } else if !selected && !cell.isMatched {
                flipTo(front: false)
            }
        }
        .onChange(of: cell.isMatched) { _, matched in
            if matched {
                flipTo(front: true)
                withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
                    matchScale = true
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    withAnimation { matchScale = false }
                }
            }
        }
    }

    private func flipTo(front: Bool) {
        withAnimation(.easeIn(duration: 0.15)) {
            rotation = front ? -90 : 90
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            showFront = front
            rotation = front ? 90 : -90
            withAnimation(.easeOut(duration: 0.15)) {
                rotation = 0
            }
            if !front {
                triggerShake()
            }
        }
    }

    private func triggerShake() {
        withAnimation(.interpolatingSpring(stiffness: 600, damping: 10)) {
            shakeOffset = 8
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
            withAnimation(.interpolatingSpring(stiffness: 600, damping: 15)) {
                shakeOffset = 0
            }
        }
    }
}
