package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.GetHåndterer
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RESTHÅNDTERERE
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLCustomScalarType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLEnumType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputObjectType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInterfaceType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLListInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLListOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOrListInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOrListOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNonNullInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNonNullOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLObjectOrInterfaceType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLObjectType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLRestMutation
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLRestQuery
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLScalarType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.scalarTypes
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmName

class Generator {
    fun generate() {
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer output-typer nødvendige for ${håndterer::class.simpleName}...")
            resolveOutputType(håndterer.responseBodyType, ::resolveOrGenerateNamedOutputType)
        }
        RESTHÅNDTERERE.filterIsInstance<PostHåndterer<*, *, *>>().forEach { håndterer ->
            println("Genererer input-typer nødvendige for ${håndterer::class.simpleName}...")
            resolveInputType(håndterer.requestBodyType, ::resolveNamedInputTypeWithGeneration)
        }
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer query / mutation basert på ${håndterer::class.simpleName}...")

            val håndtererNavn = håndterer::class.simpleName!!.removeSuffix("Håndterer")
            val urlParameters =
                håndterer.urlParametersClass.declaredMemberProperties
                    .associate { property -> property.name.replaceÆØÅ() to property.returnType.toUrlParameterTypeReference() }

            val path = "/${håndterer.urlPath.gqlParameterizedPath()}"

            val fieldType = resolveOutputType(håndterer.responseBodyType) {
                resolveNamedOutputType(it) ?: error("Fant ikke respons-typen $it, som skulle vært generert")
            }

            val operationName = "REST$håndtererNavn"
            val fieldName = "rest$håndtererNavn"

