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
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.util.UUID

internal class VedtakDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `lagre vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId)
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, personId, arbeidsgiverId, false)
    }

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
                                            UUID.randomUUID(),
                                            VEDTAKSPERIODE,
                                            null,
                                            UUID.randomUUID(),
                                            1.januar,
                                            1.januar,
                                            31.januar,
                                            TilstandDto.VidereBehandlingAvklares,
                                            emptyList(),
                                            emptyList(),
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
                                            UUID.randomUUID(),
                                            VEDTAKSPERIODE,
                                            null,
                                            UUID.randomUUID(),
                                            1.januar,
                                            1.januar,
                                            31.januar,
                                            TilstandDto.VidereBehandlingAvklares,
                                            emptyList(),
                                            emptyList(),
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
    fun `opprette duplikat vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId)
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        assertThrows<SQLException> {
            vedtakDao.opprett(
                VEDTAKSPERIODE,
                nyFom,
                nyTom,
                personId,
                arbeidsgiverId,
            )
        }
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

    private fun vedtak(fødselsnummer: String = FNR) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                """
                SELECT vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, forkastet
                FROM vedtak
                JOIN person p on vedtak.person_ref = p.id
                WHERE fodselsnummer = :foedselsnummer
                """.trimIndent()
            it.run(
                queryOf(query, mapOf("foedselsnummer" to fødselsnummer.toLong())).map { row ->
                    Vedtak(
                        vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                        personRef = row.long("person_ref"),
                        arbeidsgiverRef = row.long("arbeidsgiver_ref"),
                        forkastet = row.boolean("forkastet"),
                    )
                }.asList,
            )
        }

    private fun opprettSpesialsak(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = """INSERT INTO spesialsak(vedtaksperiode_id) VALUES(?)"""
        sessionOf(dataSource).use {
            it.run(queryOf(query, vedtaksperiodeId).asExecute)
        }
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val personRef: Long,
        private val arbeidsgiverRef: Long,
        private val forkastet: Boolean,
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetPersonRef: Long,
            forventetArbeidsgiverRef: Long,
            forventetForkastet: Boolean,
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetForkastet, forkastet)
        }
    }
}
