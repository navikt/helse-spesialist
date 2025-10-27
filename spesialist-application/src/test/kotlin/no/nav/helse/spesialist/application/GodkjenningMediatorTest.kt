package no.nav.helse.spesialist.application

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.application.modell.inspektør
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagTilfeldigSaksbehandlerepost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class GodkjenningMediatorTest {
    private lateinit var context: CommandContext
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val hendelserInspektør =
        object : CommandContextObserver {
            private val hendelser = mutableListOf<UtgåendeHendelse>()

            inline fun <reified T : UtgåendeHendelse> hendelseOrNull() = hendelser.singleOrNull { it is T }

            override fun hendelse(hendelse: UtgåendeHendelse) {
                hendelser.add(hendelse)
            }
        }
    private val mediator = GodkjenningMediator(opptegnelseDao)

    private val saksbehandler =
        Saksbehandlerløsning.Saksbehandler(
            ident = "saksbehandlerident",
            epostadresse = "saksbehandler@nav.no",
        )

    private val beslutter =
        Saksbehandlerløsning.Saksbehandler(
            ident = "beslutterident",
            epostadresse = "beslutter@nav.no",
        )

    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(hendelserInspektør)
        clearMocks(opptegnelseDao)
    }

    @Test
    fun `godkjent saksbehandlerløsning medfører VedtaksperiodeGodkjentManuelt`() {
        mediator.saksbehandlerUtbetaling(
            context = context,
            behov = godkjenningsbehovData(),
            saksbehandlerIdent = lagSaksbehandlerident(),
            saksbehandlerEpost = lagTilfeldigSaksbehandlerepost(),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1 jan 2018, listOf(generasjon())),
            utbetaling = utbetaling,
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeGodkjentManuelt>())
    }

    @Test
    fun `avvist saksbehandlerløsning medfører VedtaksperiodeAvvistManuelt`() {
        mediator.saksbehandlerAvvisning(
            context = context,
            behov = godkjenningsbehovData(),
            saksbehandlerIdent = lagSaksbehandlerident(),
            saksbehandlerEpost = lagTilfeldigSaksbehandlerepost(),
            saksbehandler = saksbehandler,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            utbetaling = utbetaling,
            årsak = null,
            begrunnelser = null,
            kommentar = null
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeAvvistManuelt>())
    }

    @Test
    fun `automatisk godkjenning medfører VedtaksperiodeGodkjentAutomatisk`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            utbetaling = utbetaling
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeGodkjentAutomatisk>())
    }

    @Test
    fun `automatisk avvisning medfører VedtaksperiodeAvvistAutomatisk`() {
        mediator.automatiskAvvisning(
            context = context,
            utbetaling = utbetaling,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            begrunnelser = emptyList(),
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeAvvistAutomatisk>())
    }

    @Test
    fun `automatisk avvisning skal opprette opptegnelse`() {
        mediator.automatiskAvvisning(
            context = context,
            begrunnelser = listOf("foo"),
            utbetaling = utbetaling,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            utbetaling = utbetaling
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1 jan 2018, listOf(generasjon())),
            utbetaling = utbetaling,
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `saksbehandler avvisning skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerAvvisning(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            utbetaling = utbetaling,
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `godkjenner varsler for alle gjeldende generasjoner`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId1, vedtaksperiodeId1)
        val generasjon2 = generasjon(generasjonId2, vedtaksperiodeId2)
        val varsel1 = LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId1)
        val varsel2 = LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2)
        generasjon1.håndterNyttVarsel(varsel1)
        generasjon2.håndterNyttVarsel(varsel2)

        godkjenning(listOf(generasjon1, generasjon2))
        generasjon1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals(vedtaksperiodeId1, varsler.first().vedtaksperiodeId)
        }
        generasjon2.inspektør {
            assertEquals(1, varsler.size)
            assertEquals(vedtaksperiodeId2, varsler.first().vedtaksperiodeId)
        }
    }

    private fun generasjon(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
    ) = LegacyBehandling(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1 jan 2018,
        tom = 31 jan 2018,
        skjæringstidspunkt = 1 jan 2018,
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
    )

    private fun godkjenning(generasjoner: List<LegacyBehandling>) =
        mediator.saksbehandlerUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr),
            saksbehandlerIdent = "Z000000",
            saksbehandlerEpost = "saksbehandler@nav.no",
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1 jan 2018, generasjoner),
            utbetaling = utbetaling,
        )

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() =
        verify(exactly = 1) {
            opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV))
        }

    private fun assertOpptegnelseIkkeOpprettet() =
        verify(exactly = 0) {
            opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE))
        }

    private companion object {
        const val fnr = "12341231221"
    }
}
