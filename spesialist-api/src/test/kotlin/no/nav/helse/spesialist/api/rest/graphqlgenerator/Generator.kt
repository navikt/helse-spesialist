package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.GetHåndterer
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RESTHÅNDTERERE
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmName

class Generator {
    fun generate() {
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer output-typer nødvendige for ${håndterer::class.simpleName}...")
            resolveOutputReferenceWithGeneration(håndterer.responseBodyType)
        }
        RESTHÅNDTERERE.filterIsInstance<PostHåndterer<*, *, *>>().forEach { håndterer ->
            println("Genererer input-typer nødvendige for ${håndterer::class.simpleName}...")
            resolveInputReferenceWithGeneration(håndterer.requestBodyType)
        }
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer query / mutation basert på ${håndterer::class.simpleName}...")

            val håndtererNavn = håndterer::class.simpleName!!.removeSuffix("Håndterer")
            val urlParameters =
                håndterer.urlParametersClass.declaredMemberProperties
                    .associate { property -> property.name.replaceÆØÅ() to property.returnType.toUrlParameterTypeReference() }

            val path = "/${håndterer.urlPath.gqlParameterizedPath()}"

            val fieldType = resolveOutputReferenceWithGeneration(håndterer.responseBodyType)

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
                            arguments = urlParameters,
                            inputArgument = resolveInputReferenceWithGeneration(håndterer.requestBodyType),
                            fieldType = fieldType,
                            path = path
                        )
                    )
                }
            }
        }
    }

    val inputTypes = mutableMapOf<KClass<*>, GQLInputObjectType>()
    val outputTypes = mutableMapOf<KClass<*>, GQLObjectOrInterfaceType>()
    val queries = mutableListOf<GQLRestQuery>()
    val mutations = mutableListOf<GQLRestMutation>()
    val referencedTypes = mutableSetOf<GQLType>()

    private fun resolveOutputReferenceWithGeneration(type: KType): GQLTypeReference<GQLOutputType> =
        resolveReferenceWithGeneration(
            type = type,
            referenceResolver = ::resolveOutputReferenceWithGeneration,
            typeResolver = ::resolveOutputTypeWithGeneration
        )

    private fun resolveOutputType(klass: KClass<*>): GQLOutputType? =
        resolveScalarType(klass) ?: outputTypes[klass]

    private fun resolveOutputReference(type: KType): GQLTypeReference<GQLOutputType>? =
        resolveReference(
            type = type,
            referenceResolver = ::resolveOutputReference,
            typeResolver = ::resolveOutputType
        )

    private fun resolveOutputTypeWithGeneration(klass: KClass<*>): GQLOutputType {
        if (klass.java.isEnum) {
            error("Støtter ikke enum-klasser (ennå). Gjelder $klass.")
        }

        resolveOutputType(klass)?.let { return it }

        println("Genererer output-type for $klass...")
        val superclasses = klass.superclasses.filterNot { it == Any::class }
        val fields = klass.declaredMemberProperties
            .filterNot { it.name == "__typename" }
            .onEach { it.name.assertNoÆØÅ() }
            .associate { it.name to resolveOutputReferenceWithGeneration(it.returnType) }

        val typeName = klass.gqlTypeName()
        val implementedInterfaces =
            superclasses.map {
                val resolveWithGenerationBasedOnClass = resolveOutputTypeWithGeneration(it)
                if (resolveWithGenerationBasedOnClass !is GQLInterfaceType) {
                    error(
                        "Støtter ikke ikke-abstrakte superklasser, ettersom de ikke kan bli" +
                                " til GraphQL interfaces. Gjelder $klass."
                    )
                }
                resolveWithGenerationBasedOnClass
            }

        if (klass.isAbstract || klass.java.isInterface) {
            if (!klass.isSealed) {
                error(
                    "Støtter ikke abtrakt klasse / interface som ikke er sealed," +
                            " ettersom vi da ikke vet hvilke undertyper som finnes mtp. query'en." +
                            " Gjelder $klass."
                )
            }
            GQLInterfaceType(
                name = typeName,
                implementedInterfaces = implementedInterfaces,
                fields = fields
            ).also { outputTypes[klass] = it }
                .also {
                    // Pass på å generere alle undertyper, selv om de ikke er eksplisitt referert til
                    klass.sealedSubclasses.forEach { subclass ->
                        resolveOutputTypeWithGeneration(subclass).also { referencedTypes += it }
                    }
                }
        } else {
            GQLObjectType(
                name = typeName,
                implementedInterfaces = implementedInterfaces,
                fields = fields
            ).also { outputTypes[klass] = it }
        }

        return resolveOutputType(klass)!!
    }

    private fun <INPUT_OR_OUTPUT : GQLType> resolveReference(
        type: KType,
        referenceResolver: (KType) -> GQLTypeReference<INPUT_OR_OUTPUT>?,
        typeResolver: (KClass<*>) -> INPUT_OR_OUTPUT?,
    ): GQLTypeReference<INPUT_OR_OUTPUT>? =
        when (val forenklet = type.tilForenkletType()) {
            is CollectionForenkletType -> referenceResolver(forenklet.elementType)?.let {
                GQLListTypeReference(
                    wrappedReference = it,
                    nullable = type.isMarkedNullable
                )
            }

            is KClassForenkletType -> typeResolver(forenklet.klass)?.also { referencedTypes += it }?.let {
                GQLDirectTypeReference(
                    type = it,
                    nullable = type.isMarkedNullable
                )
            }
        }

    private fun <INPUT_OR_OUTPUT : GQLType> resolveReferenceWithGeneration(
        type: KType,
        referenceResolver: (KType) -> GQLTypeReference<INPUT_OR_OUTPUT>,
        typeResolver: (KClass<*>) -> INPUT_OR_OUTPUT,
    ): GQLTypeReference<INPUT_OR_OUTPUT> =
        when (val forenklet = type.tilForenkletType()) {
            is CollectionForenkletType -> GQLListTypeReference(
                wrappedReference = referenceResolver(forenklet.elementType),
                nullable = type.isMarkedNullable
            )

            is KClassForenkletType -> GQLDirectTypeReference(
                type = typeResolver(forenklet.klass).also { referencedTypes += it },
                nullable = type.isMarkedNullable
            )
        }

    private fun resolveInputReferenceWithGeneration(type: KType): GQLTypeReference<GQLInputType> =
        resolveReferenceWithGeneration(
            type = type,
            referenceResolver = ::resolveInputReferenceWithGeneration,
            typeResolver = ::resolveInputTypeWithGeneration
        )

    private fun resolveToInputType(klass: KClass<*>): GQLInputType? =
        resolveScalarType(klass) ?: inputTypes[klass]

    private fun resolveInputTypeWithGeneration(klass: KClass<*>): GQLInputType {
        if (klass.java.isEnum) {
            error("Støtter ikke enum-klasser (ennå). Gjelder $klass.")
        }

        resolveToInputType(klass)?.let { return it }

        println("Genererer input-type for $klass...")

        if (klass.superclasses.filterNot { it == Any::class }.isNotEmpty()) {
            error("Støtter ikke input-typer som arver av andre typer. Gjelder $klass.")
        }
        if (klass.isAbstract || klass.java.isInterface) {
            error("Støtter ikke abstrakt klasse / interface som input. Gjelder $klass.")
        }

        GQLInputObjectType(
            name = klass.gqlTypeName() + if (outputTypes.containsKey(klass)) "Input" else "",
            fields = klass.declaredMemberProperties
                .filterNot { it.name == "__typename" }
                .onEach { it.name.assertNoÆØÅ() }
                .associate { it.name to resolveInputReferenceWithGeneration(it.returnType) }
        ).also { inputTypes[klass] = it }

        return resolveToInputType(klass)!!
    }

    private fun KType.toUrlParameterTypeReference(): GQLDirectTypeReference<GQLScalarType> {
        when (val forenklet = tilForenkletType()) {
            is CollectionForenkletType -> {
                error("Kan ikke ha lister som URL-parametre. Gjelder type $this.")
            }

            is KClassForenkletType -> {
                val type = resolveScalarType(forenklet.klass)
                    ?: error("URL-parameter må være en skalartype. Gjelder type $this.")
                return GQLDirectTypeReference(
                    type = type,
                    nullable = isMarkedNullable
                )
            }
        }
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

    private fun KClass<*>.gqlTypeName(): String =
        this.jvmName.substringAfterLast('.')
            .split("$")
            .joinToString(separator = "") {
                it.removePrefix("Api")
                    .removeSuffix("Håndterer")
                    .assertNoÆØÅ()
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
