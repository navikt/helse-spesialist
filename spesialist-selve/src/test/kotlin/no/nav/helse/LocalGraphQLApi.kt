package no.nav.helse

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.installGraphQLApi
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.tildeling.TildelingDao
import java.time.LocalDate

fun main() = runBlocking {
    Toggle.GraphQLApi.enable()
    Toggle.GraphQLPlayground.enable()
    TestApplication(4321).start() { dataSource ->
        val snapshotDao = mockk<SnapshotDao>(relaxed = true)
        val personApiDao = PersonApiDao(dataSource)
        val tildelingDao = TildelingDao(dataSource)
        val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
        val overstyringApiDao = OverstyringApiDao(dataSource)

        every { snapshotDao.hentSnapshotMedMetadata(any()) } returns (enPersoninfo to enPerson)

        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }

        installGraphQLApi(
            snapshotDao = snapshotDao,
            personApiDao = personApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao
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
