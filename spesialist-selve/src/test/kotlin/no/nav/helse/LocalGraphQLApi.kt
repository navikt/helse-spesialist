package no.nav.helse

import TestToggles.enable
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphQLApi
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.EnhetDto
import no.nav.helse.vedtaksperiode.VarselDao
import java.time.LocalDate
import java.util.*

fun main() = runBlocking {
    Toggle.GraphQLApi.enable()
    Toggle.GraphQLPlayground.enable()
    TestApplication(4321).start { dataSource ->
        val snapshotDao = mockk<SnapshotDao>(relaxed = true)
        val personApiDao = mockk<PersonApiDao>(relaxed = true)
        val tildelingDao = TildelingDao(dataSource)
        val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
        val overstyringApiDao = OverstyringApiDao(dataSource)
        val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
        val varselDao = VarselDao(dataSource)

        every { snapshotDao.hentSnapshotMedMetadata(any()) } returns (enPersoninfo to enPerson)
        every { personApiDao.personHarAdressebeskyttelse(any(), any()) } returns false
        every { personApiDao.personHarAdressebeskyttelse(any(), no.nav.helse.person.Adressebeskyttelse.Ugradert) } returns true
        every { personApiDao.finnEnhet(any()) } returns EnhetDto("1234", "Bømlo")

        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }

        graphQLApi(
            snapshotDao = snapshotDao,
            personApiDao = personApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            snapshotGraphQLClient = mockk(relaxed = true)
        )
    }
}

private val enPersoninfo = PersoninfoDto(
    fornavn = "Luke",
    mellomnavn = null,
    etternavn = "Skywalker",
    fødselsdato = LocalDate.EPOCH,
    kjønn = Kjønn.Mann,
    adressebeskyttelse = Adressebeskyttelse.Ugradert
)

private val enPerson = GraphQLPerson(
    aktorId = "jedi-master",
    arbeidsgivere = emptyList(),
    dodsdato = null,
    fodselsnummer = "01017012345",
    inntektsgrunnlag = emptyList(),
    versjon = 1,
    vilkarsgrunnlaghistorikk = emptyList()
)
