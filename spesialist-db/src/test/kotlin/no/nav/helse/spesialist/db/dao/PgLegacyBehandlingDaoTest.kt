package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PgLegacyBehandlingDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val generasjonDao = daos.legacyBehandlingDao

    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode1)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode2)

        val vedtaksperiodeIder = generasjonDao.finnVedtaksperiodeIderFor(person.id.value)
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiode1.id.value, vedtaksperiode2.id.value)))
    }

    @Test
    fun `finner vedtaksperiodeider kun for aktuell person`() {
        val person1 = person
        val arbeidsgiver1 = arbeidsgiver

        val person2 = opprettPerson()
        val arbeidsgiver2 = opprettArbeidsgiver()

        val vedtaksperiode1 = opprettVedtaksperiode(person1, arbeidsgiver1)
        opprettBehandling(vedtaksperiode1)
        val vedtaksperiode2 = opprettVedtaksperiode(person2, arbeidsgiver2)
        opprettBehandling(vedtaksperiode2)

        val vedtaksperiodeIderPerson1 = generasjonDao.finnVedtaksperiodeIderFor(person1.id.value)

        val vedtaksperiodeIderPerson2 = generasjonDao.finnVedtaksperiodeIderFor(person2.id.value)
        assertEquals(1, vedtaksperiodeIderPerson1.size)
        assertEquals(1, vedtaksperiodeIderPerson2.size)
        assertTrue(vedtaksperiodeIderPerson1.containsAll(setOf(vedtaksperiode1.id.value)))
        assertTrue(vedtaksperiodeIderPerson2.containsAll(setOf(vedtaksperiode2.id.value)))
    }

    @Test
    fun `lagre og finne generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val varsel =
            VarselDto(
                id = UUID.randomUUID(),
                varselkode = "SB_EX_1",
                opprettet = LocalDateTime.now(),
                vedtaksperiodeId = vedtaksperiodeId,
                status = VarselStatusDto.AKTIV,
            )
        generasjonDao.finnLegacyBehandling(
            BehandlingDto(
                id = behandlingId,
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
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ),
        )
        val funnet = generasjonDao.finnLegacyBehandlinger(vedtaksperiodeId)
        assertEquals(1, funnet.size)
        assertEquals(
            BehandlingDto(
                id = behandlingId,
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
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ),
            funnet.single(),
        )
    }
}
