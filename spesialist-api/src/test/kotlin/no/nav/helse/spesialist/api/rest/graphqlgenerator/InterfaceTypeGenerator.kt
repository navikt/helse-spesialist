package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInterfaceType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses

class InterfaceTypeGenerator(
    private val outputTypeGenerator: OutputTypeGenerator,
    private val typeNameGenerator: TypeNameGenerator,
) {
    val interfaceTypes = mutableMapOf<KClass<*>, GQLInterfaceType>()

    fun isInterface(klass: KClass<*>): Boolean = klass.isAbstract || klass.java.isInterface

    fun resolveOrGenerate(klass: KClass<*>): GQLInterfaceType =
        interfaceTypes[klass] ?: generateInterfaceType(klass).also { generated ->
            interfaceTypes[klass] = generated
            // Pass på å generere alle undertyper, selv om de ikke er eksplisitt referert til
            klass.sealedSubclasses.forEach { subclass ->
                outputTypeGenerator.resolveOrGenerateNamedOutputType(
                    subclass
                )
            }
        }

    private fun generateInterfaceType(klass: KClass<*>): GQLInterfaceType {
        println("Genererer interface-type for $klass...")
        check(isInterface(klass)) {
            error("$klass er ikke en interface-type")
        }
        check(klass.isSealed) {
            "Støtter ikke abtrakt klasse / interface som ikke er sealed," +
                    " ettersom vi da ikke vet hvilke undertyper som finnes mtp. query'en." +
                    " Gjelder $klass."
        }
        return GQLInterfaceType(
            name = typeNameGenerator.generateGQLTypeName(klass, "Interface"),
            implementedInterfaces = klass.superclasses.filterNot { it == Any::class }
                .map { resolveOrGenerate(it) },
            fields = klass.declaredMemberProperties
                .also { properties ->
                    check(properties.any { it.name == "__typename" }) {
                        "Må ha et __typename-felt på et interface for å simulere GraphQL sin typeklassifisering." +
                                " Gjelder $klass."
                    }
                }
                .filterNot { it.name == "__typename" }
                .onEach { it.name.assertNoÆØÅ() }
                .associate { it.name to outputTypeGenerator.resolveOrGenerate(it.returnType) }
                .ifEmpty {
                    error(
                        "Alle GraphQL-typer må ha minst ett felt. Hvis dette er en skalarverdi, legg den til i genereringen." +
                                " Gjelder $klass."
                    )
                }
        )
    }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        interfaceTypes.entries
            .find { (_, type) -> type.name == name }
            ?.toPair()
}

