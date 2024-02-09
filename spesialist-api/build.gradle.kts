import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

val testcontainersVersion = "1.19.5"
val graphQLKotlinVersion = "7.0.2"
val ktorVersion = "2.3.8"

plugins {
    id("com.expediagroup.graphql") version "7.0.2"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    api("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation(project(":spesialist-felles"))
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}

val graphqlDir = "${project.projectDir}/src/main/resources/graphql"

graphql {
    client {
        // hvis endpoint settes her vil spleis introspectes hver gang man bygger
        schemaFile = file("$graphqlDir/schema.graphql")
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
