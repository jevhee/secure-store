import org.gradle.api.GradleException
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension

plugins {
    `maven-publish`
    signing
}

val publicationGroup = providers.gradleProperty("GROUP")
    .orElse("io.github.jevhee")
val publicationArtifact = providers.gradleProperty("POM_ARTIFACT_ID")
    .orElse(project.name)
val publicationVersion = providers.gradleProperty("VERSION_NAME")
    .orElse("0.1.0-SNAPSHOT")

val repositoryUrl = providers.environmentVariable("MAVEN_REPOSITORY_URL")
    .orElse(providers.gradleProperty("mavenRepositoryUrl"))
val repositoryUsername = providers.environmentVariable("MAVEN_REPOSITORY_USERNAME")
    .orElse(providers.gradleProperty("mavenRepositoryUsername"))
val repositoryPassword = providers.environmentVariable("MAVEN_REPOSITORY_PASSWORD")
    .orElse(providers.gradleProperty("mavenRepositoryPassword"))

val signingKey = providers.environmentVariable("SIGNING_KEY")
    .orElse(providers.gradleProperty("signingKey"))
val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
    .orElse(providers.gradleProperty("signingPassword"))
val signingRequired = providers.environmentVariable("SIGNING_REQUIRED")
    .orElse(providers.gradleProperty("signingRequired"))
    .map(String::toBooleanStrict)
    .orElse(false)

extensions.configure<PublishingExtension> {
    repositories {
        if (repositoryUrl.isPresent) {
            maven {
                name = "remote"
                url = uri(repositoryUrl.get())

                if (repositoryUsername.isPresent || repositoryPassword.isPresent) {
                    credentials(PasswordCredentials::class) {
                        username = repositoryUsername.orNull
                        password = repositoryPassword.orNull
                    }
                }
            }
        }
    }
}

afterEvaluate {
    val publication = extensions.getByType<PublishingExtension>()
        .publications
        .create<MavenPublication>("release") {
            groupId = publicationGroup.get()
            artifactId = publicationArtifact.get()
            version = publicationVersion.get()
            from(components.getByName("release"))

            pom {
                name.set("SecureStore")
                description.set("Kotlin-first encrypted key-value storage for Android.")

                providers.gradleProperty("POM_URL").orNull?.let(url::set)

                val licenseName = providers.gradleProperty("POM_LICENSE_NAME")
                    .orElse("Apache License, Version 2.0")
                val licenseUrl = providers.gradleProperty("POM_LICENSE_URL")
                    .orElse("https://www.apache.org/licenses/LICENSE-2.0.txt")
                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                        distribution.set("repo")
                    }
                }

                val developerId = providers.gradleProperty("POM_DEVELOPER_ID").orNull
                val developerName = providers.gradleProperty("POM_DEVELOPER_NAME").orNull
                if (developerId != null || developerName != null) {
                    developers {
                        developer {
                            id.set(developerId)
                            name.set(developerName)
                        }
                    }
                }

                val scmUrl = providers.gradleProperty("POM_SCM_URL").orNull
                if (scmUrl != null) {
                    scm {
                        url.set(scmUrl)
                        connection.set(providers.gradleProperty("POM_SCM_CONNECTION").orNull)
                        developerConnection.set(
                            providers.gradleProperty("POM_SCM_DEV_CONNECTION").orNull,
                        )
                    }
                }
            }
        }

    extensions.configure<SigningExtension> {
        isRequired = signingRequired.get()

        if (signingKey.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
            sign(publication)
        } else if (signingRequired.get()) {
            throw GradleException(
                "Signing is required, but SIGNING_KEY/signingKey was not provided.",
            )
        }
    }
}
