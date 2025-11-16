package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PgArbeidsgiverRepositoryTest : AbstractDBIntegrationTest() {
    private val arbeidsgiverRepository = sessionContext.arbeidsgiverRepository

    @Test
    fun `kan lagre og hente opp arbeidsgiver`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = organisasjonsnavn)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id)

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.id)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn.sistOppdatertDato)
    }

    @Test
    fun `kan lagre arbeidsgiver, oppdatere med nytt navn, og hente opp`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = lagOrganisasjonsnavn())
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val lagretArbeidsgiver = arbeidsgiverRepository.finn(identifikator)
        assertNotNull(lagretArbeidsgiver)
        val nyttOrganisasjonsnavn = lagOrganisasjonsnavn()

        // When:
        lagretArbeidsgiver!!.oppdaterMedNavn(nyttOrganisasjonsnavn)
        arbeidsgiverRepository.lagre(lagretArbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id)

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.id)
        assertEquals(nyttOrganisasjonsnavn, actualArbeidsgiver.navn.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn.sistOppdatertDato)
    }

    @Test
    fun `kan lagre og hente opp arbeidsgiver basert på identifikator`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = organisasjonsnavn)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(identifikator)

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.id)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn.sistOppdatertDato)
    }

    @Test
    fun `kan lagre og hente opp arbeidsgiver basert på identifikator i liste`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = organisasjonsnavn)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgivere = arbeidsgiverRepository.finnAlle(setOf(identifikator))

        // Then:
        assertEquals(1, actualArbeidsgivere.size)
        val actualArbeidsgiver = actualArbeidsgivere.first()
        assertEquals(identifikator, actualArbeidsgiver.id)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn.sistOppdatertDato)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med tallet 0 får vi riktig fødselsnummer ut igjen`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Fødselsnummer(
            fødselsnummer = lagFødselsnummer().replaceFirstChar { "0" }
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = lagOrganisasjonsnavn())

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)

        // Then:
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id)
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.id)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med annet siffer enn 0 får vi riktig fødselsnummer ut igjen`() {
        // Given:
        val identifikator = ArbeidsgiverIdentifikator.Fødselsnummer(
            fødselsnummer = lagFødselsnummer().replaceFirstChar { "1" }
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(id = identifikator, navnString = lagOrganisasjonsnavn())

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)

        // Then:
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id)
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.id)
    }
}
