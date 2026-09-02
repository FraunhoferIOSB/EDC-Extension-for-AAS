plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven("https://maven.pkg.github.com/FraunhoferIOSB/EDC-Extension-for-AAS") {
            name = "githubPackages"
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: providers.gradleProperty("gpr.user").orNull
                    ?: ""
                password = System.getenv("GITHUB_TOKEN")
                    ?: providers.gradleProperty("gpr.key").orNull
                    ?: ""
            }
        }
    }
}
