import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    alias(libs.plugins.graphql)
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.bundles.graphql.ktor.client)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
}

val graphqlDir = "${project.projectDir}/src/main/resources/graphql"

graphql {
    client {
        // hvis endpoint settes her vil spleis introspectes hver gang man bygger
        schemaFile = file("$graphqlDir/schema.graphql")
        // Ved å sette opp UUID her vil koden som genereres for spleis-typene bruke UUID
        customScalars =
            listOf(
                GraphQLScalar(
                    "UUID",
                    "java.util.UUID",
                    "no.nav.helse.spesialist.client.spleis.converters.UUIDScalarConverter",
                ),
                GraphQLScalar(
                    "LocalDateTime",
                    "java.time.LocalDateTime",
                    "no.nav.helse.spesialist.client.spleis.converters.LocalDateTimeScalarConverter",
                ),
                GraphQLScalar(
                    "LocalDate",
                    "java.time.LocalDate",
                    "no.nav.helse.spesialist.client.spleis.converters.LocalDateScalarConverter",
                ),
                GraphQLScalar(
                    "YearMonth",
                    "java.time.YearMonth",
                    "no.nav.helse.spesialist.client.spleis.converters.YearMonthScalarConverter",
                ),
            )
        queryFileDirectory = graphqlDir
        packageName = "no.nav.helse.spleis.graphql"
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON
    }
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.intern.dev.nav.no/graphql")
}

val copySchemaFile by tasks.registering(Copy::class) {
    from(graphqlIntrospectSchema.outputFile)
    into(graphqlDir)
}

tasks.graphqlIntrospectSchema {
    finalizedBy(copySchemaFile)
}
