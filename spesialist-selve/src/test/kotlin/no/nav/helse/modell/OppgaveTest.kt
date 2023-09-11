package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.OppgaveInspektør.Companion.inspektør
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.oppgaveMedEgenskaper
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.STIKKPRØVE
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.SØKNAD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
        private val saksbehandler = Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )
        private val beslutter = Saksbehandler(
            oid = BESLUTTER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )
    }

    @ParameterizedTest
    @EnumSource(value = Oppgavetype::class, names = ["SØKNAD"], mode = EnumSource.Mode.EXCLUDE)
    fun `Første egenskap anses som oppgavetype`(type: Oppgavetype) {
        val oppgave = nyOppgave(type, SØKNAD)

        inspektør(oppgave) {
            assertEquals(type, this.type)
        }
    }

    @Test
    fun `Defaulter til SØKNAD dersom ingen egenskaper er oppgitt`() {
        val oppgave = nyOppgave()

        inspektør(oppgave) {
            assertEquals(SØKNAD, this.type)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildeling(saksbehandler, false, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon med påVent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(true, påVent)
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved stikkprøve`() {
        val oppgave = nyOppgave(STIKKPRØVE)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved manglende tilgang`() {
        val oppgave = nyOppgave(STIKKPRØVE)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> false })

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
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
        oppgave.sendTilBeslutter(saksbehandler)
        assertThrows<OppgaveAlleredeSendtBeslutter> {
            oppgave.sendTilBeslutter(saksbehandler)
        }
    }

    @Test
    fun `kaster exception dersom oppgave sendes til beslutter uten at oppgaven krever totrinnsvurdering`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = false)
        assertThrows<IllegalArgumentException> {
            oppgave.sendTilBeslutter(saksbehandler)
        }
    }

    @Test
    fun `oppgave sendt til beslutter tildeles ingen dersom det ikke finnes noen tidligere beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.forsøkTildeling(saksbehandler, false) { _, _ -> true }
        oppgave.sendTilBeslutter(saksbehandler)
        inspektør(oppgave) {
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt i retur tildeles opprinnelig saksbehandler`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandler)
        oppgave.sendIRetur(beslutter)
        inspektør(oppgave) {
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `oppgave sendt i retur og deretter tilbake til beslutter tildeles opprinnelig beslutter`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandler)
        oppgave.sendIRetur(beslutter)
        oppgave.sendTilBeslutter(saksbehandler)
        inspektør(oppgave) {
            assertEquals(beslutter, tildeltTil)
        }
    }

    @Test
    fun `Kaster exception dersom oppgave allerede er sendt i retur`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandler)
        oppgave.sendIRetur(beslutter)
        assertThrows<OppgaveAlleredeSendtIRetur> {
            oppgave.sendIRetur(beslutter)
        }
    }

    @Test
    fun `Kaster exception dersom beslutter er samme som opprinnelig saksbehandler ved retur`() {
        val oppgave = nyOppgave(SØKNAD, medTotrinnsvurdering = true)
        oppgave.sendTilBeslutter(saksbehandler)
        assertThrows<OppgaveKreverVurderingAvToSaksbehandlere> {
            oppgave.sendIRetur(saksbehandler)
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
    fun equals() {
        val gjenopptattOppgave = oppgaveMedEgenskaper(1L, VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID, UUID.randomUUID(), listOf(OPPGAVETYPE))
        val oppgave1 = oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), listOf(SØKNAD))
        val oppgave2 = oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), listOf(SØKNAD))
        val oppgave3 = oppgaveMedEgenskaper(OPPGAVE_ID, UUID.randomUUID(), UTBETALING_ID, UUID.randomUUID(), listOf(SØKNAD))
        val oppgave4 = oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), listOf(STIKKPRØVE))
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

    private fun nyOppgave(vararg egenskaper: Oppgavetype, medTotrinnsvurdering: Boolean = false): Oppgave {
        val totrinnsvurdering = if (medTotrinnsvurdering) totrinnsvurdering() else null
        return oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), egenskaper.toList(), totrinnsvurdering)
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
}
