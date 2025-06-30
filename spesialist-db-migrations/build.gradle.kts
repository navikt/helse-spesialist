dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
}

tasks {
    val fatJar =
        register<Jar>("fatJar") {
            dependsOn.addAll(
                listOf(
                    "compileJava",
                    "compileKotlin",
                    "processResources",
                ),
            )
            archiveClassifier.set("standalone") // Naming the jar
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            manifest { attributes(mapOf("Main-Class" to "no.nav.helse.spesialist.db.migrations.AppKt")) }
            val sourcesMain = sourceSets.main.get()
            val contents =
                configurations.runtimeClasspath.get()
                    .map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output
            from(contents)
        }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}
