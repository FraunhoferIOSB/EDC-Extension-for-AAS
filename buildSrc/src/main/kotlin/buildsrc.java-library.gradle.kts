import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin

project.plugins.apply(JavaLibraryPlugin::class.java)
project.plugins.apply(JacocoPlugin::class.java)

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}
