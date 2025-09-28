package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLListOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOrListOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNonNullOutputType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLOutputType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class OutputTypeGenerator(
    private val scalars: ScalarTypeResolver,
    private val enums: EnumTypeGenerator,
    private val typeNameGenerator: TypeNameGenerator,
) {
    private val objects = ObjectTypeGenerator(this, typeNameGenerator)
    val interfaces = InterfaceTypeGenerator(this, typeNameGenerator)
    fun resolveOrGenerate(type: KType): GQLOutputType =
        if (!type.isMarkedNullable) {
            GQLNonNullOutputType(resolveOrGenerateNamedOrListOutputType(type.withNullability(true)))
        } else {
            resolveOrGenerateNamedOrListOutputType(type)
        }

    private fun resolveOrGenerateNamedOrListOutputType(type: KType): GQLNamedOrListOutputType =
        when (val forenklet = type.toCollectionOrKClassType()) {
            is CollectionType -> GQLListOutputType(resolveOrGenerate(forenklet.elementType))
            is KClassType -> resolveOrGenerateNamedOutputType(forenklet.klass)
        }

    fun resolveOrGenerateNamedOutputType(klass: KClass<*>): GQLNamedOutputType =
        if (scalars.isScalar(klass)) {
            scalars.resolve(klass)
        } else if (enums.isEnum(klass)) {
            enums.resolveOrGenerate(klass)
        } else if (interfaces.isInterface(klass)) {
            interfaces.resolveOrGenerate(klass)
        } else {
            objects.resolveOrGenerateObjectType(klass)
        }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        interfaces.lookupGQLType(name)
            ?: objects.lookupGQLType(name)

    fun allTypes() = interfaces.interfaceTypes.values + objects.objectTypes.values
}
