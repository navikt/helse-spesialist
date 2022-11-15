package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class VedtaksperiodeReberegnetE2ETest : AbstractE2ETest() {
    private val OPPGAVEID get() = testRapid.inspektør.oppgaveId()

    @Test
    fun `avbryter saksbehandling før oppgave er opprettet til saksbehandling`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            AKTØR,
            FØDSELSNUMMER,
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "AVVENTER_GODKJENNING",
            gjeldendeTilstand = "AVVENTER_HISTORIKK"
        )

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
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
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "AVVENTER_GODKJENNING",
            gjeldendeTilstand = "AVVENTER_HISTORIKK"
        )

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
            "SUSPENDERT",
            "FERDIG"
        )
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Invalidert)
    }

    @Test
    fun `tildeler andre rundes oppgave til saksbehandler`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val saksbehandlerOid = UUID.randomUUID()

        vedtaksperiodeTilGodkjenning()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")
        tildelOppgave(saksbehandlerOid)

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "AVVENTER_GODKJENNING",
            gjeldendeTilstand = "AVVENTER_HISTORIKK"
        )
        testRapid.reset()
        vedtaksperiodeTilGodkjenning()

        assertEquals(saksbehandlerOid, finnOidForTildeling(OPPGAVEID))
    }

    @Test
    fun `beholder påVent-flagget ved gjentildeling`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val saksbehandlerOid = UUID.randomUUID()

        vedtaksperiodeTilGodkjenning()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")
        tildelOppgave(saksbehandlerOid, påVent = true)

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "AVVENTER_GODKJENNING",
            gjeldendeTilstand = "AVVENTER_HISTORIKK"
        )
        testRapid.reset()
        vedtaksperiodeTilGodkjenning()

        val (oid, påVent) = finnOidOgPåVentForTildeling(OPPGAVEID)!!
        assertEquals(saksbehandlerOid, oid)
        assertEquals(true, påVent) { "Ny oppgave skal være lagt på vent etter reberegning" }
    }

    @Test
    fun `avbryter kommandokjede ved reberegning og oppretter oppgave hos saksbehandler andre runde`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        var godkjenningsmeldingId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId
        )

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "AVVENTER_GODKJENNING",
            gjeldendeTilstand = "AVVENTER_HISTORIKK"
        )

        assertTilstand(
            godkjenningsmeldingId,
            "NY",
            "SUSPENDERT",
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
            "SUSPENDERT",
            "FERDIG"
        )

        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `avbryt ikke-eksisterende vedtaksperiode`() {
        assertDoesNotThrow {
            sendVedtaksperiodeEndret(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                forrigeTilstand = "AVVENTER_GODKJENNING",
                gjeldendeTilstand = "AVVENTER_HISTORIKK"
            )
        }
    }

    @Test
    fun `avbryter ikke om forrige tilstand er noe annet enn AVVENTER_GODKJENNING eller AVVENTER_GODKJENNING_REVURDERING`() {
        testIkkeAvbrutt("TIL_UTBETALING", "UBETALING_FEILET")
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er TIL_INFOTRYGD`() {
        testIkkeAvbrutt("AVVENTER_GODKJENNING_REVURDERING", "TIL_INFOTRYGD")
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er AVSLUTTET`() {
        testIkkeAvbrutt("AVVENTER_GODKJENNING_REVURDERING", "AVSLUTTET")
    }

    @Test
    fun `avbryter ikke om forrige tilstand er AVVENTER_GODKJENNING_REVURDERING og gjeldende tilstand er TIL_UTBETALING`() {
        testIkkeAvbrutt("AVVENTER_GODKJENNING_REVURDERING","TIL_UTBETALING")
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er TIL_INFOTRYGD`() {
        testIkkeAvbrutt(gjeldendeTilstand = "TIL_INFOTRYGD")
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er AVSLUTTET`() {
        testIkkeAvbrutt(gjeldendeTilstand = "AVSLUTTET")
    }

    @Test
    fun `avbryter ikke om gjeldende tilstand er TIL_UTBETALING`() {
        testIkkeAvbrutt(gjeldendeTilstand = "TIL_UTBETALING")
    }

    private fun testIkkeAvbrutt(forrigeTilstand: String = "AVVENTER_GODKJENNING", gjeldendeTilstand: String) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS
        val godkjenningsmeldingId = vedtaksperiodeTilGodkjenning()

        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand
        )

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
            "SUSPENDERT",
            "FERDIG"
        )
        assertOppgavestatuser(0, Oppgavestatus.AvventerSaksbehandler)
    }

    private fun vedtaksperiodeTilGodkjenning(): UUID {
        val godkjenningsmeldingId1 = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        sendPersoninfoløsningComposite(godkjenningsmeldingId1, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId1,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erEgenAnsatt = false
        )
        sendVergemålløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1
        )


        sendDigitalKontaktinformasjonløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            erDigital = true
        )

        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1
        )

        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId1
    }

    private fun tildelOppgave(saksbehandlerOid: UUID, påVent: Boolean = false) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref, på_vent) VALUES(:oppgave_id_ref, :saksbehandler_ref, :paa_vent);",
                    mapOf(
                        "oppgave_id_ref" to OPPGAVEID,
                        "saksbehandler_ref" to saksbehandlerOid,
                        "paa_vent" to påVent,
                    )
                ).asUpdate
            )
        }
    }

    private fun finnOidForTildeling(oppgaveId: Long) = hentFraTildeling<UUID?>(oppgaveId) {
        it.uuid("saksbehandler_ref")
    }

    private fun finnOidOgPåVentForTildeling(oppgaveId: Long) =
        hentFraTildeling<Pair<UUID, Boolean>?>(oppgaveId) {
            it.uuid("saksbehandler_ref") to it.boolean("på_vent")
        }

    private fun <T> hentFraTildeling(oppgaveId: Long, mapping: (Row) -> T) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM tildeling WHERE oppgave_id_ref=?;", oppgaveId
                ).map(mapping).asSingle
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
