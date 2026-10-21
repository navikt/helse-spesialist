package no.nav.helse.e2e

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.rest.personer.saksbilde.SaksbildeHenter
import no.nav.helse.spesialist.api.rest.saksbilde.ApiSaksbildePerson
import no.nav.helse.spesialist.application.InMemoryPersoninfoHenter
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertNull

class HentSaksbildeE2ETest : AbstractE2ETest() {
    @Test
    fun `Kan hente en minimal person når personinfo er kjent, og trigger ikke klargjøring`() {
        assertKanIkkeHentePerson()

        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        berikPersonMedPersoninfo()

        every { snapshothenter.hentPerson(FØDSELSNUMMER) } returns
            SnapshotPerson(
                arbeidsgivere = emptyList(),
                dodsdato = null,
                fodselsnummer = FØDSELSNUMMER,
                vilkarsgrunnlag = emptyList(),
            )

        assertKanHentePerson()
        assertIkkeUtgåendeMelding("klargjør_person_for_visning")
    }

    @Test
    fun `Kan hente person både før og etter at egen ansatt-status er avklart, og trigger ikke klargjøring`() {
        // Egen ansatt-status er kun brukt til tilgangsstyring av Tilgangsmaskinen (dekket av GetSaksbildeForPersonIntegrationTest),
        // ikke til å avgjøre om personen er klar for visning. Denne testen viser at henting av person ikke
        // lenger avhenger av at egenAnsattStatus er satt, verken før eller etter den blir kjent.
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()

        assertKanHentePerson()

        håndterEgenansattløsning(erEgenAnsatt = false)
        assertKanHentePerson()
        assertIkkeUtgåendeMelding("klargjør_person_for_visning")
    }

    private fun berikPersonMedPersoninfo(fødselsnummer: String = FØDSELSNUMMER) {
        sessionFactory.transactionalSessionScope { transaction ->
            val person =
                transaction.personRepository.finnOrNull(Identitetsnummer.fraString(fødselsnummer))
                    ?: error("Fant ikke person med fødselsnummer=$fødselsnummer")
            person.oppdaterInfo(
                Personinfo(
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    fødselsdato = LocalDate.of(1990, 1, 1),
                    dødsdato = null,
                    kjønn = Personinfo.Kjønn.Ukjent,
                    adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert,
                ),
            )
            transaction.personRepository.lagre(person)
        }
    }

    private fun sendMeldingerOppTilEgenAnsatt(adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert) {
        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingEndret()
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IM_1"))
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning(adressebeskyttelse = adressebeskyttelse)
        håndterEnhetløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
    }

    private fun hentSaksbilde(): ApiSaksbildePerson? =
        sessionFactory.transactionalSessionScope { transaction ->
            transaction.personRepository
                .finnOrNull(Identitetsnummer.fraString(FØDSELSNUMMER))
                ?.let { saksbildeHenter.hent(it, transaction) }
        }

    private fun assertKanIkkeHentePerson() {
        assertNull(hentSaksbilde())
    }

    private fun assertKanHentePerson() {
        assertEquals(FØDSELSNUMMER, hentSaksbilde()?.fodselsnummer)
    }

    private val saksbildeHenter =
        SaksbildeHenter(
            daos = daos,
            apiOppgaveService = mockk(relaxed = true),
            snapshothenter = snapshothenter,
            personPseudoIdProvider = personPseudoIdProvider,
            personinfoHenter = InMemoryPersoninfoHenter(),
        )
}
