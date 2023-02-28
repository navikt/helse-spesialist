package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendInntektløsningOld
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Meldingssender.sendRisikovurderingløsningOld
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Meldingssender.sendVergemålløsningOld
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsningOld
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class VedtaksperiodeReberegnetE2ETest : AbstractE2ETest() {
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
        sendÅpneGosysOppgaverløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1, 1
        )
        sendRisikovurderingløsningOld(
            godkjenningsmeldingId = godkjenningsmeldingId1,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendInntektløsningOld(godkjenningsmeldingId = godkjenningsmeldingId1)
        return godkjenningsmeldingId1
    }
}
