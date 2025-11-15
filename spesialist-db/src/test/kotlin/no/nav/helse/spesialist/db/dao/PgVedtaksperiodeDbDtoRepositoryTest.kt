package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PgVedtaksperiodeDbDtoRepositoryTest: AbstractDBIntegrationTest() {

    private val pgLegacyVedtaksperiodeRepository = PgLegacyVedtaksperiodeRepository(
        legacyBehandlingDao = PgLegacyBehandlingDao(session),
        vedtakDao = PgVedtakDao(session),
    )

    @Test
    fun `lagrer og finner liste av vedtaksperioder`() {
        val person1 = lagFødselsnummer()
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        opprettPerson(fødselsnummer = person1, lagAktørId())
        opprettArbeidsgiver(identifikator = organisasjonsnummer1)

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
                        Yrkesaktivitetstype.ARBEIDSTAKER
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
                        Yrkesaktivitetstype.ARBEIDSTAKER
                    ),
                )
            )
        )
        pgLegacyVedtaksperiodeRepository.lagreVedtaksperioder(person1, vedtaksperioder)

        val funnet = pgLegacyVedtaksperiodeRepository.finnVedtaksperioder(person1)
        assertEquals(vedtaksperioder.toSet(), funnet.toSet())
    }
}
