plugins {
    `java-library`
    jacoco
}

val rsApi = "3.1.0"
val okHttpVersion = "4.10.0"
val javaVersion = 11
val mockitoVersion = "4.8.1"
val jupiterVersion = "5.9.1"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}
}

dependencies {
    val edcGroup = "org.eclipse.dataspaceconnector"
    val edcVersion = "0.0.1-milestone-6"
    // FA³ST
    implementation ("de.fraunhofer.iosb.ilt.faaast.service:starter:0.1.0")
    // AAS serializer
    implementation("io.admin-shell.aas:dataformat-json:1.2.1")
    // AAS model by admin-shell.io, used for parsing submodelElementCollections
    implementation("io.admin-shell.aas:model:1.2.0")

    // EDC asset management
    implementation("$edcGroup:data-management-api:$edcVersion")

    // Send HTTP requests to AAS service
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    // HTTP endpoint of extension
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // Tests
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-netty:5.14.0") 
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.11.1") 
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
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