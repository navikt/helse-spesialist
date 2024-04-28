package no.nav.helse.modell.person

import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.SpleisBehandling
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PersonTest {
    private val testperson = TestPerson()
    private val aktørId = testperson.aktørId
    private val fødselsnummer = testperson.fødselsnummer
    private val arbeidsgiver1 = testperson.nyArbeidsgiver()

    @Test
    fun `oppretter ny vedtaksperiode ved ny Spleis-behandling dersom perioden ikke er kjent fra før`() {
        val person = Person.gjenopprett(aktørId, fødselsnummer, emptyList(), emptyList())
        person.nySpleisBehandling(nySpleisBehandling(UUID.randomUUID()))
        val dto = person.toDto()
        assertEquals(1, dto.vedtaksperioder.size)
    }

    private fun nySpleisBehandling(vedtaksperiodeId: UUID) =
        SpleisBehandling(
            organisasjonsnummer = arbeidsgiver1.organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = UUID.randomUUID(),
            fom = 1.januar,
            tom = 31.januar,
        )
}
