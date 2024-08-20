package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import kotliquery.sessionOf
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class GenerasjonRepositoryTest: DatabaseIntegrationTest() {

    private val generasjonRepository = GenerasjonRepository(dataSource)

    @Test
    fun `lagrer og finner liste av vedtaksperioder`() {
        val person1 = lagFødselsnummer()
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        opprettPerson(fødselsnummer = person1, lagAktørId())
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer1)

        val vedtaksperioder = listOf(
            VedtaksperiodeDto(
                organisasjonsnummer = organisasjonsnummer1,
                vedtaksperiodeId = vedtaksperiodeId1,
                forkastet = false,
                generasjoner = listOf(
                    GenerasjonDto(
                        id = UUID.randomUUID(),
                        vedtaksperiodeId = vedtaksperiodeId1,
                        utbetalingId = null,
                        spleisBehandlingId = UUID.randomUUID(),
                        skjæringstidspunkt = 1.januar,
                        fom = 1.januar,
                        tom = 31.januar,
                        tilstand = TilstandDto.VidereBehandlingAvklares,
                        emptyList(),
                        emptyList(),
                        null
                    ),
                )
            ),
            VedtaksperiodeDto(
                organisasjonsnummer = organisasjonsnummer1,
                vedtaksperiodeId = vedtaksperiodeId2,
                forkastet = false,
                generasjoner = listOf(
                    GenerasjonDto(
                        id = UUID.randomUUID(),
                        vedtaksperiodeId = vedtaksperiodeId2,
                        utbetalingId = null,
                        spleisBehandlingId = UUID.randomUUID(),
                        skjæringstidspunkt = 1.februar,
                        fom = 1.februar,
                        tom = 28.februar,
                        tilstand = TilstandDto.VidereBehandlingAvklares,
                        emptyList(),
                        emptyList(),
                        null
                    ),
                )
            )
        )

        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                with(generasjonRepository) {
                    tx.lagreVedtaksperioder(person1, vedtaksperioder)
                }
            }
        }

        val funnet = sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                with(generasjonRepository) {
                    tx.finnVedtaksperioder(person1)
                }
            }
        }

        assertEquals(vedtaksperioder, funnet)
    }
}
