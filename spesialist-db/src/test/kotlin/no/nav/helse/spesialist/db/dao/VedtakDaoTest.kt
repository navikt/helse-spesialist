package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtakDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val organisasjonsnummer =
        when (val id = arbeidsgiver.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
    private val vedtaksperiodeId = UUID.randomUUID()

    @Test
    fun `lagre og finn vedtaksperiode`() {
        sessionOf(dataSource).use {
            it.transaction {
                PgVedtakDao(it).lagreVedtaksperiode(
                    fødselsnummer = person.id.value,
                    vedtaksperiodeDto =
                        VedtaksperiodeDto(
                            organisasjonsnummer = organisasjonsnummer,
                            vedtaksperiodeId = vedtaksperiodeId,
                            forkastet = false,
                            behandlinger =
                                listOf(
                                    BehandlingDto(
                                        id = UUID.randomUUID(),
                                        vedtaksperiodeId = vedtaksperiodeId,
                                        utbetalingId = null,
                                        spleisBehandlingId = UUID.randomUUID(),
                                        skjæringstidspunkt = 1 jan 2018,
                                        fom = 1 jan 2018,
                                        tom = 31 jan 2018,
                                        tilstand = TilstandDto.VidereBehandlingAvklares,
                                        tags = emptyList(),
                                        vedtakBegrunnelse = null,
                                        varsler = emptyList(),
                                        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                                    ),
                                ),
                        ),
                )
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    PgVedtakDao(it).finnVedtaksperiode(vedtaksperiodeId)
                }
            }
        assertNotNull(vedtaksperiode)
        assertEquals(vedtaksperiodeId, vedtaksperiode?.vedtaksperiodeId)
        assertEquals(organisasjonsnummer, vedtaksperiode?.organisasjonsnummer)
        assertEquals(false, vedtaksperiode?.forkastet)
    }

    @Test
    fun `finn forkastet vedtaksperiode`() {
        sessionOf(dataSource).use {
            it.transaction {
                PgVedtakDao(it).lagreVedtaksperiode(
                    fødselsnummer = person.id.value,
                    vedtaksperiodeDto =
                        VedtaksperiodeDto(
                            organisasjonsnummer = organisasjonsnummer,
                            vedtaksperiodeId = vedtaksperiodeId,
                            forkastet = true,
                            behandlinger =
                                listOf(
                                    BehandlingDto(
                                        id = UUID.randomUUID(),
                                        vedtaksperiodeId = vedtaksperiodeId,
                                        utbetalingId = null,
                                        spleisBehandlingId = UUID.randomUUID(),
                                        skjæringstidspunkt = 1 jan 2018,
                                        fom = 1 jan 2018,
                                        tom = 31 jan 2018,
                                        tilstand = TilstandDto.VidereBehandlingAvklares,
                                        tags = emptyList(),
                                        vedtakBegrunnelse = null,
                                        varsler = emptyList(),
                                        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                                    ),
                                ),
                        ),
                )
            }
        }
        val vedtaksperiode =
            sessionOf(dataSource).use {
                it.transaction {
                    PgVedtakDao(it).finnVedtaksperiode(vedtaksperiodeId)
                }
            }
        assertNotNull(vedtaksperiode)
        assertEquals(vedtaksperiodeId, vedtaksperiode?.vedtaksperiodeId)
        assertEquals(organisasjonsnummer, vedtaksperiode?.organisasjonsnummer)
        assertEquals(true, vedtaksperiode?.forkastet)
    }

    @Test
    fun `lagrer og leser vedtaksperiodetype hvis den er satt`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver, periodetype = Periodetype.FØRSTEGANGSBEHANDLING, inntektskilde = Inntektskilde.EN_ARBEIDSGIVER)
        opprettBehandling(vedtaksperiode)
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, finnVedtaksperiodetype(vedtaksperiode.id.value))
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, vedtakDao.finnInntektskilde(vedtaksperiode.id.value))
    }

    @Test
    fun `oppretter innslag i koblingstabellen`() {
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val utbetalingId = UUID.randomUUID()
        val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        godkjenningsbehov(oppgave.godkjenningsbehovId, fødselsnummer = person.id.value)
        vedtakDao.opprettKobling(vedtaksperiodeId, oppgave.godkjenningsbehovId)
        assertEquals(vedtaksperiodeId, finnKobling(oppgave.godkjenningsbehovId))
    }

    @Test
    fun `ikke automatisk godkjent dersom det ikke finnes innslag i db`() {
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val utbetalingId = UUID.randomUUID()
        val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        assertFalse(vedtakDao.erAutomatiskGodkjent(oppgave.utbetalingId))
    }

    @Test
    fun `ikke automatisk godkjent dersom innslag i db sier false`() {
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val utbetalingId = UUID.randomUUID()
        val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        godkjenningsbehov(oppgave.godkjenningsbehovId, fødselsnummer = person.id.value)
        nyttAutomatiseringsinnslag(false, vedtaksperiode.id.value, utbetalingId, hendelseId = oppgave.godkjenningsbehovId)
        assertFalse(vedtakDao.erAutomatiskGodkjent(oppgave.utbetalingId))
    }

    @Test
    fun `automatisk godkjent dersom innslag i db sier true`() {
        val utbetalingId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
        godkjenningsbehov(hendelseId, fødselsnummer = person.id.value)
        nyttAutomatiseringsinnslag(true, vedtaksperiode.id.value, utbetalingId, hendelseId)
        assertTrue(vedtakDao.erAutomatiskGodkjent(utbetalingId))
    }

    private fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype =
        sessionOf(dataSource).use {
            it.run(
                queryOf("SELECT type FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = ?)", vedtaksperiodeId)
                    .map { row -> enumValueOf<Periodetype>(row.string("type")) }
                    .asSingle,
            )!!
        }

    private fun finnKobling(hendelseId: UUID) =
        sessionOf(dataSource).use {
            it.run(
                queryOf("SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                    .map { row -> row.uuid("vedtaksperiode_id") }
                    .asSingle,
            )
        }
}
