import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        DiKt.doInitKoinIos()
        print("Koin for iOS initialized!")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}