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
    fun `Kan ikke opprette et sykefraværstilfelle uten å ha en generasjon`() {
        assertThrows<IllegalStateException> {
            sykefraværstilfelle(gjeldendeGenerasjoner = emptyList())
        }
    }

    @Test
    fun `har ikke aktive varsler`() {
        val gjeldendeGenerasjon1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeGenerasjon2 = legacyBehandling(UUID.randomUUID())
        assertFalse(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `har ikke aktive varsler når generasjonene har utbetalingId men ikke fom`() {
        val gjeldendeGenerasjon1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeGenerasjon2 = legacyBehandling(UUID.randomUUID())
        val utbetalingId = UUID.randomUUID()
        gjeldendeGenerasjon1.håndterNyUtbetaling(utbetalingId)
        gjeldendeGenerasjon2.håndterNyUtbetaling(utbetalingId)
        assertFalse(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `har aktive varsler`() {
        val vedtaksperiodeId2 = UUID.randomUUID()
        val gjeldendeGenerasjon1 = legacyBehandling(UUID.randomUUID())
        val gjeldendeGenerasjon2 = legacyBehandling(vedtaksperiodeId2)
        gjeldendeGenerasjon2.håndterNyttVarsel(
            LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2),
        )
        assertTrue(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28 feb 2018))
    }

    @Test
    fun `thrower hvis generasjon ikke finnes`() {
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
        gjeldendeGenerasjoner: List<LegacyBehandling> = listOf(legacyBehandling()),
    ) = Sykefraværstilfelle(
        fødselsnummer,
        skjæringstidspunkt,
        gjeldendeGenerasjoner,
    )
}
