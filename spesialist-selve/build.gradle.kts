import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

val micrometerRegistryPrometheusVersion = "1.13.6"
val logbackEncoderVersion = "8.0"

plugins {
    kotlin("plugin.serialization") version "2.0.20"
    id("com.expediagroup.graphql") version libs.versions.graphql.kotlin
}

dependencies {
    api(project(":spesialist-modell"))

    implementation(libs.bundles.logging)
    implementation(libs.micrometer.prometheus)
}

val graphqlDir = "${project.projectDir}/src/main/resources/graphql"

graphql {
    client {
        // hvis endpoint settes her vil spleis introspectes hver gang man bygger
        schemaFile = file("$graphqlDir/schema.graphql")
        // Ved å sette opp UUID her vil koden som genereres for spleis-typene bruke UUID
        customScalars =
            listOf(
                GraphQLScalar("UUID", "java.util.UUID", "no.nav.helse.spesialist.api.graphql.schema.converter.UUIDScalarConverter"),
                GraphQLScalar(
                    "LocalDateTime",
                    "java.time.LocalDateTime",
                    "no.nav.helse.spesialist.api.graphql.schema.converter.LocalDateTimeScalarConverter",
                ),
                GraphQLScalar(
                    "LocalDate",
                    "java.time.LocalDate",
                    "no.nav.helse.spesialist.api.graphql.schema.converter.LocalDateScalarConverter",
                ),
                GraphQLScalar(
                    "YearMonth",
                    "java.time.YearMonth",
                    "no.nav.helse.spesialist.api.graphql.schema.converter.YearMonthScalarConverter",
                ),
            )
        queryFileDirectory = graphqlDir
        packageName = "no.nav.helse.spleis.graphql"
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON
    }
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.intern.dev.nav.no/graphql/introspection")
}

val copySchemaFile by tasks.registering(Copy::class) {
    from(graphqlIntrospectSchema.outputFile)
    into(graphqlDir)
}

tasks.graphqlIntrospectSchema {
    finalizedBy(copySchemaFile)
}
