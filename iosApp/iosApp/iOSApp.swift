import SwiftUI
import Shared

@main
struct KochWeatherApp: App {
    var body: some Scene {
        WindowGroup {
            VStack(spacing: 16) {
                Image(systemName: "cloud.sun.rain.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(.tint)

                Text(AppInfo.shared.name)
                    .font(.largeTitle.bold())

                Text(AppInfo.shared.greeting())
                    .foregroundStyle(.secondary)

                Text(IosPlatform.shared.summary())
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                Text(AppInfo.shared.buildInfo())
                    .font(.caption.monospaced())
                    .foregroundStyle(.tertiary)
            }
            .padding()
        }
    }
}
