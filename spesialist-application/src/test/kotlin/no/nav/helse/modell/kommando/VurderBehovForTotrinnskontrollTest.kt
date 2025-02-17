package no.nav.helse.modell.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.FeatureToggles
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.OverstyringType
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.februar
import no.nav.helse.spesialist.application.januar
import no.nav.helse.spesialist.application.lagSaksbehandlerident
import no.nav.helse.spesialist.application.lagSaksbehandlernavn
import no.nav.helse.spesialist.application.lagTilfeldigSaksbehandlerepost
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class VurderBehovForTotrinnskontrollTest {
    private companion object {
        private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID_1 = UUID.randomUUID()
        private val VEDTAKSPERIODE = Vedtaksperiode.nyVedtaksperiode(SpleisBehandling("987654321", VEDTAKSPERIODE_ID_2, UUID.randomUUID(), LocalDate.now(), LocalDate.now()))
        private val FØDSELSNUMMER = "fnr"
    }

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val totrinnsvurderingRepository = mockk<TotrinnsvurderingRepository>(relaxed = true)
    private lateinit var context: CommandContext

    val sykefraværstilfelle =
        Sykefraværstilfelle(
            FØDSELSNUMMER,
            1.januar,
            listOf(
                Behandling(UUID.randomUUID(), VEDTAKSPERIODE_ID_1, 1.januar, 31.januar, 1.januar),
                Behandling(UUID.randomUUID(), VEDTAKSPERIODE_ID_2, 1.februar, 28.februar, 1.januar),
            ),
        )
    private val command =
        VurderBehovForTotrinnskontroll(
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            vedtaksperiode = VEDTAKSPERIODE,
            oppgaveService = oppgaveService,
            overstyringDao = overstyringDao,
            periodehistorikkDao = periodehistorikkDao,
            totrinnsvurderingRepository = totrinnsvurderingRepository,
            sykefraværstilfelle = sykefraværstilfelle,
            spleisVedtaksperioder = listOf(
                SpleisVedtaksperiode(
                    VEDTAKSPERIODE_ID_1,
                    UUID.randomUUID(),
                    1.januar,
                    31.januar,
                    1.januar
                ), SpleisVedtaksperiode(VEDTAKSPERIODE_ID_2, UUID.randomUUID(), 1.februar, 28.februar, 1.januar)
            ),
            featureToggles = object : FeatureToggles {
                override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean = true
            }
        )

    @BeforeEach
    fun setUp() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Oppretter totrinnsvurdering dersom vedtaksperioden finnes i overstyringer_for_vedtaksperioder`() {
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for lovvalg og medlemskap, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        sykefraværstilfelle.håndter(
            Varsel(UUID.randomUUID(), "RV_MV_1", LocalDateTime.now(), VEDTAKSPERIODE_ID_2),
        )
        every { oppgaveService.harFerdigstiltOppgave(VEDTAKSPERIODE_ID_2) } returns false

        assertTrue(command.execute(context))
        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `Oppretter ikke totrinnssvurdering dersom tidligere vedtaksperiode har varsel for lovvalg og medlemskap og er utbetalt`(
        status: Varsel.Status,
    ) {
        sykefraværstilfelle.håndter(
            Varsel(UUID.randomUUID(), "RV_MV_1", LocalDateTime.now(), VEDTAKSPERIODE_ID_1, status),
        )
        every { oppgaveService.harFerdigstiltOppgave(VEDTAKSPERIODE_ID_2) } returns false

        assertTrue(command.execute(context))
        verify(exactly = 0) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    @Test
    fun `Hvis totrinnsvurdering har saksbehander skal oppgaven reserveres`() {
        val saksbehandler = lagSaksbehandler(UUID.randomUUID())

        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)
        every { totrinnsvurderingRepository.finn(any<String>()) } returns
                lagTotrinnsvurdering(saksbehandler = saksbehandler)

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.oid, FØDSELSNUMMER) }
    }

    @Test
    fun `Hvis totrinnsvurdering har beslutter skal totrinnsvurderingen markeres som retur`() {
        val saksbehandler = lagSaksbehandler()
        val beslutter = lagSaksbehandler()

        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)
        every { totrinnsvurderingRepository.finn(any<String>()) } returns
                lagTotrinnsvurdering(false, saksbehandler, beslutter)

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.oid, FØDSELSNUMMER) }
        verify(exactly = 1) {
            totrinnsvurderingRepository.lagre(
                lagTotrinnsvurdering(true, saksbehandler, beslutter),
                FØDSELSNUMMER
            )
        }
        verify(exactly = 1) {
            periodehistorikkDao.lagre(historikkinnslag = any<TotrinnsvurderingAutomatiskRetur>(), any())
        }
    }

    @Test
    fun `Oppretter ikke totrinnsvurdering om det ikke er overstyring eller varsel for lovvalg og medlemskap`() {
        assertTrue(command.execute(context))

        verify(exactly = 0) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    @Test
    fun `Oppretter trengerTotrinnsvurdering dersom oppgaven har blitt overstyrt`() {
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))
        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    @Test
    fun `Oppretter trengerTotrinnsvurdering dersom oppgaven har fått avklart skjønnsfastsatt sykepengegrunnlag`() {
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Sykepengegrunnlag)

        assertTrue(command.execute(context))
        verify(exactly = 1) { totrinnsvurderingRepository.finn(any<String>()) }
    }

    private fun lagSaksbehandler(oid: UUID = UUID.randomUUID()) =
        Saksbehandler(
            epostadresse = lagTilfeldigSaksbehandlerepost(),
            oid = oid,
            navn = lagSaksbehandlernavn(),
            ident = lagSaksbehandlerident(),
            TilgangskontrollForTestHarIkkeTilgang
        )

    private fun lagTotrinnsvurdering(
        erRetur: Boolean = false,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        beslutter: Saksbehandler = lagSaksbehandler()
    ) =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(nextLong()),
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            erRetur = erRetur,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            utbetalingId = null,
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            overstyringer = emptyList(),
            ferdigstilt = false,
        )
}
