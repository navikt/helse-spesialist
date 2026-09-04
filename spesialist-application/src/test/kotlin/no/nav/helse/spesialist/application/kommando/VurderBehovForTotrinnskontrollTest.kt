package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.spesialist.domain.*
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.domain.testfixtures.*
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class VurderBehovForTotrinnskontrollTest : ApplicationTest() {
    private val fødselsnummer = lagFødselsnummer()

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())

    private val behandlingUnikId = lagBehandlingUnikId()
    private val spleisBehandlingId = lagSpleisBehandlingId()
    private val vedtaksperiodeId = lagVedtaksperiodeId()
    private val command =
        VurderBehovForTotrinnskontroll(
            fødselsnummer = fødselsnummer,
            oppgaveService = oppgaveService,
            behandlingUnikId = behandlingUnikId,
            vedtaksperiodeId = vedtaksperiodeId,
        )

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for lovvalg og medlemskap, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        sessionContext.varselRepository.lagre(lagVarsel(behandlingUnikId = behandlingUnikId, spleisBehandlingId = spleisBehandlingId, kode = "RV_MV_1"))
        every { oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId.value) } returns false

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertEquals(1, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for manglende inntektsmelding, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        sessionContext.varselRepository.lagre(lagVarsel(behandlingUnikId = behandlingUnikId, spleisBehandlingId = spleisBehandlingId, kode = "RV_IV_10"))
        every { oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId.value) } returns false

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertEquals(1, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `Oppretter ikke totrinnssvurdering dersom tidligere vedtaksperiode har varsel for lovvalg og medlemskap og er utbetalt`(
        status: Varsel.Status,
    ) {
        sessionContext.varselRepository.lagre(lagVarsel(behandlingUnikId = behandlingUnikId, spleisBehandlingId = spleisBehandlingId, kode = "RV_MV_1", status = status))
        every { oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId.value) } returns false

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertEquals(0, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `Oppretter ikke totrinnssvurdering dersom tidligere vedtaksperiode har varsel for manglende inntektsmelding og er utbetalt`(
        status: Varsel.Status,
    ) {
        sessionContext.varselRepository.lagre(lagVarsel(behandlingUnikId = behandlingUnikId, spleisBehandlingId = spleisBehandlingId, kode = "RV_IV_10", status = status))
        every { oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId.value) } returns false

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertEquals(0, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    @Test
    fun `Hvis totrinnsvurdering har saksbehander skal oppgaven reserveres`() {
        val saksbehandler = lagSaksbehandlerOid(UUID.randomUUID())

        sessionContext.totrinnsvurderingRepository.lagre(lagTotrinnsvurdering(saksbehandler = saksbehandler))

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.totrinnsvurderingRepository.alle().size)
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.value, fødselsnummer) }
    }

    @Test
    fun `Hvis totrinnsvurdering har beslutter skal tilstanden til totrinnsvurderingen settes tilbake til AVVENTER_SAKSBEHANDLER`() {
        val saksbehandler = lagSaksbehandlerOid()
        val beslutter = lagSaksbehandlerOid()

        sessionContext.totrinnsvurderingRepository.lagre(
            lagTotrinnsvurdering(
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            ),
        )

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.totrinnsvurderingRepository.alle().size)
        verify(exactly = 1) { oppgaveService.reserverOppgave(saksbehandler.value, fødselsnummer) }

        assertEquals(
            AVVENTER_SAKSBEHANDLER,
            sessionContext.totrinnsvurderingRepository
                .alle()
                .single()
                .tilstand,
        )

        assertEquals(
            1,
            sessionContext.periodehistorikkDao.behandlingData[behandlingUnikId.value]
                ?.filterIsInstance<TotrinnsvurderingAutomatiskRetur>()
                ?.size,
        )
    }

    @Test
    fun `Oppretter ikke totrinnsvurdering om det ikke er overstyring eller varsel for lovvalg og medlemskap`() {
        assertTrue(command.execute(commandContext, sessionContext, outbox))

        assertEquals(0, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    private fun lagSaksbehandlerOid(oid: UUID = UUID.randomUUID()) = SaksbehandlerOid(oid)

    private fun lagTotrinnsvurdering(
        tilstand: TotrinnsvurderingTilstand = AVVENTER_BESLUTTER,
        saksbehandler: SaksbehandlerOid = lagSaksbehandlerOid(),
        beslutter: SaksbehandlerOid = lagSaksbehandlerOid(),
    ) = Totrinnsvurdering.fraLagring(
        id = TotrinnsvurderingId(nextLong()),
        fødselsnummer = fødselsnummer,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        overstyringer = emptyList(),
        tilstand = tilstand,
        vedtaksperiodeForkastet = false,
    )
}
