package no.nav.helse.spesialist.api

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Jackson skriver discriminatoren i HTTP-svaret (@JsonTypeInfo), mens OpenAPI-spec-en lages fra
 * kotlinx.serialization (@JsonClassDiscriminator og @SerialName). Testen sjekker at de to sier det samme.
 */
@OptIn(ExperimentalSerializationApi::class)
class PolymorfeTyperTest {
    @TestFactory
    fun `discriminator i OpenAPI-spec stemmer med det Jackson skriver`(): List<DynamicTest> {
        val røtter = klasserIPakken("no.nav.helse.spesialist.api").filter { it.java.isAnnotationPresent(JsonTypeInfo::class.java) }
        assertFalse(røtter.isEmpty(), "Fant ingen klasser med @JsonTypeInfo, så testen sjekker ingenting")

        return røtter.map { rot ->
            dynamicTest(rot.java.name) {
                val jsonTypeInfo = rot.java.getAnnotation(JsonTypeInfo::class.java)
                assertEquals(JsonTypeInfo.Id.SIMPLE_NAME, jsonTypeInfo.use, "@JsonTypeInfo.use på ${rot.simpleName}")
                assertEquals(
                    jsonTypeInfo.property,
                    rot.findAnnotation<JsonClassDiscriminator>()?.discriminator,
                    "@JsonClassDiscriminator på ${rot.simpleName} må ha samme property som @JsonTypeInfo",
                )

                val subklasser = konkreteSubklasser(rot)
                assertFalse(subklasser.isEmpty(), "${rot.simpleName} har ingen konkrete subklasser")
                subklasser.forEach { subklasse ->
                    assertEquals(
                        subklasse.simpleName,
                        subklasse.findAnnotation<SerialName>()?.value,
                        "@SerialName på ${subklasse.qualifiedName} må være lik klassenavnet",
                    )
                }
            }
        }
    }

    private fun konkreteSubklasser(klasse: KClass<*>): List<KClass<*>> =
        klasse.sealedSubclasses.flatMap { subklasse ->
            if (subklasse.isSealed || subklasse.isAbstract || subklasse.java.isInterface) {
                konkreteSubklasser(subklasse)
            } else {
                listOf(subklasse)
            }
        }

    private fun klasserIPakken(pakke: String): List<KClass<*>> {
        val sti = pakke.replace('.', '/')
        val classLoader = Thread.currentThread().contextClassLoader
        return classLoader
            .getResources(sti)
            .toList()
            .filter { it.protocol == "file" }
            .flatMap { url ->
                val rot = File(url.toURI())
                rot
                    .walk()
                    .filter { it.isFile && it.extension == "class" }
                    .map { fil ->
                        "$pakke." +
                            fil
                                .relativeTo(rot)
                                .path
                                .removeSuffix(".class")
                                .replace(File.separatorChar, '.')
                    }
            }.map { Class.forName(it, false, classLoader).kotlin }
    }
}
