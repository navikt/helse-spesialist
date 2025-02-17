package no.nav.helse.spesialist.db

import no.nav.helse.db.PgGenerasjonDao
import no.nav.helse.db.PgVedtakDao
import no.nav.helse.db.PgVedtaksperiodeRepository
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PgVedtaksperiodeDbDtoRepositoryTest: DatabaseIntegrationTest() {

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
                        skjæringstidspunkt = 1 jan 2018,
                        fom = 1 jan 2018,
                        tom = 31 jan 2018,
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
                        skjæringstidspunkt = 1 feb 2018,
                        fom = 1 feb 2018,
                        tom = 28 feb 2018,
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
