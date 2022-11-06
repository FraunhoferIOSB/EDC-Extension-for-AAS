plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
val faaastVersion: String by project
val edcGroup: String by project
val edcVersion: String by project
val okHttpVersion: String by project
val rsApi: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}
}

dependencies {
    // FA³ST
    implementation ("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
    // AAS serializer
    implementation("io.admin-shell.aas:dataformat-json:1.2.1")
    // AAS model by admin-shell.io, used for parsing submodelElementCollections
    implementation("io.admin-shell.aas:model:1.2.0")

    // EDC asset management
    implementation("${edcGroup}:data-management-api:${edcVersion}")

    // Send HTTP requests to AAS service
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    // HTTP endpoint of extension
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // Tests
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("${edcGroup}:junit:${edcVersion}")
    
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}") 
    testImplementation("org.mock-server:mockserver-netty:${mockserverVersion}") 
}

repositories {
	mavenLocal()
	mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.module.toString() == "com.google.inject:guice") {
            artifactSelection{
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
        if(requested.module.toString() == "org.yaml:snakeyaml") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
    }
}