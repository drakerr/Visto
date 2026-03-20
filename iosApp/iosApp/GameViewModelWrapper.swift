import Foundation
import Shared

@MainActor
class GameViewModelWrapper: ObservableObject {

    private let viewModel = GameViewModel()

    @Published var state: GameState

    init() {
        self.state = GameState.companion.initial(
            seed: Int64(Date().timeIntervalSince1970 * 1000)
        )
        startObserving()
    }

    private func startObserving() {
        Task { @MainActor [weak self] in
            guard let self else { return }
            try? await viewModel.state.collect(collector: FlowCollector { [weak self] newState in
                guard let self, let newState = newState as? GameState else { return }
                await MainActor.run {
                    self.state = newState
                }
            })
        }
    }
    func startGame() {
        viewModel.startGame(seed: Int64(Date().timeIntervalSince1970 * 1000))
    }

    func selectCell(cellId: Int) {
        viewModel.onAction(action: GameActionTapItem(itemId: Int32(cellId)))
    }

    func resetGame() {
        viewModel.resetGame()
    }

    deinit {
        viewModel.onCleared()
    }
}

class FlowCollector: Kotlinx_coroutines_coreFlowCollector {
    private let handler: (Any?) async -> Void

    init(handler: @escaping (Any?) async -> Void) {
        self.handler = handler
    }

    func emit(value: Any?) async throws {
        await handler(value)
    }
}
