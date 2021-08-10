package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

internal class VedtaksperiodeReberegnetE2ETest : AbstractE2ETest() {
    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `avbryter saksbehandling før oppgave er opprettet til saksbehandling`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )

        sendAvbrytSaksbehandling(FØDSELSNUMMER, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "AVBRUTT"
        )
        assertOppgaver(0)
    }

    @Test
    fun `avbryter saksbehandling etter oppgave er opprettet til saksbehandling`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        val godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        sendAvbrytSaksbehandling(FØDSELSNUMMER, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Invalidert)
    }

    @Test
    fun `tildeler andre rundes oppgave til saksbehandler`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        val saksbehandlerOid = UUID.randomUUID()

        vedtaksperiodeTilGodkjenning()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")
        tildelOppgave(saksbehandlerOid)

        sendAvbrytSaksbehandling(FØDSELSNUMMER, VEDTAKSPERIODE_ID)
        testRapid.reset()
        vedtaksperiodeTilGodkjenning()

        assertEquals(saksbehandlerOid, finnOidForTildeling(OPPGAVEID))
    }

    @Test
    fun `avbryter kommandokjede ved reberegning og oppretter oppgave hos saksbehandler andre runde`() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        var godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )

        sendAvbrytSaksbehandling(FØDSELSNUMMER, VEDTAKSPERIODE_ID)

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "AVBRUTT"
        )
        assertOppgaver(0)

        godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "SUSPENDERT",
            "FERDIG"
        )

        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `avbryt ikke-eksisterende vedtaksperiode`() =
        assertDoesNotThrow { sendAvbrytSaksbehandling(FØDSELSNUMMER, VEDTAKSPERIODE_ID) }

    private fun vedtaksperiodeTilGodkjenning(): UUID {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsning(godkjenningsmeldingId1, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erEgenAnsatt = false
        )

        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erDigital = true
        )

        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1
        )

        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId1
    }

    private fun tildelOppgave(saksbehandlerOid: UUID) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref, gyldig_til) VALUES(:oppgave_id_ref, :saksbehandler_ref, now() + INTERVAL '14 DAYS');",
                    mapOf(
                        "oppgave_id_ref" to OPPGAVEID,
                        "saksbehandler_ref" to saksbehandlerOid
                    )
                ).asUpdate
            )
        }
    }

    private fun finnOidForTildeling(oppgaveId: Long) =
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT * FROM tildeling WHERE oppgave_id_ref=?;", oppgaveId
                ).map {
                    UUID.fromString(it.string("saksbehandler_ref"))
                }.asSingle
            )
        }

    private fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) {
        sessionOf(dataSource).use {
            val opprettSaksbehandlerQuery = "INSERT INTO saksbehandler(oid, navn, epost) VALUES (:oid, :navn, :epost)"
            it.run(
                queryOf(
                    opprettSaksbehandlerQuery,
                    mapOf<String, Any>(
                        "oid" to oid, "navn" to navn, "epost" to epost
                    )
                ).asUpdate
            )
        }
    }
}
