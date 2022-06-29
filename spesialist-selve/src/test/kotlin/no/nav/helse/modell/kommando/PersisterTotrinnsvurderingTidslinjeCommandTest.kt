package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.overstyring.OverstyringType
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersisterTotrinnsvurderingTidslinjeCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val ORGNR = "123456789"
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = 1.januar,
                type = Dagtype.Sykedag,
                grad = 100
            )
        )
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val overstyrtVedtaksperiodeDao = mockk<OverstyrtVedtaksperiodeDao>(relaxed = true)
    private val automatiseringDao = mockk<AutomatiseringDao>(relaxed = true)

    private val context = CommandContext(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, overstyrtVedtaksperiodeDao)
    }

    @Test
    fun `lagrer overstyrt vedtaksperiode hvis vi finner vedtaksperiode som inneholder første overstyrt dag`() {
        every { oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato) }.returns(VEDTAKSPERIODE_ID)

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer ikke overstyrt vedtaksperiode hvis vi ikke finner vedtaksperiode som inneholder første overstyrt dag`() {
        every { oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(any(), any(), OVERSTYRTE_DAGER.first().dato) }.returns(null)
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(any(), any()) }.returns(null)

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao)
        command.execute(context)

        verify(exactly = 0) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(any(), any())
        }
    }

    @Test
    fun `Hopper ut tidlig hvis overstyrte dager er tom`() {
        every { oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(any(), any(), OVERSTYRTE_DAGER.first().dato) }.returns(null)

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            listOf(),
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao)
        command.execute(context)

        verify(exactly = 0) {
            oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(any(), any(), any())
        }
        verify(exactly = 0) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(any(), any())
        }
    }
}