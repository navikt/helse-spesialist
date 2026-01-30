package no.nav.helse.modell.oppgave

import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.OppgaveIkkeTildelt
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.OppgaveInspektør.Companion.inspektør
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class OppgaveTest {
    private companion object {
        private val OPPGAVETYPE = SØKNAD
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val SAKSBEHANDLER_IDENT = NAVIdent("Z999999")
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
            ident: NAVIdent = SAKSBEHANDLER_IDENT,
        ) = SaksbehandlerWrapper(
            Saksbehandler(
                id = SaksbehandlerOid(oid),
                navn = navn,
                epost = epost,
                ident = ident,
            ),
        )
    }

    @Test
    fun `Forsøker tildeling ved reservasjon`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerUtenTilgang.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `Kan tildele ved reservasjon dersom saksbehandler har tilgang til alle tilgangsstyrte egenskaper på oppgaven`() {
        val oppgave = nyOppgave(SØKNAD, FORTROLIG_ADRESSE)
        val saksbehandler = saksbehandler()
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandler,
            saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet(),
            brukerroller = emptySet()
        )

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(saksbehandler.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `Kan ikke tildele ved reservasjon dersom saksbehandler ikke har tilgang til alle tilgangsstyrte egenskaper på oppgaven`() {
        val oppgave = nyOppgave(SØKNAD, FORTROLIG_ADRESSE, STRENGT_FORTROLIG_ADRESSE)
        assertThrows<ManglerTilgang> {
            oppgave.forsøkTildelingVedReservasjon(
                saksbehandlerWrapper = saksbehandler(),
                saksbehandlerTilgangsgrupper = setOf(Tilgangsgruppe.KODE_7),
                brukerroller = emptySet()
            )
        }

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertNull(tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon med påVent`() {
        val oppgave = nyOppgave(PÅ_VENT, SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(true, påVent)
            assertEquals(saksbehandlerUtenTilgang.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved stikkprøve`() {
        val oppgave = nyOppgave(PÅ_VENT, STIKKPRØVE)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøk avmelding av oppgave`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildeling(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.forsøkAvmelding(saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøk avmelding av oppgave når oppgaven er tildelt noen andre`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildeling(
            saksbehandlerWrapper = saksbehandler(oid = UUID.randomUUID()),
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.forsøkAvmelding(saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøk avmelding av oppgave når oppgaven ikke er tildelt`() {
        val oppgave = nyOppgave()
        assertThrows<OppgaveIkkeTildelt> {
            oppgave.forsøkAvmelding(saksbehandlerUtenTilgang)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "STRENGT_FORTROLIG_ADRESSE"])
    fun `Forsøker tildeling ved reservasjon ved manglende tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        assertThrows<ManglerTilgang> {
            oppgave.forsøkTildelingVedReservasjon(
                saksbehandlerWrapper = saksbehandlerUtenTilgang,
                saksbehandlerTilgangsgrupper = emptySet(),
                brukerroller = emptySet()
            )
        }

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(null, tildeltTil)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "STRENGT_FORTROLIG_ADRESSE", "STIKKPRØVE"])
    fun `Forsøker tildeling ved manglende tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        assertThrows<ManglerTilgang> {
            oppgave.forsøkTildeling(
                saksbehandlerWrapper = saksbehandlerUtenTilgang,
                saksbehandlerTilgangsgrupper = emptySet(),
                brukerroller = emptySet()
            )
        }

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(null, tildeltTil)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["EGEN_ANSATT", "FORTROLIG_ADRESSE", "BESLUTTER", "SPESIALSAK", "STIKKPRØVE"])
    fun `Forsøker tildeling ved tilgang`(egenskap: Egenskap) {
        val oppgave = nyOppgave(egenskap)
        val saksbehandlerMedTilgang = saksbehandler()
        oppgave.forsøkTildeling(
            saksbehandlerWrapper = saksbehandlerMedTilgang,
            saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet(),
            brukerroller = emptySet()
        )

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerMedTilgang.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling når oppgaven er tildelt noen andre`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildeling(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        assertThrows<OppgaveTildeltNoenAndre> {
            oppgave.forsøkTildeling(
                saksbehandlerWrapper = saksbehandler(oid = UUID.randomUUID()),
                saksbehandlerTilgangsgrupper = emptySet(),
                brukerroller = emptySet()
            )
        }

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandlerUtenTilgang.saksbehandler.id, tildeltTil)
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
    fun `oppgave sendt til beslutter tildeles ingen dersom det ikke finnes noen tidligere beslutter`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.sendTilBeslutter(null)
        inspektør(oppgave) {
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt til beslutter får egenskap BESLUTTER`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertTrue(egenskaper.contains(Egenskap.BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt i retur får egenskap RETUR`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertTrue(egenskaper.contains(Egenskap.RETUR))
            assertFalse(egenskaper.contains(Egenskap.BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt i retur ligger ikke lenger på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = beslutter,
            saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet(),
            brukerroller = emptySet()
        )
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertFalse(påVent)
        }
    }

    @Test
    fun `oppgave sendt til beslutter etter å ha vært sendt i retur har ikke egenskap RETUR`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        oppgave.sendIRetur(beslutter)
        oppgave.sendTilBeslutter(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertFalse(egenskaper.contains(Egenskap.RETUR))
            assertTrue(egenskaper.contains(Egenskap.BESLUTTER))
        }
    }

    @Test
    fun `oppgave sendt i retur tildeles opprinnelig saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.sendTilBeslutter(beslutter)
        oppgave.sendIRetur(saksbehandlerUtenTilgang)
        inspektør(oppgave) {
            assertEquals(saksbehandlerUtenTilgang.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt i retur og deretter tilbake til beslutter tildeles opprinnelig beslutter`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.sendTilBeslutter(beslutter)
        oppgave.sendIRetur(saksbehandlerUtenTilgang)
        oppgave.sendTilBeslutter(beslutter)
        inspektør(oppgave) {
            assertEquals(beslutter.saksbehandler.id, tildeltTil)
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
        val oppgave = nyOppgave(SØKNAD)
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
        val oppgave = nyOppgave(SØKNAD)
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
    fun `legg til egenskap for gosys kun dersom den ikke finnes fra før`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.leggTilGosys()
        oppgave.leggTilGosys()

        inspektør(oppgave) {
            assertEquals(1, egenskaper.filter { it == Egenskap.GOSYS }.size)
        }
    }

    @Test
    fun `legg på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.leggPåVent(true, saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(true, påVent)
        }
    }

    @Test
    fun `legg på vent og skalTildeles`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.leggPåVent(true, saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(true, påVent)
            assertTrue(egenskaper.contains(PÅ_VENT))
            assertEquals(saksbehandlerUtenTilgang.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `legg på vent og skalTildeles ny saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.leggPåVent(true, beslutter)

        inspektør(oppgave) {
            assertEquals(true, påVent)
            assertTrue(egenskaper.contains(PÅ_VENT))
            assertEquals(beslutter.saksbehandler.id, tildeltTil)
        }
    }

    @Test
    fun `legg på vent og !skalTildeles`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.leggPåVent(false, saksbehandlerUtenTilgang)

        inspektør(oppgave) {
            assertEquals(true, påVent)
            assertTrue(egenskaper.contains(PÅ_VENT))
            assertNull(this.tildeltTil)
        }
    }

    @Test
    fun `fjern på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.leggPåVent(false, saksbehandlerUtenTilgang)
        oppgave.fjernFraPåVent()

        inspektør(oppgave) {
            assertEquals(false, påVent)
            assertTrue(egenskaper.none { it == PÅ_VENT })
            assertNull(this.tildeltTil)
        }
    }

    @Test
    fun `sender ut oppgaveEndret når oppgave sendes legges på vent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.register(observer)
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.leggPåVent(true, saksbehandlerUtenTilgang)

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
        oppgave.forsøkTildelingVedReservasjon(
            saksbehandlerWrapper = saksbehandlerUtenTilgang,
            saksbehandlerTilgangsgrupper = emptySet(),
            brukerroller = emptySet()
        )
        oppgave.leggPåVent(true, saksbehandlerUtenTilgang)
        oppgave.fjernFraPåVent()

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
        val gjenopptattOppgave =
            Oppgave.ny(
                id = 1L,
                førsteOpprettet = null,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(OPPGAVETYPE),
            )
        val oppgave1 =
            Oppgave.ny(
                id = OPPGAVE_ID,
                førsteOpprettet = null,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(SØKNAD),
            )
        val oppgave2 =
            Oppgave.ny(
                id = OPPGAVE_ID,
                førsteOpprettet = null,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(SØKNAD),
            )
        val oppgave3 =
            Oppgave.ny(
                id = OPPGAVE_ID,
                førsteOpprettet = null,
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(SØKNAD),
            )
        val oppgave4 =
            Oppgave.ny(
                id = OPPGAVE_ID,
                førsteOpprettet = null,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(STIKKPRØVE),
            )
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

        oppgave2.avventerSystem(lagSaksbehandler().ident, UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }

    private fun nyOppgave(vararg egenskaper: Egenskap) =
        Oppgave.ny(
            id = OPPGAVE_ID,
            førsteOpprettet = null,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            behandlingId = UTBETALING_ID,
            utbetalingId = BEHANDLING_ID,
            hendelseId = UUID.randomUUID(),
            kanAvvises = true,
            egenskaper = egenskaper.toSet(),
        )

    private val observer =
        object : OppgaveObserver {
            val oppgaverEndret = mutableListOf<Oppgave>()

            override fun oppgaveEndret(oppgave: Oppgave) {
                oppgaverEndret.add(oppgave)
            }
        }
}
