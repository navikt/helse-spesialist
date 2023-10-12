package no.nav.helse.modell.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveIkkeTildelt
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.RETUR
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.OppgaveInspektør.Companion.inspektør
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.modell.saksbehandler.handlinger.TilgangskontrollForTestHarTilgang
import no.nav.helse.modell.saksbehandler.handlinger.TilgangskontrollForTestMedKunRiskQA
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random.Default.nextLong

internal class OppgaveTest {
    private companion object {
        private val OPPGAVETYPE = SØKNAD
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "Z999999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Hen Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val BESLUTTER_OID = UUID.randomUUID()
        private val OPPGAVE_ID = nextLong()
        private val saksbehandlerUtenTilgang = saksbehandler()
        private val beslutter = saksbehandler(oid = BESLUTTER_OID)
        private fun saksbehandler(
            epost: String = SAKSBEHANDLER_EPOST,
            oid: UUID = SAKSBEHANDLER_OID,
            navn: String = SAKSBEHANDLER_NAVN,
            ident: String = SAKSBEHANDLER_IDENT,
            tilgangskontroll: Tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        ) = Saksbehandler(
            epostadresse = epost,
            oid = oid,
            navn = navn,
            ident = ident,
            tilgangskontroll = tilgangskontroll,
        )
    }


    @Test
    fun `Første egenskap anses som oppgavetype`() {
        val oppgave = nyOppgave(RISK_QA, SØKNAD)

        inspektør(oppgave) {
            assertEquals(RISK_QA, this.egenskap)
        }
    }

