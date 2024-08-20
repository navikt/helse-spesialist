package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.GenerasjonDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.TilstandDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtakDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre og finn vedtaksperiode`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        sessionOf(dataSource).use {
            it.transaction {
                with(vedtakDao) {
                    it.lagreVedtaksperiode(
                        fødselsnummer = FNR,
                        vedtaksperiodeDto =
                            VedtaksperiodeDto(
                                organisasjonsnummer = ORGNUMMER,
                                vedtaksperiodeId = VEDTAKSPERIODE,
                                forkastet = false,
                                generasjoner =
                                    listOf(
                                        GenerasjonDto(
                                            id = UUID.randomUUID(),
                                            vedtaksperiodeId = VEDTAKSPERIODE,
                                            utbetalingId = null,
                                            spleisBehandlingId = UUID.randomUUID(),
                                            skjæringstidspunkt = 1.januar,
                                            fom = 1.januar,
                                            tom = 31.januar,
                                            tilstand = TilstandDto.VidereBehandlingAvklares,
                                            tags = emptyList(),
                                            varsler = emptyList(),
                                            avslag = null
                                        ),
                                    ),
                            ),
                    )
                }
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    with(vedtakDao) {
                        it.finnVedtaksperiode(VEDTAKSPERIODE)
                    }
                }
            }
        assertNotNull(vedtaksperiode)
        assertEquals(VEDTAKSPERIODE, vedtaksperiode?.vedtaksperiodeId)
        assertEquals(ORGNUMMER, vedtaksperiode?.organisasjonsnummer)
        assertEquals(false, vedtaksperiode?.forkastet)
    }

    @Test
    fun `finn forkastet vedtaksperiode`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        sessionOf(dataSource).use {
            it.transaction {
                with(vedtakDao) {
                    it.lagreVedtaksperiode(
                        fødselsnummer = FNR,
                        vedtaksperiodeDto =
                            VedtaksperiodeDto(
                                organisasjonsnummer = ORGNUMMER,
                                vedtaksperiodeId = VEDTAKSPERIODE,
                                forkastet = true,
                                generasjoner =
                                    listOf(
                                        GenerasjonDto(
                                            id = UUID.randomUUID(),
                                            vedtaksperiodeId = VEDTAKSPERIODE,
                                            utbetalingId = null,
                                            spleisBehandlingId = UUID.randomUUID(),
                                            skjæringstidspunkt = 1.januar,
                                            fom = 1.januar,
                                            tom = 31.januar,
                                            tilstand = TilstandDto.VidereBehandlingAvklares,
                                            tags = emptyList(),
                                            varsler = emptyList(),
                                            avslag = null
                                        ),
                                    ),
                            ),
                    )
                }
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    with(vedtakDao) {
                        it.finnVedtaksperiode(VEDTAKSPERIODE)
                    }
                }
            }
        assertNotNull(vedtaksperiode)
        assertEquals(VEDTAKSPERIODE, vedtaksperiode?.vedtaksperiodeId)
        assertEquals(ORGNUMMER, vedtaksperiode?.organisasjonsnummer)
        assertEquals(true, vedtaksperiode?.forkastet)
    }

    @Test
    fun `lagrer og leser vedtaksperiodetype hvis den er satt`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val vedtaksperiodetype = Periodetype.FØRSTEGANGSBEHANDLING
        val inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        vedtakDao.leggTilVedtaksperiodetype(VEDTAKSPERIODE, vedtaksperiodetype, inntektskilde)
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, vedtakDao.finnVedtaksperiodetype(VEDTAKSPERIODE))
        assertEquals(inntektskilde, vedtakDao.finnInntektskilde(VEDTAKSPERIODE))
    }

    @Test
    fun `oppretter innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(VEDTAKSPERIODE, finnKobling(HENDELSE_ID))
    }

    @Test
    fun `fjerner innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(VEDTAKSPERIODE, finnKobling(HENDELSE_ID))

        vedtakDao.fjernKobling(VEDTAKSPERIODE, HENDELSE_ID)

        assertNull(finnKobling(HENDELSE_ID))
    }

    @Test
    fun `ikke automatisk godkjent dersom det ikke finnes innslag i db`() {
        nyPerson()
        assertFalse(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    @Test
    fun `ikke automatisk godkjent dersom innslag i db sier false`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        nyttAutomatiseringsinnslag(false)
        assertFalse(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    @Test
    fun `automatisk godkjent dersom innslag i db sier true`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        nyttAutomatiseringsinnslag(true)
        assertTrue(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    @Test
    fun `kan markere vedtaksperiode som forkastet`() {
        nyPerson()
        vedtakDao.markerForkastet(VEDTAKSPERIODE, HENDELSE_ID)
        assertForkastet(VEDTAKSPERIODE, HENDELSE_ID)
    }

    @Test
    fun `Finner orgnummer med vedtaksperiodeId`() {
        nyPerson()
        assertEquals(ORGNUMMER, vedtakDao.finnOrgnummer(VEDTAKSPERIODE))
    }

    @Test
    fun spesialsak() {
        nyPerson()
        opprettSpesialsak(VEDTAKSPERIODE)
        assertTrue(vedtakDao.erSpesialsak(VEDTAKSPERIODE))
    }

    @Test
    fun `ikke spesialsak`() {
        nyPerson()
        assertFalse(vedtakDao.erSpesialsak(VEDTAKSPERIODE))
    }

    @Test
    fun `sett spesialsak ferdigbehandlet`() {
        nyPerson()
        opprettSpesialsak(VEDTAKSPERIODE)
        assertTrue(vedtakDao.erSpesialsak(VEDTAKSPERIODE))
        vedtakDao.spesialsakFerdigbehandlet(VEDTAKSPERIODE)
        assertFalse(vedtakDao.erSpesialsak(VEDTAKSPERIODE))
    }

    private fun assertForkastet(
        vedtaksperiodeId: UUID,
        forventetHendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT forkastet, forkastet_av_hendelse, forkastet_tidspunkt FROM vedtak WHERE vedtaksperiode_id = ?"
        val respons =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(query, vedtaksperiodeId).map {
                        Triple(
                            it.boolean("forkastet"),
                            it.uuidOrNull("forkastet_av_hendelse"),
                            it.localDateTimeOrNull("forkastet_tidspunkt"),
                        )
                    }.asSingle,
                )
            }
        assertNotNull(respons)
        assertEquals(true, respons?.first)
        assertEquals(forventetHendelseId, respons?.second)
        assertNotNull(respons?.third)
    }

    private fun finnKobling(hendelseId: UUID) =
        sessionOf(dataSource).use {
            it.run(
                queryOf("SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                    .map { row -> row.uuid("vedtaksperiode_id") }.asSingle,
            )
        }

    private fun opprettSpesialsak(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = """INSERT INTO spesialsak(vedtaksperiode_id) VALUES(?)"""
        sessionOf(dataSource).use {
            it.run(queryOf(query, vedtaksperiodeId).asExecute)
        }
    }
}
