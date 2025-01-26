package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PgVedtaksperiodeRepositoryTest: DatabaseIntegrationTest() {

    private val pgVedtaksperiodeRepository = PgVedtaksperiodeRepository(
        generasjonDao = PgGenerasjonDao(session),
        vedtakDao = PgVedtakDao(session),
    )

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
        pgVedtaksperiodeRepository.lagreVedtaksperioder(person1, vedtaksperioder)

        val funnet = pgVedtaksperiodeRepository.finnVedtaksperioder(person1)
        assertEquals(vedtaksperioder.toSet(), funnet.toSet())
    }
}
