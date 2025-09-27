package no.nav.helse.spesialist.api.rest.graphqlgenerator

import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed interface ForenkletType
class CollectionForenkletType(val elementType: KType) : ForenkletType
class KClassForenkletType(val klass: KClass<*>) : ForenkletType

fun KType.tilForenkletType(): ForenkletType {
    if (classifier !is KClass<*>) {
        error(
            "Støtter ikke type som ikke kan refereres til som klasse." +
                    " Gjelder: $this, som har classifier $classifier"
        )
    }
    val classifierKlass = classifier as KClass<*>

    return if (classifierKlass in setOf(List::class, Set::class, Array::class)) {
        if (arguments.size != 1) {
            error(
                "Forventet kun ett typeargument på en collection. " +
                        " Gjelder: $this, som har argumenter $arguments"
            )
        }
        val argumentType = arguments.single().type
            ?: error(
                "Støtter ikke collections med stjerneargument. " +
                        " Gjelder: $this, som har argumenter $arguments"
            )
        CollectionForenkletType(argumentType)
    } else {
        if (arguments.isNotEmpty()) {
            error(
                "Støtter ikke generics utenom kjente collections. " +
                        " Gjelder: $this, som har argumenter $arguments"
            )
        }
        KClassForenkletType(classifierKlass)
    }
}
