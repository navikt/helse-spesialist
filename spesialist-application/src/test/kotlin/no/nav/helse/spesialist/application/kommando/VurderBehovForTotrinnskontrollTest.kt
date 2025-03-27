package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.FeatureToggles
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
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
        private val VEDTAKSPERIODE = Vedtaksperiode.nyVedtaksperiode(
            SpleisBehandling(
                "987654321",
                VEDTAKSPERIODE_ID_2,
                UUID.randomUUID(),
                LocalDate.now(),
                LocalDate.now()
            )
        )
        private val FØDSELSNUMMER = "fnr"
        private val AKTØR = "fnr"
    }

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val overstyringRepository = object : OverstyringRepository {
        var overstyringerSomSkalReturneres: Overstyring? = null
        override fun lagre(overstyringer: List<Overstyring>, totrinnsvurderingId: TotrinnsvurderingId?) =
            error("Not implemented for test")

        override fun finnAktive(fødselsnummer: String, totrinnsvurderingId: TotrinnsvurderingId): List<Overstyring> =
            error("Not implemented for test")

        override fun finnAktive(fødselsnummer: String): List<Overstyring> {
            return overstyringerSomSkalReturneres?.let { listOf(it) } ?: emptyList()
        }

    }
    private val totrinnsvurderingRepository = object : TotrinnsvurderingRepository {
        val lagredeTotrinnsvurderinger = mutableListOf<Totrinnsvurdering>()
        var totrinnsvurderingSomSkalReturneres: Totrinnsvurdering? = null
        override fun lagre(totrinnsvurdering: Totrinnsvurdering) {
            if (!totrinnsvurdering.harFåttTildeltId()) {
                totrinnsvurdering.tildelId(TotrinnsvurderingId(nextLong()))
            }
            lagredeTotrinnsvurderinger.add(totrinnsvurdering)
        }

        override fun finn(fødselsnummer: String): Totrinnsvurdering? = totrinnsvurderingSomSkalReturneres

        @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull", ReplaceWith("finn"))
        override fun finn(vedtaksperiodeId: UUID): Totrinnsvurdering? = totrinnsvurderingSomSkalReturneres
    }
    private lateinit var context: CommandContext

    val sykefraværstilfelle =
        Sykefraværstilfelle(
            FØDSELSNUMMER,
            1 jan 2018,
            listOf(
                LegacyBehandling(UUID.randomUUID(), VEDTAKSPERIODE_ID_1, 1 jan 2018, 31 jan 2018, 1 jan 2018),
                LegacyBehandling(UUID.randomUUID(), VEDTAKSPERIODE_ID_2, 1 feb 2018, 28 feb 2018, 1 jan 2018),
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
            overstyringRepository = overstyringRepository,
            sykefraværstilfelle = sykefraværstilfelle,
            featureToggles = object : FeatureToggles {
                override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean = true
            }
        )

    @BeforeEach
    fun setUp() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for lovvalg og medlemskap, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        sykefraværstilfelle.håndter(
            Varsel(UUID.randomUUID(), "RV_MV_1", LocalDateTime.now(), VEDTAKSPERIODE_ID_2),
        )
        every { oppgaveService.harFerdigstiltOppgave(VEDTAKSPERIODE_ID_2) } returns false

        assertTrue(command.execute(context))
        assertEquals(1, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
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
        assertEquals(0, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
    }

    @Test
    fun `Hvis totrinnsvurdering har saksbehander skal oppgaven reserveres`() {
        val saksbehandler = lagSaksbehandlerOid(UUID.randomUUID())

        overstyringRepository.overstyringerSomSkalReturneres = lagOverstyring(saksbehandlerOid = saksbehandler)
        totrinnsvurderingRepository.totrinnsvurderingSomSkalReturneres =
            lagTotrinnsvurdering(saksbehandler = saksbehandler)

        assertTrue(command.execute(context))

        assertEquals(1, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.value, FØDSELSNUMMER) }
    }

    @Test
    fun `Hvis totrinnsvurdering har beslutter skal tilstanden til totrinnsvurderingen settes tilbake til AVVENTER_SAKSBEHANDLER`() {
        val saksbehandler = lagSaksbehandlerOid()
        val beslutter = lagSaksbehandlerOid()

        overstyringRepository.overstyringerSomSkalReturneres = lagOverstyring(saksbehandlerOid = saksbehandler)
        totrinnsvurderingRepository.totrinnsvurderingSomSkalReturneres = lagTotrinnsvurdering(
            saksbehandler = saksbehandler,
            beslutter = beslutter
        )

        assertTrue(command.execute(context))

        assertEquals(1, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.value, FØDSELSNUMMER) }

        assertEquals(AVVENTER_SAKSBEHANDLER, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.single().tilstand)

        verify(exactly = 1) {
            periodehistorikkDao.lagre(historikkinnslag = any<TotrinnsvurderingAutomatiskRetur>(), any())
        }
    }

    @Test
    fun `Oppretter ikke totrinnsvurdering om det ikke er overstyring eller varsel for lovvalg og medlemskap`() {
        assertTrue(command.execute(context))

        assertEquals(0, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
    }

    @Test
    fun `Tester at gammel løype oppretter totrinnsvurdering dersom oppgaven har blitt overstyrt`() {
        every { overstyringDao.harVedtaksperiodePågåendeOverstyring(any()) } returns true

        assertTrue(command(nyTotrinnsløype = false).execute(context))
        assertEquals(1, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
    }

    @Test
    fun `Tester at gammel løype oppretter totrinnsvurdering dersom oppgaven har fått avklart skjønnsfastsatt sykepengegrunnlag`() {
        every { overstyringDao.harVedtaksperiodePågåendeOverstyring(any()) } returns true

        assertTrue(command(nyTotrinnsløype = false).execute(context))
        assertEquals(1, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
    }

    private fun lagSaksbehandlerOid(oid: UUID = UUID.randomUUID()) = SaksbehandlerOid(oid)

    private fun lagOverstyring(saksbehandlerOid: SaksbehandlerOid = lagSaksbehandlerOid()) =
        OverstyrtTidslinje.ny(
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            organisasjonsnummer = "123456789",
            dager = emptyList(),
            begrunnelse = "begrunnelse",
        )

    private fun lagTotrinnsvurdering(
        tilstand: TotrinnsvurderingTilstand = AVVENTER_BESLUTTER,
        saksbehandler: SaksbehandlerOid = lagSaksbehandlerOid(),
        beslutter: SaksbehandlerOid = lagSaksbehandlerOid()
    ) =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(nextLong()),
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            fødselsnummer = FØDSELSNUMMER,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            utbetalingId = null,
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            overstyringer = emptyList(),
            tilstand = tilstand,
            vedtaksperiodeForkastet = false,
        )

    private fun command(nyTotrinnsløype: Boolean = true) =
        VurderBehovForTotrinnskontroll(
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            vedtaksperiode = VEDTAKSPERIODE,
            oppgaveService = oppgaveService,
            overstyringDao = overstyringDao,
            periodehistorikkDao = periodehistorikkDao,
            totrinnsvurderingRepository = totrinnsvurderingRepository,
            overstyringRepository = overstyringRepository,
            sykefraværstilfelle = sykefraværstilfelle,
            featureToggles = object : FeatureToggles {
                override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean = nyTotrinnsløype
            }
        )
}
