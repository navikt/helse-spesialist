package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class TypeNameGenerator(
    private val generatedTypeResolver: (gqlTypeName: String) -> Pair<KClass<*>, GQLNamedType>?
) {
    fun generateGQLTypeName(klass: KClass<*>, suffix: String): String {
        val preferredName = klass.jvmName.substringAfterLast('.')
            .split("$")
            .joinToString<String>(separator = "") {
                it.removePrefix("Api")
                    .removeSuffix("Håndterer")
                    .assertNoÆØÅ()
            }
        return selectNameIfUnique(preferredName) { attemptSuffixedName(preferredName, suffix) }
    }

    private fun selectNameIfUnique(name: String, fallback: () -> String): String {
        val existingGeneratedType = generatedTypeResolver(name)
        return if (existingGeneratedType != null) {
            println("Det finnes allerede en generert type som heter $name (en ${existingGeneratedType.second::class.simpleName} basert på ${existingGeneratedType.first})")
            fallback()
        } else {
            name
        }
    }

    private fun attemptSuffixedName(preferredName: String, suffix: String): String {
        val suffixedName = preferredName + suffix
        println("Forsøker med $suffixedName...")
        return selectNameIfUnique(suffixedName) { attemptNumberSuffixedName(preferredName, 2) }
    }

    private fun attemptNumberSuffixedName(preferredName: String, number: Int): String {
        val suffixedName = preferredName + number
        println("Forsøker med $suffixedName...")
        return selectNameIfUnique(suffixedName) { attemptNumberSuffixedName(preferredName, number + 1) }
    }
}
