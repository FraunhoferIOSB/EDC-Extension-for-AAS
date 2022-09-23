plugins {
    `java-library`
}

val rsApi = "3.1.0"
val okHttpVersion = "4.10.0"
val javaVersion = 11

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
    implementation("io.admin-shell.aas:dataformat-json:1.2.0")
    // AAS model by admin-shell.io, used for parsing submodelElementCollections
    implementation("io.admin-shell.aas:model:1.2.0")

    // EDC asset management
    implementation("$edcGroup:data-management-api:$edcVersion")

    // Send HTTP requests to AAS service
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    // HTTP endpoint of extension
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}

repositories {
	mavenLocal()
	mavenCentral()
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