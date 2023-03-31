package no.nav.helse.mediator

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GodkjenningMediatorTest {
    private lateinit var context: CommandContext
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val varselRepository = object : VarselRepository {
        val generasjonerMedGodkjenteVarsler = mutableSetOf<UUID>()
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {}
        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {}
        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            generasjonerMedGodkjenteVarsler.add(generasjonId)
        }
        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {}
        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {}
        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {}
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {}
    }
    private val mediator = GodkjenningMediator(
        warningDao = mockk(relaxed = true),
        vedtakDao = mockk(relaxed = true),
        opptegnelseDao = opptegnelseDao,
        varselRepository = varselRepository,
    )

    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(opptegnelseDao)
    }

    @Test
    fun `automatisk avvisning skal opprette opptegnelse`() {
        mediator.automatiskAvvisning(context, UtbetalingsgodkjenningMessage("{}", utbetaling), UUID.randomUUID(), fnr, listOf("foo"), UUID.randomUUID())
        assertOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(context, UtbetalingsgodkjenningMessage("{}", utbetaling), UUID.randomUUID(), fnr, UUID.randomUUID())
        assertOpptegnelseOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerUtbetaling(
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            gjeldendeGenerasjoner = listOf(generasjon())
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `saksbehandler avvisning skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerAvvisning(
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
            emptyList(),
            listOf(generasjon())
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `godkjenner varsler for alle gjeldende generasjoner`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId1)
        val generasjon2 = generasjon(generasjonId2)
        generasjon1.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon2.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)

        godkjenning(listOf(generasjon1, generasjon2))
        assertEquals(2, varselRepository.generasjonerMedGodkjenteVarsler.size)
        assertEquals(generasjonId1, varselRepository.generasjonerMedGodkjenteVarsler.toList()[0])
        assertEquals(generasjonId2, varselRepository.generasjonerMedGodkjenteVarsler.toList()[1])
    }

    private fun generasjon(id: UUID = UUID.randomUUID()) = Generasjon(
        id = id,
        vedtaksperiodeId = UUID.randomUUID(),
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private fun godkjenning(generasjoner: List<Generasjon>) = mediator.saksbehandlerUtbetaling(
        context,
        UtbetalingsgodkjenningMessage("{}", utbetaling),
        UUID.randomUUID(),
        fnr,
        "Z000000",
        "saksbehandler@nav.no",
        LocalDateTime.now(),
        emptyList(),
        generasjoner
    )

    private fun assertOpptegnelseOpprettet() = verify(exactly = 1) { opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)) }

    private fun assertOpptegnelseIkkeOpprettet() = verify(exactly = 0) { opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)) }

    private companion object {
        const val fnr = "12341231221"
    }
}
