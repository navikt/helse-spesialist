package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringType
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersisterTotrinnsvurderingArbeidsforholdCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private val SKJÆRINGSTIDSPUNKT = 1.januar
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val overstyrtVedtaksperiodeDao = mockk<OverstyrtVedtaksperiodeDao>(relaxed = true)

    private val context = CommandContext(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, overstyrtVedtaksperiodeDao)
    }

    @Test
    fun `lagrer overstyrt vedtaksperiode hvis vi finner aktiv vedtaksperiode for skjæringstidspunkt`() {
        every { oppgaveDao.finnAktivVedtaksperiodeId(FNR) }.returns(VEDTAKSPERIODE_ID)

        val command = PersisterTotrinnsvurderingArbeidsforholdCommand(
            FNR,
            SKJÆRINGSTIDSPUNKT,
            oppgaveDao,
            overstyrtVedtaksperiodeDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Arbeidsforhold)
        }
    }

    @Test
    fun `lagrer ikke overstyrt vedtaksperiode hvis vi ikke finner aktiv vedtaksperiode for skjæringstidspunkt`() {
        every { oppgaveDao.finnAktivVedtaksperiodeId(any()) }.returns(null)

        val command = PersisterTotrinnsvurderingArbeidsforholdCommand(
            FNR,
            SKJÆRINGSTIDSPUNKT,
            oppgaveDao,
            overstyrtVedtaksperiodeDao)
        command.execute(context)

        verify(exactly = 0) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(any(), any())
        }
    }
}