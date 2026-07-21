import org.gradle.api.provider.Property

interface SasDeployableExtension {
    val mainClass: Property<String>

    /** Image-navn (uten registry/tag). Default er `rootProject.name`. */
    val imageName: Property<String>
}
