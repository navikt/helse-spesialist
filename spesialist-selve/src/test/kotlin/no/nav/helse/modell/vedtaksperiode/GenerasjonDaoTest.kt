package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import kotliquery.sessionOf
import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.AvslagDto
import no.nav.helse.modell.vedtak.AvslagstypeDto
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {
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
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId3)

        val vedtaksperiodeIder =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnVedtaksperiodeIderFor(FNR)
                    }
                }
            }
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

        val vedtaksperiodeIderPerson1 =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnVedtaksperiodeIderFor(person1)
                    }
                }
            }

        val vedtaksperiodeIderPerson2 =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnVedtaksperiodeIderFor(person2)
                    }
                }
            }
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
        val avslag = AvslagDto(AvslagstypeDto.AVSLAG, begrunnelse = "En begrunnelse")
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                with(generasjonDao) {
                    tx.lagreGenerasjon(
                        GenerasjonDto(
                            id = generasjonId,
                            vedtaksperiodeId = vedtaksperiodeId,
                            utbetalingId = utbetalingId,
                            spleisBehandlingId = spleisBehandlingId,
                            skjæringstidspunkt = 1.januar,
                            fom = 1.januar,
                            tom = 31.januar,
                            tilstand = TilstandDto.KlarTilBehandling,
                            tags = listOf("TAG"),
                            varsler = listOf(varsel),
                            avslag = avslag
                        )
                    )
                }
            }
        }
        val funnet = sessionOf(dataSource).use { session ->
            session.transaction {
                with(generasjonDao) {
                    it.finnGenerasjoner(vedtaksperiodeId)
                }
            }
        }
        assertEquals(1, funnet.size)
        assertEquals(
            GenerasjonDto(
                id = generasjonId,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = 1.januar,
                fom = 1.januar,
                tom = 31.januar,
                tilstand = TilstandDto.KlarTilBehandling,
                tags = listOf("TAG"),
                varsler = listOf(varsel),
                avslag = null //Lagres ikke enda
            ),
            funnet.single()
        )
    }
}
