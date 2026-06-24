import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

project.plugins.apply(ApplicationPlugin::class.java)
project.plugins.apply("com.gradleup.shadow")
project.plugins.apply("com.bmuschko.docker-remote-api")

extensions.configure<JavaApplication>("application") {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.named("startScripts") { enabled = false }
tasks.named<Jar>("jar") { enabled = false }
tasks.named("distZip") { enabled = false }
tasks.named("distTar") { enabled = false }

tasks.named("startShadowScripts") { enabled = false }
tasks.named("shadowDistZip") { enabled = false }
tasks.named("shadowDistTar") { enabled = false }

val copyLegalDocs = tasks.register("copyLegalDocs", Copy::class) {
    description = "Copy legal documents into the docker image"
    from(rootProject.projectDir)
    into("build/legal")
    include("LICENSE")
}

val copyDockerfile = tasks.register("copyDockerfile", Copy::class) {
    description = "Copy the docker file into the build directory"
    from(rootProject.projectDir.toPath().resolve("launchers"))
    into(layout.buildDirectory.dir("resources").get().dir("docker"))
    include("Dockerfile")
}

tasks.named<ShadowJar>("shadowJar") {
    setProperty("zip64", true)
    setProperty("archiveFileName", "${project.projectDir.name}.jar")
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles() // merges META-INF/services/**
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    dependsOn(copyDockerfile, copyLegalDocs)
}

tasks.register("dockerize", com.bmuschko.gradle.docker.tasks.image.DockerBuildImage::class) {
    dependsOn(tasks.named("shadowJar"), tasks.named("test"))

    dockerFile.set(File("build/resources/docker/Dockerfile"))
    images.add("${project.name}:${project.version}")
    images.add("${project.name}:latest")

    if (System.getProperty("platform") != null) {
        platform.set(System.getProperty("platform"))
    }

    buildArgs.put("PROJECT", project.projectDir.name)
    buildArgs.put("ADDITIONAL_FILES", "build/legal/*")
    inputDir.set(project.projectDir)
}
