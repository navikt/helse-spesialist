package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.PgVedtakDao
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.DatabaseIntegrationTest
import no.nav.helse.spesialist.db.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtakDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre og finn vedtaksperiode`() {
        opprettPerson()
        opprettArbeidsgiver()
        sessionOf(dataSource).use {
            it.transaction {
                PgVedtakDao(it).lagreVedtaksperiode(
                        fødselsnummer = FNR,
                        vedtaksperiodeDto =
                            VedtaksperiodeDto(
                                organisasjonsnummer = ORGNUMMER,
                                vedtaksperiodeId = VEDTAKSPERIODE,
                                forkastet = false,
                                behandlinger =
                                    listOf(
                                        BehandlingDto(
                                            id = UUID.randomUUID(),
                                            vedtaksperiodeId = VEDTAKSPERIODE,
                                            utbetalingId = null,
                                            spleisBehandlingId = UUID.randomUUID(),
                                            skjæringstidspunkt = 1 jan 2018,
                                            fom = 1 jan 2018,
                                            tom = 31 jan 2018,
                                            tilstand = TilstandDto.VidereBehandlingAvklares,
                                            tags = emptyList(),
                                            vedtakBegrunnelse = null,
                                            varsler = emptyList(),
                                        ),
                                    ),
                            ),
                    )
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    PgVedtakDao(it).finnVedtaksperiode(VEDTAKSPERIODE)
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
        sessionOf(dataSource).use {
            it.transaction {
                PgVedtakDao(it).lagreVedtaksperiode(
                        fødselsnummer = FNR,
                        vedtaksperiodeDto =
                            VedtaksperiodeDto(
                                organisasjonsnummer = ORGNUMMER,
                                vedtaksperiodeId = VEDTAKSPERIODE,
                                forkastet = true,
                                behandlinger =
                                    listOf(
                                        BehandlingDto(
                                            id = UUID.randomUUID(),
                                            vedtaksperiodeId = VEDTAKSPERIODE,
                                            utbetalingId = null,
                                            spleisBehandlingId = UUID.randomUUID(),
                                            skjæringstidspunkt = 1 jan 2018,
                                            fom = 1 jan 2018,
                                            tom = 31 jan 2018,
                                            tilstand = TilstandDto.VidereBehandlingAvklares,
                                            tags = emptyList(),
                                            vedtakBegrunnelse = null,
                                            varsler = emptyList(),
                                        ),
                                    ),
                            ),
                    )
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    PgVedtakDao(it).finnVedtaksperiode(VEDTAKSPERIODE)
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
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, finnVedtaksperiodetype(VEDTAKSPERIODE))
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
    fun `Finner orgnummer med vedtaksperiodeId`() {
        nyPerson()
        assertEquals(ORGNUMMER, vedtakDao.finnOrganisasjonsnummer(VEDTAKSPERIODE))
    }

    private fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype {
        return sessionOf(dataSource).use {
            it.run(
                queryOf("SELECT type FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)", vedtaksperiodeId)
                    .map { row -> enumValueOf<Periodetype>(row.string("type")) }.asSingle,
            )!!
        }
    }

    private fun finnKobling(hendelseId: UUID) =
        sessionOf(dataSource).use {
            it.run(
                queryOf("SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                    .map { row -> row.uuid("vedtaksperiode_id") }.asSingle,
            )
        }
}
