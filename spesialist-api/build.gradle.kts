import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val testcontainersVersion = "1.18.3"
val graphQLKotlinVersion = "6.5.3"
val ktorVersion = "2.3.3"

plugins {
    kotlin("plugin.serialization") version "1.9.10"
    id("com.expediagroup.graphql") version "6.5.3"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.31")
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
