package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.LocalDate

class PgArbeidsgiverRepositoryTest : AbstractDBIntegrationTest() {
    private val arbeidsgiverRepository = sessionContext.arbeidsgiverRepository

    @Test
    fun `kan lagre og hente opp arbeidsgiver uten navn`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id())

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
        assertNull(actualArbeidsgiver.navn)
    }

    @Test
    fun `kan lagre og hente opp arbeidsgiver med navn`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)
            .apply { oppdaterMedNavn(organisasjonsnavn) }

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id())

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn?.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn?.sistOppdatertDato)
    }

    @Test
    fun `kan lagre arbeidsgiver uten navn, oppdatere med navn, og hente opp`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val lagretArbeidsgiver = arbeidsgiverRepository.finnForIdentifikator(identifikator)
        assertNotNull(lagretArbeidsgiver)
        val organisasjonsnavn = lagOrganisasjonsnavn()

        // When:
        lagretArbeidsgiver!!.oppdaterMedNavn(organisasjonsnavn)
        arbeidsgiverRepository.lagre(lagretArbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id())

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn?.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn?.sistOppdatertDato)
    }

    @Test
    fun `kan lagre og hente opp arbeidsgiver basert på identifikator`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)
            .apply { oppdaterMedNavn(organisasjonsnavn) }

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgiver = arbeidsgiverRepository.finnForIdentifikator(identifikator)

        // Then:
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn?.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn?.sistOppdatertDato)
    }

    @Test
    fun `kan lagre og hente opp arbeidsgiver basert på identifikator i liste`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Organisasjonsnummer(
            organisasjonsnummer = lagOrganisasjonsnummer()
        )
        val organisasjonsnavn = lagOrganisasjonsnavn()
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)
            .apply { oppdaterMedNavn(organisasjonsnavn) }

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)
        val actualArbeidsgivere = arbeidsgiverRepository.finnAlleForIdentifikatorer(setOf(identifikator))

        // Then:
        assertEquals(1, actualArbeidsgivere.size)
        val actualArbeidsgiver = actualArbeidsgivere.first()
        assertEquals(identifikator, actualArbeidsgiver.identifikator)
        assertEquals(organisasjonsnavn, actualArbeidsgiver.navn?.navn)
        assertEquals(LocalDate.now(), actualArbeidsgiver.navn?.sistOppdatertDato)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med tallet 0 får vi riktig fødselsnummer ut igjen`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Fødselsnummer(
            fødselsnummer = lagFødselsnummer().replaceFirstChar { "0" }
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)

        // Then:
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id())
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med annet siffer enn 0 får vi riktig fødselsnummer ut igjen`() {
        // Given:
        val identifikator = Arbeidsgiver.Identifikator.Fødselsnummer(
            fødselsnummer = lagFødselsnummer().replaceFirstChar { "1" }
        )
        val arbeidsgiver = Arbeidsgiver.Factory.ny(identifikator = identifikator)

        // When:
        arbeidsgiverRepository.lagre(arbeidsgiver)

        // Then:
        val actualArbeidsgiver = arbeidsgiverRepository.finn(arbeidsgiver.id())
        assertNotNull(actualArbeidsgiver)
        assertEquals(identifikator, actualArbeidsgiver!!.identifikator)
    }
}
