package no.nav.helse.mediator

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GodkjenningMediatorTest {
    private lateinit var context: CommandContext
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val observer = object : IVedtaksperiodeObserver {
        val generasjonerMedGodkjenteVarsler = mutableSetOf<UUID>()
        override fun varselOpprettet(
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            generasjonerMedGodkjenteVarsler.add(generasjonId)
        }
    }
    private val mediator = GodkjenningMediator(
        vedtakDao = mockk(relaxed = true),
        opptegnelseDao = opptegnelseDao,
        oppgaveDao = mockk(relaxed = true),
        utbetalingDao = mockk(relaxed = true),
        hendelseDao = mockk(relaxed = true) {
            every { finnUtbetalingsgodkjenningbehovJson(any()) } returns "{}"
            every { finnFødselsnummer(any()) } returns fnr
        },
    )

    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(opptegnelseDao)
    }

    @Test
    fun `automatisk avvisning skal opprette opptegnelse`() {
        mediator.automatiskAvvisning(context::publiser, UUID.randomUUID(), listOf("foo"), utbetaling, UUID.randomUUID())
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(context, UtbetalingsgodkjenningMessage("{}", utbetaling), UUID.randomUUID(), fnr, UUID.randomUUID())
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon()), emptyList())
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `saksbehandler avvisning skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerAvvisning(
            behandlingId = UUID.randomUUID(),
            context,
            UtbetalingsgodkjenningMessage("{}", utbetaling),
            UUID.randomUUID(),
            fnr,
            "1",
            "2@nav.no",
            LocalDateTime.now(),
            null,
            null,
            null,
            emptyList()
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
        generasjon1.registrer(observer)
        generasjon2.registrer(observer)
        val varsel1 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId1)
        val varsel2 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2)
        generasjon1.håndterNyttVarsel(varsel1, UUID.randomUUID())
        generasjon2.håndterNyttVarsel(varsel2, UUID.randomUUID())

        godkjenning(listOf(generasjon1, generasjon2))
        assertEquals(2, observer.generasjonerMedGodkjenteVarsler.size)
        assertEquals(generasjonId1, observer.generasjonerMedGodkjenteVarsler.toList()[0])
        assertEquals(generasjonId2, observer.generasjonerMedGodkjenteVarsler.toList()[1])
    }

    private fun generasjon(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private fun godkjenning(generasjoner: List<Generasjon>) = mediator.saksbehandlerUtbetaling(
        behandlingId = UUID.randomUUID(),
        UUID.randomUUID(),
        context,
        UtbetalingsgodkjenningMessage("{}", utbetaling),
        UUID.randomUUID(),
        fnr,
        "Z000000",
        "saksbehandler@nav.no",
        LocalDateTime.now(),
        emptyList(),
        Sykefraværstilfelle(fnr, 1.januar, generasjoner, emptyList())
    )

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() = verify(exactly = 1) {
        opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.FERDIGBEHANDLET_GODKJENNIGSBEHOV))
    }

    private fun assertOpptegnelseIkkeOpprettet() = verify(exactly = 0) { opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)) }

    private companion object {
        const val fnr = "12341231221"
    }
}
