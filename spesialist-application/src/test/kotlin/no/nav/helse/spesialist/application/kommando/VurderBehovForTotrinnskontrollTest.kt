package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
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
                organisasjonsnummer = "987654321",
                vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
                spleisBehandlingId = UUID.randomUUID(),
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
            )
        )
        private val FØDSELSNUMMER = "fnr"
    }

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val totrinnsvurderingRepository = object : TotrinnsvurderingRepository {
        val lagredeTotrinnsvurderinger = mutableListOf<Totrinnsvurdering>()
        var totrinnsvurderingSomSkalReturneres: Totrinnsvurdering? = null
        override fun lagre(totrinnsvurdering: Totrinnsvurdering) {
            if (!totrinnsvurdering.harFåttTildeltId()) {
                totrinnsvurdering.tildelId(TotrinnsvurderingId(nextLong()))
            }
            lagredeTotrinnsvurderinger.add(totrinnsvurdering)
        }

        override fun finn(id: TotrinnsvurderingId): Totrinnsvurdering? = totrinnsvurderingSomSkalReturneres

        override fun finnAktivForPerson(fødselsnummer: String): Totrinnsvurdering? = totrinnsvurderingSomSkalReturneres
    }
    private lateinit var context: CommandContext

    val sykefraværstilfelle =
        Sykefraværstilfelle(
            FØDSELSNUMMER,
            1 jan 2018,
            listOf(
                LegacyBehandling(
                    id = UUID.randomUUID(),
                    vedtaksperiodeId = VEDTAKSPERIODE_ID_1,
                    fom = 1 jan 2018,
                    tom = 31 jan 2018,
                    skjæringstidspunkt = 1 jan 2018,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
                ),
                LegacyBehandling(
                    id = UUID.randomUUID(),
                    vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
                    fom = 1 feb 2018,
                    tom = 28 feb 2018,
                    skjæringstidspunkt = 1 jan 2018,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
                ),
            ),
        )
    private val command =
        VurderBehovForTotrinnskontroll(
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiode = VEDTAKSPERIODE,
            oppgaveService = oppgaveService,
            periodehistorikkDao = periodehistorikkDao,
            totrinnsvurderingRepository = totrinnsvurderingRepository,
            sykefraværstilfelle = sykefraværstilfelle,
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

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for manglende inntektsmelding, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        sykefraværstilfelle.håndter(
            Varsel(UUID.randomUUID(), "RV_IV_10", LocalDateTime.now(), VEDTAKSPERIODE_ID_2),
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

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `Oppretter ikke totrinnssvurdering dersom tidligere vedtaksperiode har varsel for manglende inntektsmelding og er utbetalt`(
        status: Varsel.Status,
    ) {
        sykefraværstilfelle.håndter(
            Varsel(UUID.randomUUID(), "RV_IV_10", LocalDateTime.now(), VEDTAKSPERIODE_ID_1, status),
        )
        every { oppgaveService.harFerdigstiltOppgave(VEDTAKSPERIODE_ID_2) } returns false

        assertTrue(command.execute(context))
        assertEquals(0, totrinnsvurderingRepository.lagredeTotrinnsvurderinger.size)
    }

    @Test
    fun `Hvis totrinnsvurdering har saksbehander skal oppgaven reserveres`() {
        val saksbehandler = lagSaksbehandlerOid(UUID.randomUUID())

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



    private fun lagSaksbehandlerOid(oid: UUID = UUID.randomUUID()) = SaksbehandlerOid(oid)

    private fun lagTotrinnsvurdering(
        tilstand: TotrinnsvurderingTilstand = AVVENTER_BESLUTTER,
        saksbehandler: SaksbehandlerOid = lagSaksbehandlerOid(),
        beslutter: SaksbehandlerOid = lagSaksbehandlerOid()
    ) =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(nextLong()),
            fødselsnummer = FØDSELSNUMMER,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            overstyringer = emptyList(),
            tilstand = tilstand,
            vedtaksperiodeForkastet = false,
        )
}