            when (håndterer) {
                is GetHåndterer<*, *> -> {
                    queries.add(
                        GQLRestQuery(
                            operationName = operationName,
                            fieldName = fieldName,
                            arguments = urlParameters,
                            fieldType = fieldType,
                            path = path
                        )
                    )
                }

                is PostHåndterer<*, *, *> -> {

                    mutations.add(
                        GQLRestMutation(
                            operationName = operationName,
                            fieldName = fieldName,
                            arguments = urlParameters + mapOf("input" to resolveInputType(håndterer.requestBodyType) {
                                resolveNamedInputType(it)
                                    ?: error("Fant ikke request-type $it, som skulle vært generert")
                            }),
                            fieldType = fieldType,
                            path = path
                        )
                    )
                }
            }
        }
    }

    val enumTypes = mutableMapOf<KClass<*>, GQLEnumType>()
    val inputTypes = mutableMapOf<KClass<*>, GQLInputObjectType>()
    val outputTypes = mutableMapOf<KClass<*>, GQLObjectOrInterfaceType>()
    val queries = mutableListOf<GQLRestQuery>()
    val mutations = mutableListOf<GQLRestMutation>()
    val referencedTypes = mutableSetOf<GQLNamedType>()

    private fun resolveNamedOutputType(klass: KClass<*>): GQLNamedOutputType? =
        resolveScalarType(klass) ?: enumTypes[klass] ?: outputTypes[klass]

    private fun resolveOrGenerateNamedOutputType(klass: KClass<*>): GQLNamedOutputType {
        resolveNamedOutputType(klass)?.let { return it }

        if (klass.java.isEnum) {
            generateEnumType(klass)
        } else {
            println("Genererer output-type for $klass...")
            val superclasses = klass.superclasses.filterNot { it == Any::class }
            val fields = klass.declaredMemberProperties
                .filterNot { it.name == "__typename" }
                .onEach { it.name.assertNoÆØÅ() }
                .associate {
                    it.name to resolveOutputType(
                        type = it.returnType,
                        namedTypeResolver = ::resolveOrGenerateNamedOutputType
                    )
                }
            if (fields.isEmpty()) {
                error(
                    "Alle GraphQL-typer må ha minst ett felt. Hvis dette er en skalarverdi, legg den til i genereringen." +
                            " Gjelder $klass."
                )
            }

            val implementedInterfaces =
                superclasses.map {
                    val generatedInterface = resolveOrGenerateNamedOutputType(it)
                    if (generatedInterface !is GQLInterfaceType) {
                        error(
                            "Støtter ikke ikke-abstrakte superklasser, ettersom de ikke kan bli" +
                                    " til GraphQL interfaces. Gjelder $klass."
                        )
                    }
                    generatedInterface
                }

            if (klass.isAbstract || klass.java.isInterface) {
                if (!klass.isSealed) {
                    error(
                        "Støtter ikke abtrakt klasse / interface som ikke er sealed," +
                                " ettersom vi da ikke vet hvilke undertyper som finnes mtp. query'en." +
                                " Gjelder $klass."
                    )
                }
                if (klass.declaredMemberProperties.none { it.name == "__typename" }) {
                    error(
                        "Må ha et __typename-felt på et interface for å simulere GraphQL sin typeklassifisering." +
                                " Gjelder $klass."
                    )
                }
                GQLInterfaceType(
                    name = getUniqueName(klass, false),
                    implementedInterfaces = implementedInterfaces,
                    fields = fields
                ).also { outputTypes[klass] = it }
                    .also {
                        // Pass på å generere alle undertyper, selv om de ikke er eksplisitt referert til
                        klass.sealedSubclasses.forEach { subclass ->
                            resolveOrGenerateNamedOutputType(subclass).also { referencedTypes += it }
                        }
                    }
            } else {
                GQLObjectType(
                    name = getUniqueName(klass, false),
                    implementedInterfaces = implementedInterfaces,
                    fields = fields
                ).also { outputTypes[klass] = it }
            }
        }

        return resolveNamedOutputType(klass)!!
    }

    private fun generateEnumType(klass: KClass<*>) {
        println("Genererer enum-type for $klass...")
        GQLEnumType(
            name = getUniqueName(klass, false),
            values = klass.java.enumConstants.map { it as Enum<*> }.map { it.name }.toSet()
        ).also { enumTypes[klass] = it }
    }

    private fun resolveOutputType(
        type: KType,
        namedTypeResolver: (KClass<*>) -> GQLNamedOutputType,
    ): GQLOutputType =
        if (!type.isMarkedNullable) {
            GQLNonNullOutputType(
                resolveNamedOrListOutputType(
                    type = type.withNullability(true),
                    namedTypeResolver = namedTypeResolver
                )
            )
        } else {
            resolveNamedOrListOutputType(type, namedTypeResolver)
        }

    private fun resolveNamedOrListOutputType(
        type: KType,
        namedTypeResolver: (KClass<*>) -> GQLNamedOutputType
    ): GQLNamedOrListOutputType =
        when (val forenklet = type.tilForenkletType()) {
            is CollectionForenkletType -> GQLListOutputType(
                itemType = resolveOutputType(
                    forenklet.elementType,
                    namedTypeResolver
                )
            )

            is KClassForenkletType ->
                namedTypeResolver(forenklet.klass).also { referencedTypes += it }
        }

    private fun resolveInputType(
        type: KType,
        namedTypeResolver: (KClass<*>) -> GQLNamedInputType,
    ): GQLInputType =
        if (!type.isMarkedNullable) {
            GQLNonNullInputType(
                resolveNamedOrListInputType(
                    type = type.withNullability(true),
                    namedTypeResolver = namedTypeResolver
                )
            )
        } else {
            resolveNamedOrListInputType(type, namedTypeResolver)
        }

    private fun resolveNamedOrListInputType(
        type: KType,
        namedTypeResolver: (KClass<*>) -> GQLNamedInputType
    ): GQLNamedOrListInputType =
        when (val forenklet = type.tilForenkletType()) {
            is CollectionForenkletType -> GQLListInputType(
                itemType = resolveInputType(
                    forenklet.elementType,
                    namedTypeResolver
                )
            )

            is KClassForenkletType ->
                namedTypeResolver(forenklet.klass).also { referencedTypes += it }
        }

    private fun resolveNamedInputType(klass: KClass<*>): GQLNamedInputType? =
        resolveScalarType(klass) ?: enumTypes[klass] ?: inputTypes[klass]

    private fun resolveNamedInputTypeWithGeneration(klass: KClass<*>): GQLNamedInputType {
        resolveNamedInputType(klass)?.let { return it }

        if (klass.java.isEnum) {
            generateEnumType(klass)
        } else {
            println("Genererer input-type for $klass...")

            if (klass.superclasses.filterNot { it == Any::class }.isNotEmpty()) {
                error("Støtter ikke input-typer som arver av andre typer. Gjelder $klass.")
            }
            if (klass.isAbstract || klass.java.isInterface) {
                error("Støtter ikke abstrakt klasse / interface som input. Gjelder $klass.")
            }

            val fields = klass.declaredMemberProperties
                .filterNot { it.name == "__typename" }
                .onEach { it.name.assertNoÆØÅ() }
                .associate {
                    it.name to resolveInputType(
                        type = it.returnType,
                        namedTypeResolver = ::resolveNamedInputTypeWithGeneration
                    )
                }
            if (fields.isEmpty()) {
                error("Alle GraphQL-typer må ha minst ett felt. Gjelder $klass.")
            }

            GQLInputObjectType(
                name = getUniqueName(klass, true),
                fields = fields
            ).also { inputTypes[klass] = it }
        }

        return resolveNamedInputType(klass)!!
    }

    private fun getUniqueName(klass: KClass<*>, isInput: Boolean): String {
        val typeName = buildString {
            append(
                klass.jvmName.substringAfterLast('.')
                    .split("$")
                    .joinToString(separator = "") {
                        it.removePrefix("Api")
                            .removeSuffix("Håndterer")
                            .assertNoÆØÅ()
                    }
            )
            if (isInput && outputTypes.containsKey(klass)) {
                println("Klassen $klass benyttes både i output og input, benytter ordet Input som suffiks")
                append("Input")
            }
        }
        val klassForGQLTypeName = (inputTypes + outputTypes + scalarTypes)
            .firstNotNullOfOrNull { (klass, type) -> klass.takeIf { type.name == typeName } }
        if (klassForGQLTypeName != null) {
            error("Typenavnet $typeName er allerede tatt av en annen klasse ($klassForGQLTypeName")
        }
        return typeName
    }

    private fun KType.toUrlParameterTypeReference(): GQLInputType {
        val resolved = resolveInputType(this) {
            resolveScalarType(it) ?: error("URL-parameter må være en skalartype. Gjelder type $this.")
        }
        if (resolved is GQLListInputType) {
            error("Kan ikke ha lister som URL-parametre. Gjelder type $this.")
        }
        return resolved
    }

    private fun resolveScalarType(klass: KClass<*>): GQLScalarType? = scalarTypes[klass]

    private fun String.gqlParameterizedPath(): String =
        split('/').joinToString(separator = "/") {
            if (it.startsWith('{') && it.endsWith('}')) {
                "{args." + it.substring(1, it.length - 1).gqlUrlPropertyName() + "}"
            } else {
                it
            }
        }

    private fun String.gqlUrlPropertyName(): String =
        replaceÆØÅ()

    private fun String.replaceÆØÅ(): String =
        replace("æ", "e")
            .replace("ø", "o")
            .replace("å", "a")
            .replace("Æ", "E")
            .replace("Ø", "O")
            .replace("Å", "A")


    private fun String.assertNoÆØÅ(): String =
        apply {
            check(none { it in setOf('æ', 'ø', 'å', 'Æ', 'Ø', 'Å') }) {
                error(
                    "Det er et norsk tegn i \"$this\", det fungerer dessverre fortsatt ikke" +
                            " siden GraphQL-standarden ikke støtter det." +
                            " Unntaket er for URL-parametre siden navnet på dem ikke trenger å matche i frontend" +
                            " (vi erstatter bare æøå der ved generering)."
                )
            }
        }

    fun getReferencedCustomScalarTypes(): Collection<GQLCustomScalarType> =
        scalarTypes.values.filterIsInstance<GQLCustomScalarType>().filter { it in referencedTypes }
}
