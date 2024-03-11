package no.nav.helse.modell.person

import TestPerson
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.SpleisBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PersonTest {

    private val testperson = TestPerson()
    private val fødselsnummer = testperson.fødselsnummer
    private val arbeidsgiver1 = testperson.nyArbeidsgiver()

    @Test
    fun `oppretter ny vedtaksperiode ved ny Spleis-behandling dersom perioden ikke er kjent fra før`() {
        val person = Person.gjenopprett(fødselsnummer, emptyList())
        person.nySpleisBehandling(nySpleisBehandling(UUID.randomUUID()))
        val dto = person.toDto()
        assertEquals(1, dto.vedtaksperioder.size)
    }

    private fun nySpleisBehandling(vedtaksperiodeId: UUID) = SpleisBehandling(
        organisasjonsnummer = arbeidsgiver1.organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = UUID.randomUUID(),
        fom = 1.januar,
        tom = 31.januar
    )
}