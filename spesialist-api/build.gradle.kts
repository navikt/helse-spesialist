import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val testcontainersVersion = "1.19.1"
val graphQLKotlinVersion = "7.0.1"
val ktorVersion = "2.3.4"

plugins {
    kotlin("plugin.serialization") version "1.9.0"
    id("com.expediagroup.graphql") version "7.0.1"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.36")
    api("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation(project(":spesialist-felles"))
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}

val graphqlGenerateClient by tasks.getting(GraphQLGenerateClientTask::class) {
    val baseDir = "${project.projectDir}/src/main/resources/graphql"

    serializer.set(com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON)
    packageName.set("no.nav.helse.spleis.graphql")
    schemaFile.set(File("$baseDir/schema.graphql"))
    queryFiles.from(
        listOf(
            File("$baseDir/hentSnapshot.graphql")
        )
    )
}
