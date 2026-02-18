rootProject.name = "spesialist"
include(
    "spesialist-domain",
    "spesialist-application",
    "spesialist-api-schema",
    "spesialist-api",
    "spesialist-client-entra-id",
    "spesialist-client-krr",
    "spesialist-client-spiskammerset",
    "spesialist-client-spleis",
    "spesialist-db-migrations",
    "spesialist-db",
    "spesialist-kafka",
    "spesialist-opprydding-dev",
    "spesialist-bootstrap",
    "spesialist-e2e-tests",
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
