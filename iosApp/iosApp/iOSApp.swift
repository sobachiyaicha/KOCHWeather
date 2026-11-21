import SwiftUI
import Shared

/// Мост SwiftUI -> Compose Multiplatform.
///
/// Phase 4 debug-only QA-шов: при запуске с `-koch-media-probe` (см.
/// `PlatformVideo.ios.kt` → `mediaProbeViewController()`) показываем полноэкранный
/// PlatformVideo вместо обычной навигации. Без аргумента (и в release) — тот же
/// MainViewController, что и раньше: `mediaProbeViewController()` возвращает nil.
struct ComposeView: UIViewControllerRepresentable {
    private static let probeController = PlatformVideo_iosKt.mediaProbeViewController()

    func makeUIViewController(context: Context) -> UIViewController {
        if let probe = ComposeView.probeController {
            return probe
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct KochWeatherApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
