package no.nav.helse.spesialist.application

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.application.kommando.ApplicationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class GodkjenningMediatorTest : ApplicationTest() {
    private val fødselsnummer = lagFødselsnummer()
    private val hendelserInspektør =
        object : CommandContextObserver {
            private val hendelser = mutableListOf<UtgåendeHendelse>()

            inline fun <reified T : UtgåendeHendelse> hendelseOrNull() = hendelser.singleOrNull { it is T }

            override fun hendelse(hendelse: UtgåendeHendelse) {
                hendelser.add(hendelse)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(hendelserInspektør) }

    private val mediator = GodkjenningMediator(sessionContext.opptegnelseRepository)

    @Test
    fun `automatisk godkjenning medfører VedtaksperiodeGodkjentAutomatisk`() {
        mediator.automatiskUtbetaling(
            commandContext = commandContext,
            behov = godkjenningsbehovData(fødselsnummer = fødselsnummer),
        )
        assertNotNull(hendelserInspektør.hendelseOrNull<VedtaksperiodeGodkjentAutomatisk>())
    }

    @Test
    fun `automatisk avvisning medfører VedtaksperiodeAvvistAutomatisk`() {
        mediator.automatiskAvvisning(
            behov = godkjenningsbehovData(fødselsnummer = fødselsnummer),
            outbox = outbox,
            begrunnelser = emptyList(),
        )
        assertUtgåendeHendelse<VedtaksperiodeAvvistAutomatisk>()
    }

    @Test
    fun `automatisk avvisning skal opprette opptegnelse`() {
        mediator.automatiskAvvisning(
            begrunnelser = listOf("foo"),
            outbox = outbox,
            behov = godkjenningsbehovData(fødselsnummer = fødselsnummer),
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(
            commandContext = commandContext,
            behov = godkjenningsbehovData(fødselsnummer = fødselsnummer),
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() {
        val opptegnelser = sessionContext.opptegnelseRepository.finnAlleForPersonEtter(Sekvensnummer(0), Identitetsnummer.fraString(fødselsnummer))
        assertEquals(1, opptegnelser.size)
        assertEquals(Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV, opptegnelser.single().type)
    }
}
