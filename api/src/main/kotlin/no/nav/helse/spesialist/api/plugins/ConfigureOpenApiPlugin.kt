package no.nav.helse.spesialist.api.plugins

import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.config.OpenApiPluginConfig
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.config.SchemaOverwriteModule
import io.github.smiley4.schemakenerator.swagger.data.RefType
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

fun OpenApiPluginConfig.configureOpenApiPlugin() {
    pathFilter = { _, url -> url.firstOrNull() == "api" }
    autoDocumentResourcesRoutes = true
    schemas {
        generator =
            SchemaGenerator.kotlinx {
                serializersModule = customSerializersModule
                referencePath = RefType.OPENAPI_SIMPLE
                overwrite(SchemaGenerator.TypeOverwrites.JavaUuid())
                overwrite(SchemaGenerator.TypeOverwrites.Instant())
                overwrite(SchemaGenerator.TypeOverwrites.LocalDateTime())
                overwrite(SchemaGenerator.TypeOverwrites.LocalDate())
                overwrite(
                    object : SchemaOverwriteModule(
                        identifier = BigDecimal::class.qualifiedName!!,
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "bigdecimal"
                            }
                        },
                    ) {},
                )
            }
    }

    security {
        securityScheme("JWT") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
            bearerFormat = "JWT"
        }
        defaultSecuritySchemeNames("JWT")
    }
}
