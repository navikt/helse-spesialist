package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.forhindrerAutomatisering
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class SykefraværstilfelleTest {
    @Test
    fun `Kan ikke opprette et sykefraværstilfelle uten å ha en behandling`() {
        assertThrows<IllegalStateException> {
            sykefraværstilfelle(gjeldendeBehandlinger = emptyList())
        }
    }

    @Test
    fun `har ikke aktive varsler`() {
        val gjeldendeBehandling1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeBehandling2 = legacyBehandling(UUID.randomUUID())
        assertFalse(listOf(gjeldendeBehandling1, gjeldendeBehandling2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `har ikke aktive varsler når behandlingene har utbetalingId men ikke fom`() {
        val gjeldendeBehandling1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeBehandling2 = legacyBehandling(UUID.randomUUID())
        val utbetalingId = UUID.randomUUID()
        gjeldendeBehandling1.håndterNyUtbetaling(utbetalingId)
        gjeldendeBehandling2.håndterNyUtbetaling(utbetalingId)
        assertFalse(listOf(gjeldendeBehandling1, gjeldendeBehandling2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `har aktive varsler`() {
        val vedtaksperiodeId2 = UUID.randomUUID()
        val gjeldendeBehandling1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeBehandling2 = legacyBehandling(vedtaksperiodeId2)
        gjeldendeBehandling2.håndterNyttVarsel(
            LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2),
        )
        assertTrue(listOf(gjeldendeBehandling1, gjeldendeBehandling2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `thrower hvis behandling ikke finnes`() {
        assertThrows<IllegalArgumentException> { sykefraværstilfelle().haster(UUID.randomUUID()) }
    }

    private fun legacyBehandling(vedtaksperiodeId: UUID = UUID.randomUUID()) =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
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
