import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.jevhee"
            artifactId = project.name
            version = providers.gradleProperty("VERSION_NAME")
                .orElse("0.1.0-SNAPSHOT")
                .get()

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
