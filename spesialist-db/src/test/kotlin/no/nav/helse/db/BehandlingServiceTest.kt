package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.GenerasjonService
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingServiceTest: DatabaseIntegrationTest() {

    private val generasjonService = GenerasjonService(dataSource, repositories)

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
                behandlinger = listOf(
                    BehandlingDto(
                        id = UUID.randomUUID(),
                        vedtaksperiodeId = vedtaksperiodeId1,
                        utbetalingId = null,
                        spleisBehandlingId = UUID.randomUUID(),
                        skjæringstidspunkt = 1.januar,
                        fom = 1.januar,
                        tom = 31.januar,
                        tilstand = TilstandDto.VidereBehandlingAvklares,
                        emptyList(),
                        null,
                        emptyList(),
                    ),
                )
            ),
            VedtaksperiodeDto(
                organisasjonsnummer = organisasjonsnummer1,
                vedtaksperiodeId = vedtaksperiodeId2,
                forkastet = false,
                behandlinger = listOf(
                    BehandlingDto(
                        id = UUID.randomUUID(),
                        vedtaksperiodeId = vedtaksperiodeId2,
                        utbetalingId = null,
                        spleisBehandlingId = UUID.randomUUID(),
                        skjæringstidspunkt = 1.februar,
                        fom = 1.februar,
                        tom = 28.februar,
                        tilstand = TilstandDto.VidereBehandlingAvklares,
                        emptyList(),
                        null,
                        emptyList(),
                    ),
                )
            )
        )

        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                with(generasjonService) {
                    tx.lagreVedtaksperioder(person1, vedtaksperioder)
                }
            }
        }

        val funnet = sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                with(generasjonService) {
                    tx.finnVedtaksperioder(person1)
                }
            }
        }

        assertEquals(vedtaksperioder.toSet(), funnet.toSet())
    }
}
