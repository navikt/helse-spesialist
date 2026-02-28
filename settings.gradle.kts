rootProject.name = "spesialist"
include(
    "domain",
    "application",
    "api-schema",
    "api",
    ":clients:entra-id",
    ":clients:krr",
    ":clients:speed",
    ":clients:spillkar",
    ":clients:spiskammerset",
    ":clients:spleis",
    "valkey",
    "db-migrations",
    "db",
    "kafka",
    "opprydding-dev",
    "bootstrap",
    "e2e-tests",
)

// Sett opp repositories basert på om vi kjører i CI eller ikke
// Jf. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
dependencyResolutionManagement {
    // Bare tillat repositories-oppsett her i settings.gradle.kts
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            maven {
                url = uri("https://maven.pkg.github.com/navikt/maven-release")
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
