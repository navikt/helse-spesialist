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
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(hendelserInspektør)
        clearMocks(opptegnelseDao)
    }

    @Test
    fun `automatisk godkjenning medfører VedtaksperiodeGodkjentAutomatisk`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr)
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeGodkjentAutomatisk>())
    }

    @Test
    fun `automatisk avvisning medfører VedtaksperiodeAvvistAutomatisk`() {
        mediator.automatiskAvvisning(
            context = context,
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
            behov = godkjenningsbehovData(fødselsnummer = fnr),
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = godkjenningsbehovData(fødselsnummer = fnr)
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() =
        verify(exactly = 1) {
            opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV))
        }

    private companion object {
        const val fnr = "12341231221"
    }
}
