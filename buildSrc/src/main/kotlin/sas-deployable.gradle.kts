plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.cloud.tools.jib")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

kotlin {
    jvmToolchain(25)
}

val sasDeployable = extensions.create<SasDeployableExtension>("sasDeployable")
sasDeployable.imageName.convention(rootProject.name)

jib {
    from {
        image = "europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25"
    }
    container {
        environment = mapOf("TZ" to "Europe/Oslo")
        jvmFlags = listOf("-XX:MaxRAMPercentage=75")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}

// Verdiene fra `sasDeployable`-blokken er først tilgjengelige etter at modulen er evaluert.
afterEvaluate {
    val registry = providers.gradleProperty("image.registry").orNull
    val tag = providers.gradleProperty("image.tag").orNull
    val targetImage = registry?.let { "$it/" }.orEmpty() + sasDeployable.imageName.get() + tag?.let { ":$it" }.orEmpty()
    configure<com.google.cloud.tools.jib.gradle.JibExtension> {
        to {
            image = targetImage
        }
        container {
            mainClass = sasDeployable.mainClass.get()
        }
    }

    // Jib skriver kun _digesten_ (ikke selve image-navnet) til en dokumentert output-fil
    // (`build/jib-image.digest`, se `jib.outputPaths.digest`). Vi kjenner det fulle image-navnet
    // her, så vi skriver det til en `build/jib-image.name`-fil ved siden av digesten. Dermed kan
    // CI finne begge dokumenterte filene selv og sette sammen en full referanse (navn@digest)
    // uten at workflowen trenger å vite hvilke moduler som bygger images eller hva de heter.
    val imageNameFile = layout.buildDirectory.file("jib-image.name")
    tasks.matching { it.name in setOf("jib", "jibDockerBuild", "jibBuildTar") }.configureEach {
        outputs.file(imageNameFile)
        doLast {
            imageNameFile.get().asFile.writeText(targetImage)
        }
    }
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
