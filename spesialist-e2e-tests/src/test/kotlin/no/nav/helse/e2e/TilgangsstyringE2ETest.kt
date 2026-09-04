package no.nav.helse.e2e

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.auth.AccessToken
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertNotNull

class TilgangsstyringE2ETest : AbstractE2ETest() {
    @Test
    fun `Kan hente en minimal person når personinfo er kjent, og trigger ikke klargjøring`() {
        settOppDefaultDataOgTilganger()

        assertKanIkkeHentePerson("Fant ikke data for person")

        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        berikPersonMedPersoninfo()

        every { snapshothenter.hentPerson(FØDSELSNUMMER) } returns null

        assertKanHentePerson()
        assertIkkeUtgåendeMelding("klargjør_person_for_visning")
    }

    @Test
    fun `Kan hente person både før og etter at egen ansatt-status er avklart, og trigger ikke klargjøring`() {
        // Egen ansatt-status er kun brukt til tilgangsstyring av Tilgangsmaskinen (dekket av testene under),
        // ikke til å avgjøre om personen er klar for visning. Denne testen viser at henting av person ikke
        // lenger avhenger av at egenAnsattStatus er satt, verken før eller etter den blir kjent.
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()

        assertKanHentePerson()

        håndterEgenansattløsning(erEgenAnsatt = false)
        assertKanHentePerson()
        assertIkkeUtgåendeMelding("klargjør_person_for_visning")
    }

    @Test
    fun `Kan ikke hente person som er egen ansatt dersom saksbehandler mangler tilgang til egen ansatte`() {
        settOppDefaultDataOgTilganger()
        populasjonstilgangskontrollProvider.resultat = TilgangskontrollResultat.ManglerTilgang(TilgangSomMangler.EgenAnsatt)
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()
        håndterEgenansattløsning()
        assertKanIkkeHentePerson("Har ikke tilgang til person")
    }

    @Test
    fun `Kan ikke hente person som er kode7 dersom saksbehandler mangler tilgang til kode7`() {
        settOppDefaultDataOgTilganger()
        populasjonstilgangskontrollProvider.resultat = TilgangskontrollResultat.ManglerTilgang(TilgangSomMangler.FortroligAdresse)
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()
        håndterEgenansattløsning()
        assertKanIkkeHentePerson("Har ikke tilgang til person")
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class, names = ["StrengtFortroligAdresse", "StrengtFortroligAdresseUtland"], mode = EnumSource.Mode.INCLUDE)
    fun `Kan ikke hente kode6-personer`(tilgangSomMangler: TilgangSomMangler) {
        settOppDefaultDataOgTilganger()
        populasjonstilgangskontrollProvider.resultat = TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()
        håndterEgenansattløsning()
        assertKanIkkeHentePerson("Har ikke tilgang til person")
    }

    private fun berikPersonMedPersoninfo(fødselsnummer: String = FØDSELSNUMMER) {
        sessionFactory.transactionalSessionScope { transaction ->
            val person =
                transaction.personRepository.finn(Identitetsnummer.fraString(fødselsnummer))
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

    private fun fetchPerson() =
        personQuery.person(
            personPseudoId =
                personPseudoIdProvider
                    .nyPersonPseudoId(Identitetsnummer.fraString(FØDSELSNUMMER))
                    .value
                    .toString(),
            env = dataFetchingEnvironment,
        )

    private fun assertKanIkkeHentePerson(feilmelding: String) {
        runCatching { fetchPerson() }.let { result ->
            val exceptionOrNull = result.exceptionOrNull()
            assertNotNull(exceptionOrNull)
            assertEquals(feilmelding, exceptionOrNull.message)
        }
    }

    private fun assertKanHentePerson() {
        fetchPerson().let { response ->
            assertEquals(emptyList<GraphQLError>(), response.errors)
            assertNotNull(response.data)
        }
    }

    private fun settOppDefaultDataOgTilganger() {
        every { dataFetchingEnvironment.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER) } returns
            Saksbehandler(
                id = SaksbehandlerOid(value = UUID.randomUUID()),
                navn = "epost",
                epost = "navn",
                ident = NAVIdent("A123456"),
            )
        every { dataFetchingEnvironment.graphQlContext.get<Set<Brukerrolle>>(ContextValues.BRUKERROLLER) } returns emptySet()
        every { dataFetchingEnvironment.graphQlContext.get<AccessToken>(ContextValues.ACCESS_TOKEN) } returns AccessToken("token")
    }

    private val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)

    private val personQuery =
        PersonQuery(
            handler =
                PersonQueryHandler(
                    daos = daos,
                    apiOppgaveService = mockk(relaxed = true),
                    snapshothenter = snapshothenter,
                    sessionFactory = sessionFactory,
                    personPseudoIdProvider = personPseudoIdProvider,
                    populasjonstilgangskontrollProvider = populasjonstilgangskontrollProvider,
                ),
        )
}
