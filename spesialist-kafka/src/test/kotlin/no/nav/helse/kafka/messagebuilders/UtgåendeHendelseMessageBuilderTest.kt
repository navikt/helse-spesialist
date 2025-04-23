package no.nav.helse.kafka.messagebuilders

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlernavn
import no.nav.helse.spesialist.domain.testfixtures.lagTilfeldigSaksbehandlerepost
import no.nav.helse.spesialist.kafka.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class UtgåendeHendelseMessageBuilderTest {
    private val fødselsnummer = lagFødselsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val periodetype = Periodetype.FØRSTEGANGSBEHANDLING
    private val saksbehandlerIdent = lagSaksbehandlerident()
    private val saksbehandlerEpost = lagEpostadresseFraFulltNavn(lagSaksbehandlernavn())
    private val beslutterIdent = lagSaksbehandlerident()
    private val beslutterEpost = lagEpostadresseFraFulltNavn(lagSaksbehandlernavn())
    private fun UtgåendeHendelse.somJson() = this.somJsonMessage(fødselsnummer).toJson()

    @Test
    fun `VedtaksperiodeGodkjentManuelt-hendelse uten beslutter`() {
        val hendelse = VedtaksperiodeGodkjentManuelt(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            beslutterIdent = null,
            beslutterEpost = null
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_godkjent",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "automatiskBehandling" to false,
                "behandlingId" to behandlingId,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandlerIdent,
                    "epostadresse" to saksbehandlerEpost,
                )
            )
        )
    }

    @Test
    fun `VedtaksperiodeGodkjentManuelt-hendelse med beslutter`() {
        val hendelse = VedtaksperiodeGodkjentManuelt(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            beslutterIdent = beslutterIdent,
            beslutterEpost = beslutterEpost
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_godkjent",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "automatiskBehandling" to false,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandlerIdent,
                    "epostadresse" to saksbehandlerEpost,
                ),
                "beslutter" to mapOf(
                    "ident" to beslutterIdent,
                    "epostadresse" to beslutterEpost,
                ),
                "behandlingId" to behandlingId,
            )
        )
    }

    @Test
    fun `VedtaksperiodeAvvistManuelt-hendelse`() {
        val hendelse = VedtaksperiodeAvvistManuelt(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            årsak = "En årsak",
            begrunnelser = listOf("En begrunnelse"),
            kommentar = "En kommentar",
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_avvist",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "automatiskBehandling" to false,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandlerIdent,
                    "epostadresse" to saksbehandlerEpost,
                ),
                "behandlingId" to behandlingId,
                "årsak" to "En årsak",
                "begrunnelser" to listOf("En begrunnelse"),
                "kommentar" to "En kommentar",
            )
        )
    }

    @Test
    fun `VedtaksperiodeAvvistManuelt-hendelse uten årsak, kommentar, begrunnelser`() {
        val hendelse = VedtaksperiodeAvvistManuelt(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_avvist",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "automatiskBehandling" to false,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandlerIdent,
                    "epostadresse" to saksbehandlerEpost,
                ),
                "behandlingId" to behandlingId,
            )
        )
    }

    @Test
    fun `VedtaksperiodeGodkjentAutomatisk-hendelse`() {
        val hendelse = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_godkjent",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to "Automatisk behandlet",
                "saksbehandlerEpost" to "tbd@nav.no",
                "automatiskBehandling" to true,
                "saksbehandler" to mapOf(
                    "ident" to "Automatisk behandlet",
                    "epostadresse" to "tbd@nav.no",
                ),
                "behandlingId" to behandlingId,
            )
        )
    }

    @Test
    fun `VedtaksperiodeAvvistAutomatisk-hendelse`() {
        val hendelse = VedtaksperiodeAvvistAutomatisk(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_avvist",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to "Automatisk behandlet",
                "saksbehandlerEpost" to "tbd@nav.no",
                "automatiskBehandling" to true,
                "saksbehandler" to mapOf(
                    "ident" to "Automatisk behandlet",
                    "epostadresse" to "tbd@nav.no",
                ),
                "behandlingId" to behandlingId,
            )
        )
    }

    @Test
    fun `Godkjenningsbehovløsning-hendelse`() {
        val saksbehandlerIdent = lagSaksbehandlerident()
        val saksbehandlerEpost = lagTilfeldigSaksbehandlerepost()
        val saksbehandleroverstyringer = listOf(UUID.randomUUID(), UUID.randomUUID())
        val `godkjenningsbehov@opprettet` = LocalDateTime.now()
        val `godkjenningsbehov@id` = UUID.randomUUID()
        val godkjenttidspunkt = LocalDateTime.now()
        val hendelse = Godkjenningsbehovløsning(
            godkjent = true,
            automatiskBehandling = false,
            godkjenttidspunkt = godkjenttidspunkt,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            refusjonstype = Refusjonstype.INGEN_REFUSJON.name,
            årsak = "En årsak",
            begrunnelser = listOf("En begrunnelse"),
            kommentar = "En kommentar",
            json = """{ "@event_name": "behov", "@opprettet": "$`godkjenningsbehov@opprettet`", "@id": "$`godkjenningsbehov@id`" }""",
        )

        hendelse.somJson().assertGodkjenningsBehovløsning(
            payload = mapOf(
                "godkjent" to true,
                "automatiskBehandling" to false,
                "refusjontype" to "INGEN_REFUSJON",
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "saksbehandleroverstyringer" to saksbehandleroverstyringer,
                "årsak" to "En årsak",
                "begrunnelser" to listOf("En begrunnelse"),
                "kommentar" to "En kommentar",
            ),
            `opprinnelig@opprettet` = `godkjenningsbehov@opprettet`,
            `opprinnelig@id` = `godkjenningsbehov@id`
        )
    }

    @Test
    fun `VedtaksperiodeAvvistAutomatisk-hendelse med årsak, kommentar, begrunnelser`() {
        val hendelse = VedtaksperiodeAvvistAutomatisk(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype.name,
            årsak = "En årsak",
            begrunnelser = listOf("En begrunnelse"),
            kommentar = "En kommentar",
        )

        hendelse.somJson().assertHendelse(
            eventName = "vedtaksperiode_avvist",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to "Automatisk behandlet",
                "saksbehandlerEpost" to "tbd@nav.no",
                "automatiskBehandling" to true,
                "saksbehandler" to mapOf(
                    "ident" to "Automatisk behandlet",
                    "epostadresse" to "tbd@nav.no",
                ),
                "behandlingId" to behandlingId,
                "årsak" to "En årsak",
                "begrunnelser" to listOf("En begrunnelse"),
                "kommentar" to "En kommentar",
            )
        )
    }

    @Test
    fun `Inntektsendringer-hendelse`() {
        // Given:
        val arbeidsgiver1 = lagOrganisasjonsnummer()
        val arbeidsgiver2 = lagOrganisasjonsnummer()
        val hendelse = InntektsendringerEvent(
            inntektskilder = listOf(
                InntektsendringerEvent.Inntektskilde(
                    inntektskilde = arbeidsgiver1,
                    inntekter = listOf(
                        InntektsendringerEvent.Inntektskilde.Inntekt(1 jan 2018, 31 jan 2018, BigDecimal("1000.0")),
                        InntektsendringerEvent.Inntektskilde.Inntekt(1 feb 2018, 28 feb 2018, BigDecimal("2000.0"))
                    ),
                    nullstill = emptyList()
                ),
                InntektsendringerEvent.Inntektskilde(
                    inntektskilde = arbeidsgiver2,
                    inntekter = listOf(
                        InntektsendringerEvent.Inntektskilde.Inntekt(1 jan 2018, 31 jan 2018, BigDecimal("1500.0")),
                    ),
                    nullstill = listOf(
                        InntektsendringerEvent.Inntektskilde.Nullstilling(1 jan 2018, 31 jan 2018),
                        InntektsendringerEvent.Inntektskilde.Nullstilling(1 feb 2018, 28 feb 2018),
                    )
                ),
            ),
        )

        // When:
        val json = hendelse.somJson()

        // Then:
        json.assertHendelse(
            eventName = "inntektsendringer",
            payload = mapOf(
                "fødselsnummer" to fødselsnummer,
                "inntektskilder" to listOf(
                    mapOf(
                        "inntektskilde" to arbeidsgiver1,
                        "inntekter" to listOf(
                            mapOf(
                                "fom" to "2018-01-01",
                                "tom" to "2018-01-31",
                                "dagsbeløp" to 1000.0
                            ),
                            mapOf(
                                "fom" to "2018-02-01",
                                "tom" to "2018-02-28",
                                "dagsbeløp" to 2000.0
                            ),
                        ),
                        "nullstill" to emptyList<Map<String, Any>>()
                    ),
                    mapOf(
                        "inntektskilde" to arbeidsgiver2,
                        "inntekter" to listOf(
                            mapOf(
                                "fom" to "2018-01-01",
                                "tom" to "2018-01-31",
                                "dagsbeløp" to 1500.0
                            ),
                        ),
                        "nullstill" to listOf(
                            mapOf(
                                "fom" to "2018-01-01",
                                "tom" to "2018-01-31",
                            ),
                            mapOf(
                                "fom" to "2018-02-01",
                                "tom" to "2018-02-28",
                            ),
                        )
                    ),
                )
            )
        )
        assertValidByJsonSchema(json, "/inntektsendringer.schema.json")
    }

    private fun String.assertHendelse(eventName: String, payload: Map<String, Any>) {
        val forventedeStandardfelter = mapOf(
            "@event_name" to eventName,
            "fødselsnummer" to fødselsnummer,
        )
        val jsonNode = objectMapper.readTree(this) as ObjectNode
        jsonNode.remove("@id")
        jsonNode.remove("@opprettet")
        jsonNode.remove("system_read_count")
        jsonNode.remove("system_participating_services")
        assertEquals(objectMapper.valueToTree(forventedeStandardfelter + payload), jsonNode)
    }

    private fun assertValidByJsonSchema(json: String, schemaResourceName: String) {
        val schemaJson = javaClass.getResourceAsStream(schemaResourceName)!!.use { inputStream ->
            inputStream.reader().use(InputStreamReader::readText)
        }
        println("Validerer $json")
        val errors = mutableListOf<ValidationError>()
        val valid =
            JsonSchema.fromDefinition(schemaJson).validate(Json.decodeFromString<JsonElement>(json), errors::add)
        assertEquals(0, errors.size) {
            "Valideringsfeil:\n" + errors.joinToString("\n") { "${it.objectPath} - ${it.message} (${it.schemaPath} i skjemaet)" }
        }
        assertTrue(valid)
    }

    private fun String.assertGodkjenningsBehovløsning(
        payload: Map<String, Any>,
        `opprinnelig@id`: UUID,
        `opprinnelig@opprettet`: LocalDateTime
    ) {
        val jsonNode = objectMapper.readTree(this) as ObjectNode
        val løsningNode = objectMapper.readTree(this).get("@løsning")
        assertNotNull(løsningNode)
        assertNotEquals(`opprinnelig@id`, jsonNode["@id"].asUUID())
        assertNotEquals(`opprinnelig@opprettet`, jsonNode["@opprettet"].asLocalDateTime())
        assertEquals("behov", jsonNode.get("@event_name").asText())
        assertEquals(objectMapper.valueToTree(mapOf("Godkjenning" to payload)), løsningNode)
    }
}
