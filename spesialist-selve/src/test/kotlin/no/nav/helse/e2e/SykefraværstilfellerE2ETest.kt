package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfellerE2ETest : AbstractE2ETest() {

    @Test
    fun `Oppdaterer sykefraværstilfelle`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterSykefraværstilfeller(tilfeller = listOf(tilfelle()))

        val sykefraværstilfelleTriple = finnFørsteGenerasjon(VEDTAKSPERIODE_ID)
        assertEquals(1.januar, sykefraværstilfelleTriple.first)
        assertEquals(1.januar, sykefraværstilfelleTriple.second)
        assertEquals(5.januar, sykefraværstilfelleTriple.third)
    }

    @Test
    fun `Oppdaterer sykefraværstilfeller - flere perioder i samme tilfelle`() {
        val vedtaksperiodeId1 = testperson.vedtaksperiodeId1
        val vedtaksperiodeId2 = testperson.vedtaksperiodeId2
        val revurdertUtbetalingId = testperson.utbetalingId2

        nyttVedtak(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(periodeFom = 1.januar, periodeTom = 5.januar))
        forlengVedtak(6.januar, 10.januar, skjæringstidspunkt = 1.januar, vedtaksperiodeId = vedtaksperiodeId2)
        håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId1)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId1, utbetalingId = revurdertUtbetalingId)
        håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId2)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId2, utbetalingId = revurdertUtbetalingId)

        håndterSykefraværstilfeller(
            tilfeller = listOf(
                tilfelle(
                    skjæringstidspunkt = 15.januar,
                    vedtaksperioder = listOf(
                        periode(vedtaksperiodeId = vedtaksperiodeId1, fom = 15.januar, tom = 20.januar),
                        periode(vedtaksperiodeId = vedtaksperiodeId2, fom = 21.januar, tom = 25.januar)
                    )
                )
            )
        )

        finnFørsteGenerasjon(vedtaksperiodeId1).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(1.januar, skjæringstidspunkt)
            assertEquals(1.januar, fom)
            assertEquals(5.januar, tom)
        }
        finnSisteGenerasjon(vedtaksperiodeId1).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(15.januar, skjæringstidspunkt)
            assertEquals(15.januar, fom)
            assertEquals(20.januar, tom)
        }
        finnFørsteGenerasjon(vedtaksperiodeId2).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(1.januar, skjæringstidspunkt)
            assertEquals(6.januar, fom)
            assertEquals(10.januar, tom)
        }
        finnSisteGenerasjon(vedtaksperiodeId2).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(15.januar, skjæringstidspunkt)
            assertEquals(21.januar, fom)
            assertEquals(25.januar, tom)
        }
    }

    @Test
    fun `Oppdaterer sykefraværstilfeller for flere tilfeller`() {
        val vedtaksperiodeId1 = testperson.vedtaksperiodeId1
        val vedtaksperiodeId2 = testperson.vedtaksperiodeId2

        nyttVedtak(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(periodeFom = 1.januar, periodeTom = 5.januar))
        nyttVedtak(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(vedtaksperiodeId = vedtaksperiodeId2, periodeFom = 10.januar, periodeTom = 15.januar, skjæringstidspunkt = 10.januar, utbetalingId = UUID.randomUUID()), harOppdatertMetadata = true)

        håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId1)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId1, utbetalingId = UUID.randomUUID())
        håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId2)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId2, utbetalingId = UUID.randomUUID())

        håndterSykefraværstilfeller(
            tilfeller = listOf(
                tilfelle(
                    skjæringstidspunkt = 1.januar,
                    vedtaksperioder = listOf(
                        periode(vedtaksperiodeId = vedtaksperiodeId1, fom = 1.januar, tom = 6.januar),
                    )
                ),
                tilfelle(
                    skjæringstidspunkt = 11.januar,
                    vedtaksperioder = listOf(
                        periode(vedtaksperiodeId = vedtaksperiodeId2, fom = 11.januar, tom = 26.januar)
                    )
                )
            )
        )

        finnFørsteGenerasjon(vedtaksperiodeId1).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(1.januar, skjæringstidspunkt)
            assertEquals(1.januar, fom)
            assertEquals(5.januar, tom)
        }
        finnSisteGenerasjon(vedtaksperiodeId1).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(1.januar, skjæringstidspunkt)
            assertEquals(1.januar, fom)
            assertEquals(6.januar, tom)
        }
        finnFørsteGenerasjon(vedtaksperiodeId2).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(10.januar, skjæringstidspunkt)
            assertEquals(10.januar ,fom)
            assertEquals(15.januar ,tom)
        }
        finnSisteGenerasjon(vedtaksperiodeId2).also { (skjæringstidspunkt, fom, tom) ->
            assertEquals(11.januar, skjæringstidspunkt)
            assertEquals(11.januar, fom)
            assertEquals(26.januar, tom)
        }
    }

    private fun finnFørsteGenerasjon(vedtaksperiodeId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT skjæringstidspunkt,fom,tom 
                FROM selve_vedtaksperiode_generasjon 
                WHERE vedtaksperiode_id = :vedtaksperiode_id ORDER BY id
            """
        requireNotNull(session.run(
            queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId))
                .map {
                    Triple(
                        it.localDateOrNull("skjæringstidspunkt"),
                        it.localDateOrNull("fom"),
                        it.localDateOrNull("tom")
                    )
                }.asSingle
        ))
    }

    private fun finnSisteGenerasjon(vedtaksperiodeId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT skjæringstidspunkt,fom,tom 
                FROM selve_vedtaksperiode_generasjon 
                WHERE vedtaksperiode_id = :vedtaksperiode_id ORDER BY id DESC
            """
        requireNotNull(session.run(
            queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId))
                .map {
                    Triple(
                        it.localDateOrNull("skjæringstidspunkt"),
                        it.localDateOrNull("fom"),
                        it.localDateOrNull("tom")
                    )
                }.asSingle
        ))
    }

    private fun tilfelle(
        skjæringstidspunkt: LocalDate = 1.januar,
        vedtaksperioder: List<Map<String, Any>> = listOf(periode()),
    ) = mapOf(
        "dato" to skjæringstidspunkt,
        "perioder" to vedtaksperioder,
    )

    private fun periode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 5.januar,
    ): Map<String, Any> = mapOf(
        "vedtaksperiodeId" to vedtaksperiodeId,
        "organisasjonsnummer" to "zzzzzzzzzzz",
        "fom" to fom,
        "tom" to tom,
    )

}
