import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

val graphqlKotlinVersion = "8.3.0"

plugins {
    alias(libs.plugins.graphql)
}

dependencies {
    api(project(":spesialist-selve"))

    implementation("com.expediagroup:graphql-kotlin-client:$graphqlKotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion") {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
    }
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinVersion") {
        exclude("com.expediagroup:graphql-kotlin-client-serialization")
    }

    implementation(libs.bundles.logback)
    implementation(libs.bundles.ktor.client)
    implementation(libs.jackson.datatype)
}

val graphqlDir = "${project.projectDir}/src/main/resources/graphql"

graphql {
    client {
        // hvis endpoint settes her vil spleis introspectes hver gang man bygger
        schemaFile = file("$graphqlDir/schema.graphql")
        // Ved å sette opp UUID her vil koden som genereres for spleis-typene bruke UUID
        customScalars =
            listOf(
                GraphQLScalar("UUID", "java.util.UUID", "no.nav.helse.spesialist.client.spleis.converters.UUIDScalarConverter"),
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
    endpoint.set("https://spleis-api.intern.dev.nav.no/graphql/introspection")
}

val copySchemaFile by tasks.registering(Copy::class) {
    from(graphqlIntrospectSchema.outputFile)
    into(graphqlDir)
}

tasks.graphqlIntrospectSchema {
    finalizedBy(copySchemaFile)
}
