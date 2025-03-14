package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.testhjelp.jan
import no.nav.helse.spesialist.testhjelp.lagAktørId
import no.nav.helse.spesialist.testhjelp.lagFødselsnummer
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PgLegacyBehandlingDaoTest : AbstractDBIntegrationTest() {

    private val generasjonDao = daos.generasjonDao

    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()

        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId1)
        opprettBehandling(vedtaksperiodeId1, generasjonId1)
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2)
        opprettBehandling(vedtaksperiodeId2, generasjonId2)
        opprettBehandling(vedtaksperiodeId2, generasjonId3)

        val vedtaksperiodeIder = generasjonDao.finnVedtaksperiodeIderFor(FNR)
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiodeId1, vedtaksperiodeId2)))
    }

    @Test
    fun `finner vedtaksperiodeider kun for aktuell person`() {
        val person1 = lagFødselsnummer()
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val vedtaksperiodeId1 = UUID.randomUUID()

        val person2 = lagFødselsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val vedtaksperiodeId2 = UUID.randomUUID()

        opprettPerson(fødselsnummer = person1, lagAktørId())
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer1)
        opprettVedtaksperiode(
            fødselsnummer = person1,
            organisasjonsnummer = organisasjonsnummer1,
            vedtaksperiodeId = vedtaksperiodeId1
        )

        opprettPerson(fødselsnummer = person2, lagAktørId())
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer2)
        opprettVedtaksperiode(
            fødselsnummer = person2,
            organisasjonsnummer = organisasjonsnummer2,
            vedtaksperiodeId = vedtaksperiodeId2
        )

        val vedtaksperiodeIderPerson1 = generasjonDao.finnVedtaksperiodeIderFor(person1)

        val vedtaksperiodeIderPerson2 = generasjonDao.finnVedtaksperiodeIderFor(person2)
        assertEquals(1, vedtaksperiodeIderPerson1.size)
        assertEquals(1, vedtaksperiodeIderPerson2.size)
        assertTrue(vedtaksperiodeIderPerson1.containsAll(setOf(vedtaksperiodeId1)))
        assertTrue(vedtaksperiodeIderPerson2.containsAll(setOf(vedtaksperiodeId2)))
    }

    @Test
    fun `lagre og finne generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val varsel = VarselDto(
            id = UUID.randomUUID(),
            varselkode = "SB_EX_1",
            opprettet = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId,
            status = VarselStatusDto.AKTIV
        )
        generasjonDao.lagreGenerasjon(
            BehandlingDto(
                id = generasjonId,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = 1 jan 2018,
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                tilstand = TilstandDto.KlarTilBehandling,
                tags = listOf("TAG"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.AVSLAG, "En begrunnelse"),
                varsler = listOf(varsel),
            )

        )
        val funnet = generasjonDao.finnGenerasjoner(vedtaksperiodeId)
        assertEquals(1, funnet.size)
        assertEquals(
            BehandlingDto(
                id = generasjonId,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = 1 jan 2018,
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                tilstand = TilstandDto.KlarTilBehandling,
                tags = listOf("TAG"),
                vedtakBegrunnelse = null,
                varsler = listOf(varsel),
            ),
            funnet.single()
        )
    }
}
