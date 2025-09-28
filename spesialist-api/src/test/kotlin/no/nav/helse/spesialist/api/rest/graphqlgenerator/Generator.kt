package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.GetHåndterer
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RESTHÅNDTERERE
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLListInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLRestMutation
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLRestQuery
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties

class Generator {
    private val typeNameGenerator = TypeNameGenerator(::lookupGQLType)
    val scalars = ScalarTypeResolver()
    val enums = EnumTypeGenerator(typeNameGenerator)
    val outputTypes = OutputTypeGenerator(scalars, enums, typeNameGenerator)
    val inputTypes = InputTypeGenerator(scalars, enums, typeNameGenerator)

    fun allOutputTypes() = outputTypes.allTypes()

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        scalars.lookupGQLType(name)
            ?: enums.lookupGQLType(name)
            ?: outputTypes.lookupGQLType(name)
            ?: inputTypes.lookupGQLType(name)

    fun generate() {
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer output-typer nødvendige for ${håndterer::class.simpleName}...")
            outputTypes.resolveOrGenerate(håndterer.responseBodyType)
        }
        RESTHÅNDTERERE.filterIsInstance<PostHåndterer<*, *, *>>().forEach { håndterer ->
            println("Genererer input-typer nødvendige for ${håndterer::class.simpleName}...")
            inputTypes.resolveOrGenerate(håndterer.requestBodyType)
        }
        RESTHÅNDTERERE.forEach { håndterer ->
            when (håndterer) {
                is GetHåndterer<*, *> -> queries.add(generateQuery(håndterer))
                is PostHåndterer<*, *, *> -> mutations.add(generateMutation(håndterer))
            }
        }
    }

    private fun generateQuery(håndterer: GetHåndterer<*, *>): GQLRestQuery {
        println("Genererer query basert på ${håndterer::class.simpleName}...")
        val håndtererNavn = håndterer::class.simpleName!!.removeSuffix("Håndterer")
        return GQLRestQuery(
            operationName = "REST$håndtererNavn",
            fieldName = "rest$håndtererNavn",
            arguments = håndterer.urlParametersClass.declaredMemberProperties
                .associate { property -> property.name.replaceÆØÅ() to property.returnType.toUrlParameterTypeReference() },
            fieldType = outputTypes.resolveOrGenerate(håndterer.responseBodyType),
            path = "/${håndterer.urlPath.gqlParameterizedPath()}"
        )
    }

    private fun generateMutation(håndterer: PostHåndterer<*, *, *>): GQLRestMutation {
        println("Genererer mutation basert på ${håndterer::class.simpleName}...")
        val håndtererNavn = håndterer::class.simpleName!!.removeSuffix("Håndterer")
        return GQLRestMutation(
            operationName = "REST$håndtererNavn",
            fieldName = "rest$håndtererNavn",
            arguments =
                håndterer.urlParametersClass.declaredMemberProperties
                    .associate { property -> property.name.replaceÆØÅ() to property.returnType.toUrlParameterTypeReference() }
                    .plus(mapOf("input" to inputTypes.resolveOrGenerate(håndterer.requestBodyType))),
            fieldType = outputTypes.resolveOrGenerate(håndterer.responseBodyType),
            path = "/${håndterer.urlPath.gqlParameterizedPath()}"
        )
    }

    val queries = mutableListOf<GQLRestQuery>()
    val mutations = mutableListOf<GQLRestMutation>()

    private fun KType.toUrlParameterTypeReference(): GQLInputType {
        val resolved = inputTypes.resolveOrGenerate(this)
        if (resolved is GQLListInputType) {
            error("Kan ikke ha lister som URL-parametre. Gjelder type $this.")
        }
        return resolved
    }

    private fun String.gqlParameterizedPath(): String =
        split('/').joinToString(separator = "/") {
            if (it.startsWith('{') && it.endsWith('}')) {
                "{args." + it.substring(1, it.length - 1).replaceÆØÅ() + "}"
            } else {
                it
            }
        }

    private fun String.replaceÆØÅ(): String =
        replace("æ", "e")
            .replace("ø", "o")
            .replace("å", "a")
            .replace("Æ", "E")
            .replace("Ø", "O")
            .replace("Å", "A")
}

fun String.assertNoÆØÅ(): String =
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
