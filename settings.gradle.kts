rootProject.name = "spesialist"
include(
    "spesialist-domain",
    "spesialist-application",
    "spesialist-api-schema",
    "spesialist-api",
    ":clients:spesialist-client-entra-id",
    ":clients:spesialist-client-krr",
    ":clients:spesialist-client-sparkel-norg",
    ":clients:spesialist-client-sparkel-sykepengeperioder",
    ":clients:spesialist-client-speed",
    ":clients:spesialist-client-sp-forsikring",
    ":clients:spesialist-client-spleis",
    ":clients:spesialist-client-personpseudoid",
    ":clients:spesialist-client-tilgangsmaskinen",
    "spesialist-valkey",
    "spesialist-db-migrations",
    "spesialist-db",
    "spesialist-kafka",
    "spesialist-opprydding-dev",
    "spesialist-bootstrap",
    "spesialist-e2e-tests",
)

pluginManagement {
    repositories {
        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            maven("https://maven.pkg.github.com/navikt/maven-release") {
                credentials {
                    username = "token"
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull!!
                }
            }
        } else {
            maven("https://repo.adeo.no/repository/github-package-registry-navikt/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Bare tillat repositories-oppsett her i settings.gradle.kts
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            maven("https://maven.pkg.github.com/navikt/maven-release") {
                credentials {
                    username = "token"
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull!!
                }
            }
        } else {
            maven("https://repo.adeo.no/repository/github-package-registry-navikt/")
        }
        mavenCentral()
    }
}
