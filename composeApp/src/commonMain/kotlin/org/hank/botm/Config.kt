package org.hank.botm

enum class Environment {
    DEV,
    PROD,
}

object Config {
    // temp manual approach for using local environment
    private var environment: Environment = Environment.DEV

    val baseUrl = when (environment) {
        // 10.0.2.2 is the setup for emulator
        Environment.DEV -> "http://10.0.2.2:8080/"
        Environment.PROD -> "https://botktor-production.up.railway.app/"
    }
}