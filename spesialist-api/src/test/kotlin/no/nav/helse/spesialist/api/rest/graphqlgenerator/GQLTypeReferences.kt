package no.nav.helse.spesialist.api.rest.graphqlgenerator

sealed class GQLTypeReference<INPUT_OR_OUTPUT : GQLType>(val nullable: Boolean) {
    abstract fun toSDLPart(): String
    abstract fun unwrappedType(): INPUT_OR_OUTPUT
    fun toSDL(): String = toSDLPart() + if (nullable) "" else "!"
    override fun toString(): String = toSDL()
}

class GQLListTypeReference<INPUT_OR_OUTPUT : GQLType>(
    val wrappedReference: GQLTypeReference<INPUT_OR_OUTPUT>,
    nullable: Boolean,
) : GQLTypeReference<INPUT_OR_OUTPUT>(nullable) {
    override fun toSDLPart(): String = "[${wrappedReference}]"
    override fun unwrappedType(): INPUT_OR_OUTPUT = wrappedReference.unwrappedType()
}

class GQLDirectTypeReference<INPUT_OR_OUTPUT : GQLType>(
    val type: INPUT_OR_OUTPUT,
    nullable: Boolean,
) : GQLTypeReference<INPUT_OR_OUTPUT>(nullable) {
    override fun toSDLPart(): String = type.name
    override fun unwrappedType(): INPUT_OR_OUTPUT = type
}
