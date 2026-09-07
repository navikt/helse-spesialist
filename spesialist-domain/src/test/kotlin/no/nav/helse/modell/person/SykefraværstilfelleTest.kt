package no.nav.helse.modell.person

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class SykefraværstilfelleTest {
    @Test
    fun `Kan ikke opprette et sykefraværstilfelle uten å ha en behandling`() {
        assertThrows<IllegalStateException> {
            sykefraværstilfelle(gjeldendeBehandlinger = emptyList())
        }
    }

    private fun legacyBehandling(vedtaksperiodeId: UUID = UUID.randomUUID()) =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        )

    private fun sykefraværstilfelle(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1 jan 2018,
        gjeldendeBehandlinger: List<LegacyBehandling> = listOf(legacyBehandling()),
    ) = Sykefraværstilfelle(
        fødselsnummer,
        skjæringstidspunkt,
        gjeldendeBehandlinger,
    )
}
