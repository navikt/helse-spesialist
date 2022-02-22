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
import no.nav.helse.mediator.graphql.enums.GraphQLBehandlingstype
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.hentsnapshot.Alder
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.EnhetDto
import no.nav.helse.vedtaksperiode.VarselDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
        val oppgaveDao = mockk<OppgaveDao>(relaxed = true)

        every { snapshotDao.hentSnapshotMedMetadata(any()) } returns (enPersoninfo to enPerson)
        every { personApiDao.personHarAdressebeskyttelse(any(), any()) } returns false
        every {
            personApiDao.personHarAdressebeskyttelse(
                any(),
                no.nav.helse.person.Adressebeskyttelse.Ugradert
            )
        } returns true
        every { personApiDao.finnEnhet(any()) } returns EnhetDto("1234", "Bømlo")
        every { personApiDao.finnFødselsnummer(isNull(inverse = true)) } returns enPerson.fodselsnummer
        every { utbetalingDao.findUtbetalinger(any()) } returns emptyList()
        every { oppgaveDao.finnOppgaveId(any<UUID>()) } returns 123456789L

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
            utbetalingDao = utbetalingDao,
            oppgaveDao = oppgaveDao,
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

private fun enPeriode() = GraphQLBeregnetPeriode(
    behandlingstype = GraphQLBehandlingstype.BEHANDLET,
    erForkastet = false,
    fom = "2020-01-01",
    tom = "2020-01-31",
    inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
    opprettet = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
    periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
    tidslinje = emptyList(),
    vedtaksperiodeId = UUID.randomUUID().toString(),
    id = UUID.randomUUID().toString(),
    aktivitetslogg = emptyList(),
    beregningId = UUID.randomUUID().toString(),
    forbrukteSykedager = 10,
    gjenstaendeSykedager = 270,
    hendelser = emptyList(),
    maksdato = "2021-01-01",
    periodevilkar = GraphQLPeriodevilkar(
        Alder(
            alderSisteSykedag = 40,
            oppfylt = true
        ),
        soknadsfrist = Soknadsfrist(
            oppfylt = true,
            sendtNav = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            soknadFom = "2020-01-01",
            soknadTom = "2020-01-31"
        ),
        sykepengedager = Sykepengedager(
            forbrukteSykedager = 10,
            gjenstaendeSykedager = 270,
            maksdato = "2021-01-01",
            oppfylt = true,
            skjaeringstidspunkt = "2020-01-01"
        )
    ),
    skjaeringstidspunkt = "2020-01-01",
    utbetaling = GraphQLUtbetaling(
        arbeidsgiverFagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
        arbeidsgiverNettoBelop = 30000,
        personFagsystemId = "EN-PERSONFAGSYSTEMID",
        personNettoBelop = 0,
        status = "UTBETALT",
        type = "UTBETALING",
        vurdering = GraphQLVurdering(
            automatisk = false,
            godkjent = true,
            ident = "AB123456",
            tidsstempel = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        )
    ),
    refusjon = null,
    vilkarsgrunnlaghistorikkId = UUID.randomUUID().toString()
)

private fun enGenerasjon() = GraphQLGenerasjon(
    id = UUID.randomUUID().toString(),
    perioder = listOf(enPeriode())
)

private fun enArbeidsgiver(organisasjonsnummer: String = "987654321") = GraphQLArbeidsgiver(
    organisasjonsnummer = organisasjonsnummer,
    ghostPerioder = emptyList(),
    generasjoner = listOf(enGenerasjon())
)

private val enPerson = GraphQLPerson(
    aktorId = "jedi-master",
    arbeidsgivere = listOf(enArbeidsgiver()),
    dodsdato = null,
    fodselsnummer = "01017012345",
    inntektsgrunnlag = emptyList(),
    versjon = 1,
    vilkarsgrunnlaghistorikk = emptyList()
)
