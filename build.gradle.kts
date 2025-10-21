import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    checkstyle
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    `java-test-fixtures`
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
    alias(libs.plugins.edc.build)
}

allprojects {
    apply(plugin = "java-library")
}
subprojects {

    tasks.withType<ShadowJar>().configureEach {
        isZip64 = true
        exclude("**/pom.properties", "**/pom.xm")
        mergeServiceFiles()
        archiveFileName.set("${project.projectDir.name}.jar")
    }

    plugins.withId("application") {
        // Set main class for apps
        extensions.configure<JavaApplication>("application") {
            mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
        }

        // Disable Application plugin tasks
        tasks.named<CreateStartScripts>("startScripts") { enabled = false }
        tasks.named<Jar>("jar") { enabled = false }
        tasks.named<Zip>("distZip") { enabled = false }
        tasks.named<Tar>("distTar") { enabled = false }

        // If Shadow is applied, disable its app-related tasks
        plugins.withId("com.github.johnrengelman.shadow") {
            tasks.named<CreateStartScripts>("startShadowScripts") { enabled = false }
            tasks.named<Zip>("shadowDistZip") { enabled = false }
            tasks.named<Tar>("shadowDistTar") { enabled = false }
        }
    }

    afterEvaluate {
        // the "dockerize" task is added to all projects that use the `shadowJar` plugin
        if (project.plugins.hasPlugin(libs.plugins.shadow.get().pluginId)) {

            val copyLegalDocs = tasks.register<Copy>("copyLegalDocs") {
                from(project.rootProject.projectDir)
                into("build/legal")
                include("LICENSE")
            }

            val copyDockerfile = tasks.register<Copy>("copyDockerfile") {
                from(rootProject.projectDir.toPath().resolve("launchers"))
                into(project.layout.buildDirectory.dir("resources").get().dir("docker"))
                include("Dockerfile")
            }

            val shadowJarTask = tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME).get()

            shadowJarTask
                .dependsOn(copyDockerfile)
                .dependsOn(copyLegalDocs)

            // actually apply the plugin to the (sub-)project
            apply(plugin = libs.plugins.docker.get().pluginId)

            val dockerTask = tasks.register<DockerBuildImage>("dockerize") {
                dockerFile.set(File("build/resources/docker/Dockerfile"))

                val dockerContextDir = project.projectDir
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")

                if (System.getProperty("platform") != null) {
                    platform.set(System.getProperty("platform"))
                }

                buildArgs.put("PROJECT", project.projectDir.name)
                buildArgs.put("ADDITIONAL_FILES", "build/legal/*")
                inputDir.set(file(dockerContextDir))
            }
            dockerTask.get().dependsOn(shadowJarTask)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}