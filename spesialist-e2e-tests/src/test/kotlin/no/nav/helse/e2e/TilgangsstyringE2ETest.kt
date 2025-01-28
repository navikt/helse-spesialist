package no.nav.helse.e2e

import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

internal class TilgangsstyringE2ETest : AbstractE2ETest() {
    @Test
    fun `Gir 409 når bare søknad er mottatt`() {
        settOppDefaultDataOgTilganger()

        assertKanIkkeHentePerson("Finner ikke data for person med identifikator ")

        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")
    }

    @Test
    fun `Kan hente person som er egen ansatt så lenge vi kjenner til den og saksbehandler har tilgang til egen ansatte`() {
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")

        håndterEgenansattløsning(erEgenAnsatt = false)
        assertKanHentePerson()
    }

    @Test
    fun `Kan ikke hente person som er egen ansatt dersom saksbehandler mangler tilgang til egen ansatte`() {
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt()
        mockSnapshot()

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")

        håndterEgenansattløsning(erEgenAnsatt = true)
        assertKanIkkeHentePerson("Har ikke tilgang til person med fødselsnummer ")
    }

    @Test
    fun `Kan ikke hente person som er kode6 dersom saksbehandler mangler tilgang til kode6`() {
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        mockSnapshot()

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")

        håndterEgenansattløsning()
        assertKanIkkeHentePerson("Har ikke tilgang til person med fødselsnummer ")
    }

    @Test
    fun `Kan hente person som er kode6 dersom saksbehandler har tilgang til kode6`() {
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        mockSnapshot()

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")
        håndterEgenansattløsning()
        saksbehandlertilgangTilKode7(true)
        assertKanHentePerson()
    }

    @ParameterizedTest
    @EnumSource(Adressebeskyttelse::class, names = ["Ugradert", "Fortrolig"], mode = EnumSource.Mode.EXCLUDE)
    fun `Kan ikke hente kode7-personer`(adressebeskyttelse: Adressebeskyttelse) {
        settOppDefaultDataOgTilganger()
        sendMeldingerOppTilEgenAnsatt(adressebeskyttelse = adressebeskyttelse)
        mockSnapshot()

        assertKanIkkeHentePerson("Person med fødselsnummer $FØDSELSNUMMER er ikke klar for visning ennå")
        håndterEgenansattløsning()
        saksbehandlertilgangTilKode7(true)
        saksbehandlertilgangTilSkjermede(true)
        assertKanIkkeHentePerson("Har ikke tilgang til person med fødselsnummer ")
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
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
    }

    private fun fetchPerson() = runBlocking { personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment) }

    private fun assertKanIkkeHentePerson(feilmelding: String) {
        fetchPerson().let { response ->
            assertFeilmelding(feilmelding, response.errors)
            assertNull(response.data)
        }
    }

    private fun assertKanHentePerson() {
        fetchPerson().let { response ->
            assertEquals(emptyList<GraphQLError>(), response.errors)
            assertNotNull(response.data)
        }
    }

    private fun settOppDefaultDataOgTilganger() {
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER) } returns
            SaksbehandlerFraApi(
                UUID.randomUUID(),
                "epost",
                "navn",
                "A123456",
                emptyList(),
            )
        saksbehandlertilgangTilSkjermede(harTilgang = false)
    }

    private fun saksbehandlertilgangTilSkjermede(harTilgang: Boolean) {
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER) } returns
            mockk(relaxed = true) {
                every { harTilgangTilSkjermedePersoner() } returns harTilgang
            }
    }

    private fun saksbehandlertilgangTilKode7(harTilgang: Boolean) {
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER) } returns
            mockk(relaxed = true) {
                every { harTilgangTilKode7() } returns harTilgang
            }
    }

    private val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)

    private val personQuery =
        PersonQuery(
            personoppslagService = PersonService(
                personApiDao = repositories.personApiDao,
                egenAnsattApiDao = repositories.egenAnsattApiDao,
                tildelingApiDao = repositories.tildelingApiDao,
                arbeidsgiverApiDao = repositories.arbeidsgiverApiDao,
                overstyringApiDao = repositories.overstyringApiDao,
                risikovurderingApiDao = repositories.risikovurderingApiDao,
                varselRepository = repositories.varselApiRepository,
                oppgaveApiDao = repositories.oppgaveApiDao,
                periodehistorikkApiDao = repositories.periodehistorikkApiDao,
                notatDao = repositories.notatApiDao,
                totrinnsvurderingApiDao = repositories.totrinnsvurderingApiDao,
                påVentApiDao = repositories.påVentApiDao,
                vergemålApiDao = repositories.vergemålApiDao,
                snapshotService = SnapshotService(repositories.snapshotApiDao, snapshotClient),
                reservasjonClient = mockk(relaxed = true),
                oppgavehåndterer = mockk(relaxed = true),
                saksbehandlerhåndterer = mockk(relaxed = true),
                avviksvurderinghenter = mockk(relaxed = true),
                personhåndterer = object : Personhåndterer {
                    override fun oppdaterSnapshot(fødselsnummer: String) {}
                    override fun klargjørPersonForVisning(fødselsnummer: String) {}
                },
                stansAutomatiskBehandlinghåndterer = mockk(relaxed = true),
            ),
        )

    companion object {
        private fun assertFeilmelding(
            feilmelding: String,
            errors: List<GraphQLError>,
        ) {
            assertTrue(errors.any { it.message.contains(feilmelding) }) {
                "Forventet \"$feilmelding\", men fikk ${errors.map { it.message }}"
            }
        }
    }
}
