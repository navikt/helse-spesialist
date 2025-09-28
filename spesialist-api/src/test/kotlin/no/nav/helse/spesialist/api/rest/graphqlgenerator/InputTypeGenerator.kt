package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputObjectType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLListInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOrListInputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNonNullInputType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class InputTypeGenerator(
    private val scalars: ScalarTypeResolver,
    private val enums: EnumTypeGenerator,
    private val typeNameGenerator: TypeNameGenerator,
) {
    private val inputObjects = InputObjectTypeGenerator(this, typeNameGenerator)

    fun resolveOrGenerate(type: KType): GQLInputType =
        if (!type.isMarkedNullable) {
            GQLNonNullInputType(resolveOrGenerateNamedOrListInputType(type.withNullability(true)))
        } else {
            resolveOrGenerateNamedOrListInputType(type)
        }

    private fun resolveOrGenerateNamedOrListInputType(type: KType): GQLNamedOrListInputType =
        when (val collectionOrKClassType = type.toCollectionOrKClassType()) {
            is CollectionType -> GQLListInputType(resolveOrGenerate(collectionOrKClassType.elementType))
            is KClassType -> resolveOrGenerateNamedInputType(collectionOrKClassType.klass)
        }

    private fun resolveOrGenerateNamedInputType(klass: KClass<*>): GQLNamedInputType =
        if (scalars.isScalar(klass)) {
            scalars.resolve(klass)
        } else if (enums.isEnum(klass)) {
            enums.resolveOrGenerate(klass)
        } else {
            inputObjects.resolveOrGenerate(klass)
        }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        inputObjects.lookupGQLType(name)

    fun allTypes(): Collection<GQLInputObjectType> = inputObjects.inputObjectTypes.values
}
