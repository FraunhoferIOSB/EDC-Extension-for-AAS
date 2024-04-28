plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
val faaastVersion: String by project
val edcVersion: String by project
val rsApi: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    // Centralized auth request filter
    implementation(project(":public-api-management"))

    // See this project's README.MD for explanations
    implementation("$group:contract-core:$edcVersion")
    implementation("$group:management-api:$edcVersion")
    implementation("$group:runtime-metamodel:$edcVersion")
    implementation("$group:data-plane-http-spi:$edcVersion") // HttpDataAddress
    implementation("$group:http-spi:$edcVersion")

    implementation("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-dataformat-json:1.0.1")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:1.0.1")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:3.1.6")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}")
    testImplementation("org.mock-server:mockserver-netty:${mockserverVersion}")
}

repositories {
    mavenCentral()
}
tasks.compileJava {options.encoding = "UTF-8"}
tasks.compileTestJava {options.encoding = "UTF-8"}
tasks.test {useJUnitPlatform()}
tasks.jacocoTestReport {dependsOn(tasks.test)}

// FA³ST dependency needs the following
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.module.toString() == "com.google.inject:guice") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
        if (requested.module.toString() == "org.yaml:snakeyaml") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
    }
}