    @Test
    fun `Defaulter til SØKNAD dersom ingen egenskaper er oppgitt`() {
        val oppgave = nyOppgave()

        inspektør(oppgave) {
            assertEquals(SØKNAD, this.egenskap)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerUtenTilgang, tildeltTil)
        }
    }

    @Test
    fun `Kan tildele ved reservasjon dersom saksbehandler har tilgang til alle tilgangsstyrte egenskaper på oppgaven`() {
        val oppgave = nyOppgave(SØKNAD, RISK_QA, FORTROLIG_ADRESSE)
        val saksbehandler = saksbehandler(tilgangskontroll = TilgangskontrollForTestHarTilgang)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler, false)

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `Kan ikke tildele ved reservasjon dersom saksbehandler ikke har tilgang til alle tilgangsstyrte egenskaper på oppgaven`() {
        val oppgave = nyOppgave(SØKNAD, FORTROLIG_ADRESSE, RISK_QA)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler(tilgangskontroll = TilgangskontrollForTestMedKunRiskQA), false)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertNull(tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon med påVent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, påVent = true)

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(true, påVent)
            assertEquals(saksbehandlerUtenTilgang, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved stikkprøve`() {
        val oppgave = nyOppgave(STIKKPRØVE)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, påVent = true)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon med egenskap RISK_QA`() {
        val oppgave = nyOppgave(RISK_QA)
        val saksbehandlerMedTilgang = saksbehandler(tilgangskontroll = TilgangskontrollForTestHarTilgang)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerMedTilgang, påVent = false)

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerMedTilgang, tildeltTil)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["RISK_QA", "EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "SPESIALSAK", "STRENGT_FORTROLIG_ADRESSE", "STIKKPRØVE"])
    fun `Forsøker tildeling ved reservasjon ved manglende tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, påVent = true)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["RISK_QA", "EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "SPESIALSAK", "STRENGT_FORTROLIG_ADRESSE", "STIKKPRØVE"])
    fun `Forsøker tildeling ved manglende tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        oppgave.forsøkTildeling(saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["RISK_QA", "EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "SPESIALSAK", "STRENGT_FORTROLIG_ADRESSE", "STIKKPRØVE"])
    fun `Forsøker tildeling ved tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        val saksbehandlerMedTilgang = saksbehandler(tilgangskontroll = TilgangskontrollForTestHarTilgang)
        oppgave.forsøkTildeling(saksbehandlerMedTilgang)

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerMedTilgang, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling når oppgaven er tildelt noen andre`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildeling(saksbehandlerUtenTilgang)
        assertThrows<OppgaveTildeltNoenAndre> {
            oppgave.forsøkTildeling(saksbehandler(oid = UUID.randomUUID()))
        }

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerUtenTilgang, tildeltTil)
        }
    }

    @Test
    fun `Setter oppgavestatus til AvventerSystem når oppgaven avventer system`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)

        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSystem, tilstand)
        }
    }

    @Test
    fun `Setter oppgavestatus til Invalidert når oppgaven avbrytes`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avbryt()

        inspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, tilstand)
        }
    }

    @Test
    fun `Setter oppgavestatus til Ferdigstilt når oppgaven ferdigstilles`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()

        inspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, tilstand)
        }
    }

    @Test
    fun `kaster exception dersom oppgave allerede er sendt til beslutter når man forsøker å sende til beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        assertThrows<OppgaveAlleredeSendtBeslutter> {
            oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        }
    }

    @Test
    fun `kaster exception dersom oppgave sendes til beslutter uten at oppgaven krever totrinnsvurdering`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = false)
        assertThrows<IllegalArgumentException> {
            oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        }
    }

    @Test
    fun `oppgave sendt til beslutter tildeles ingen dersom det ikke finnes noen tidligere beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt til beslutter får egenskap BESLUTTER`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertTrue(egenskaper.contains(BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt til beslutter ligger ikke lenger på vent`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertFalse(påVent)
        }
    }

    @Test
    fun `oppgave sendt i retur får egenskap RETUR`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertTrue(egenskaper.contains(RETUR))
            assertFalse(egenskaper.contains(BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt i retur ligger ikke lenger på vent`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.forsøkTildelingVedReservasjon(beslutter, true)
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertFalse(påVent)
        }
    }

    @Test
    fun `oppgave sendt til beslutter etter å ha vært sendt i retur har ikke egenskap RETUR`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertFalse(egenskaper.contains(RETUR))
            assertTrue(egenskaper.contains(BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt i retur tildeles opprinnelig saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertEquals(saksbehandlerUtenTilgang, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt i retur og deretter tilbake til beslutter tildeles opprinnelig beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertEquals(beslutter, tildeltTil)
        }
    }

    @Test
    fun `Kaster exception dersom oppgave allerede er sendt i retur`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        assertThrows<OppgaveAlleredeSendtIRetur> {
            oppgave.sendIRetur(beslutter)
        }
    }

    @Test
    fun `Kaster exception dersom beslutter er samme som opprinnelig saksbehandler ved retur`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        assertThrows<OppgaveKreverVurderingAvToSaksbehandlere> {
            oppgave.sendIRetur(saksbehandlerUtenTilgang)
        }
    }

    @Test
    fun `kan ikke ferdigstille en oppgave som ikke har vært behandlet av saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.ferdigstill()
        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSaksbehandler, this.tilstand)
        }
    }

    @Test
    fun `kan ikke invalidere en oppgave i ferdigstilt`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        oppgave.avbryt()
        inspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, this.tilstand)
        }
    }

    @Test
    fun `kan ikke gå i avventer system når en oppgave er i ferdigstilt`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        inspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, this.tilstand)
        }
    }

    @Test
    fun `kan ikke gå i avventer system når en oppgave er i invalidert`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avbryt()
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        inspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, this.tilstand)
        }
    }

    @Test
    fun `kan invalidere en oppgave i avventer saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avbryt()
        inspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, this.tilstand)
        }
    }

    @Test
    fun `kan ferdigstille en oppgave i avventer saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avbryt()
        inspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, this.tilstand)
        }
    }

    @Test
    fun `kan ferdigstille en oppgave i avventer system`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.avbryt()
        inspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, this.tilstand)
        }
    }

    @Test
    fun `sender ut oppgaveEndret når oppgave sendes til beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.register(observer)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)

        assertEquals(1, observer.oppgaverEndret.size)
        assertEquals(oppgave, observer.oppgaverEndret[0])

        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSaksbehandler, this.tilstand)
        }
    }

    @Test
    fun `sender ut oppgaveEndret når oppgave sendes i retur`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.register(observer)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)

        assertEquals(2, observer.oppgaverEndret.size)
        assertEquals(oppgave, observer.oppgaverEndret[0])
        assertEquals(oppgave, observer.oppgaverEndret[1])

        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSaksbehandler, this.tilstand)
        }
    }

    @Test
    fun `legg oppgave på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)
        oppgave.leggPåVent(saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(true, påVent)
        }
    }

    @Test
    fun `kan ikke legge oppgave på vent uten at den er tildelt først`() {
        val oppgave = nyOppgave(SØKNAD)
        assertThrows<OppgaveIkkeTildelt> {
            oppgave.leggPåVent(saksbehandlerUtenTilgang)
        }

        inspektør(oppgave) {
            assertEquals(false, påVent)
        }
    }

    @Test
    fun `fjern påVent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)
        oppgave.leggPåVent(saksbehandlerUtenTilgang)
        oppgave.fjernPåVent(saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(false, påVent)
        }
    }

    @Test
    fun `kan ikke fjerne oppgave fra på vent uten at den er tildelt først`() {
        val oppgave = nyOppgave(SØKNAD)

        assertThrows<OppgaveIkkeTildelt> {
            oppgave.fjernPåVent(saksbehandlerUtenTilgang)
        }

        inspektør(oppgave) {
            assertEquals(false, påVent)
        }
    }

    @Test
    fun `kan ikke fjerne oppgave fra på vent hvis den er tildelt noen andre`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, påVent = false)

        assertThrows<OppgaveTildeltNoenAndre> {
            oppgave.fjernPåVent(beslutter)
        }

        inspektør(oppgave) {
            assertEquals(false, påVent)
        }
    }

    @Test
    fun `sender ut oppgaveEndret når oppgave sendes legges på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.register(observer)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)
        oppgave.leggPåVent(saksbehandlerUtenTilgang)

        assertEquals(2, observer.oppgaverEndret.size)
        assertEquals(oppgave, observer.oppgaverEndret[0])
        assertEquals(oppgave, observer.oppgaverEndret[1])

        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSaksbehandler, this.tilstand)
        }
    }

    @Test
    fun `sender ut oppgaveEndret når oppgave ikke er på vent lenger`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.register(observer)
        oppgave.forsøkTildelingVedReservasjon(saksbehandlerUtenTilgang, false)
        oppgave.leggPåVent(saksbehandlerUtenTilgang)
        oppgave.fjernPåVent(saksbehandlerUtenTilgang)

        assertEquals(3, observer.oppgaverEndret.size)
        assertEquals(oppgave, observer.oppgaverEndret[0])
        assertEquals(oppgave, observer.oppgaverEndret[1])
        assertEquals(oppgave, observer.oppgaverEndret[2])

        inspektør(oppgave) {
            assertEquals(Oppgave.AvventerSaksbehandler, this.tilstand)
        }
    }

    @Test
    fun equals() {
        val gjenopptattOppgave = Oppgave.nyOppgave(
            1L, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), true, listOf(OPPGAVETYPE)
        )
        val oppgave1 =
            Oppgave.nyOppgave(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), true, listOf(SØKNAD))
        val oppgave2 =
            Oppgave.nyOppgave(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), true, listOf(SØKNAD))
        val oppgave3 =
            Oppgave.nyOppgave(OPPGAVE_ID, UUID.randomUUID(), UTBETALING_ID, UUID.randomUUID(), true, listOf(SØKNAD))
        val oppgave4 =
            Oppgave.nyOppgave(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), true, listOf(STIKKPRØVE))
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
        assertNotEquals(oppgave1, oppgave3)
        assertNotEquals(oppgave1.hashCode(), oppgave3.hashCode())
        assertNotEquals(oppgave1, oppgave4)
        assertNotEquals(oppgave1.hashCode(), oppgave4.hashCode())

        gjenopptattOppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        assertNotEquals(gjenopptattOppgave.hashCode(), oppgave2.hashCode())
        assertNotEquals(gjenopptattOppgave, oppgave2)
        assertNotEquals(gjenopptattOppgave, oppgave3)
        assertNotEquals(gjenopptattOppgave, oppgave4)

        oppgave2.avventerSystem("ANNEN_SAKSBEHANDLER", UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }

    private fun nyOppgave(vararg egenskaper: Egenskap, medTotrinnsvurdering: Boolean = false): Oppgave {
        val totrinnsvurdering = if (medTotrinnsvurdering) totrinnsvurdering() else null
        return Oppgave.nyOppgave(
            OPPGAVE_ID,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            UUID.randomUUID(),
            true,
            egenskaper.toList(),
            totrinnsvurdering
        )
    }

    private fun totrinnsvurdering() = Totrinnsvurdering(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = null,
        beslutter = null,
        utbetalingId = null,
        opprettet = LocalDateTime.now(),
        oppdatert = null
    )

    private val observer = object : OppgaveObserver {
        val oppgaverEndret = mutableListOf<Oppgave>()
        override fun oppgaveEndret(oppgave: Oppgave) {
            oppgaverEndret.add(oppgave)
        }
    }
}
