package no.nav.helse.kafka.message_builders

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.test.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spesialist.test.lagSaksbehandlernavn
import no.nav.helse.spesialist.test.lagTilfeldigSaksbehandlerepost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
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

    private fun String.assertGodkjenningsBehovløsning(payload: Map<String, Any>, `opprinnelig@id`: UUID, `opprinnelig@opprettet`: LocalDateTime) {
        val jsonNode = objectMapper.readTree(this) as ObjectNode
        val løsningNode = objectMapper.readTree(this).get("@løsning")
        assertNotNull(løsningNode)
        assertNotEquals(`opprinnelig@id`, jsonNode["@id"].asUUID())
        assertNotEquals(`opprinnelig@opprettet`, jsonNode["@opprettet"].asLocalDateTime())
        assertEquals("behov", jsonNode.get("@event_name").asText())
        assertEquals(objectMapper.valueToTree(mapOf("Godkjenning" to payload)), løsningNode)
    }
}
