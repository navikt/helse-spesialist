package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.erDev
import no.nav.helse.spesialist.api.graphql.schema.Person
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PersonQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val snapshotMediator: SnapshotMediator,
    private val reservasjonClient: ReservasjonClient,
    private val oppgavehåndterer: Oppgavehåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    suspend fun person(fnr: String? = null, aktorId: String? = null, env: DataFetchingEnvironment): DataFetcherResult<Person?> {
        if (fnr == null) {
            if (aktorId == null)
                return DataFetcherResult.newResult<Person?>().error(getBadRequestError("Requesten mangler både fødselsnummer og aktørId")).build()
            if (aktorId.length != 13) {
                return DataFetcherResult.newResult<Person?>().error(getBadRequestError("Feil lengde på parameter aktorId: ${aktorId.length}")).build()
            }
        }

        val fødselsnummer =
            if (fnr != null && personApiDao.finnesPersonMedFødselsnummer(fnr)) fnr
            else aktorId?.let {
                try {
                    personApiDao.finnFødselsnummer(it.toLong())
                } catch (e: Exception) {
                    val fødselsnumre = personApiDao.finnFødselsnumre(aktorId.toLong()).toSet()
                    return DataFetcherResult.newResult<Person?>().error(getFlereFødselsnumreError(fødselsnumre)).build()
                }
            }
        if (fødselsnummer == null || !personApiDao.spesialistHarPersonKlarForVisningISpeil(fødselsnummer)) {
            sikkerLogg.info("Svarer not found for parametere fnr=$fnr, aktorId=$aktorId.")
            return DataFetcherResult.newResult<Person?>().error(getNotFoundError(fnr)).build()
        }

        val reservasjon = finnReservasjonsstatus(fødselsnummer)

        if (isForbidden(fødselsnummer, env)) {
            return DataFetcherResult.newResult<Person?>().error(getForbiddenError(fødselsnummer)).build()
        }

        val snapshot = try {
            snapshotMediator.hentSnapshot(fødselsnummer)
        } catch (e: Exception) {
            sikkerLogg.error("feilet under henting av snapshot for {}", keyValue("fnr", fødselsnummer), e)
            return DataFetcherResult.newResult<Person?>().error(getSnapshotValidationError()).build()
        }

        val person = snapshot?.let { (personinfo, personSnapshot) ->
            Person(
                snapshot = personSnapshot,
                personinfo = personinfo.copy(reservasjon = reservasjon.await()),
                personApiDao = personApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselRepository = varselRepository,
                oppgaveApiDao = oppgaveApiDao,
                periodehistorikkDao = periodehistorikkDao,
                notatDao = notatDao,
                totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                påVentApiDao = påVentApiDao,
                tilganger = env.graphQlContext.get("tilganger"),
                oppgavehåndterer = oppgavehåndterer,
            )
        }

        return if (person == null) {
            DataFetcherResult.newResult<Person?>().error(getNotFoundError(fødselsnummer)).build()
        } else {
            DataFetcherResult.newResult<Person?>().data(person).build()
        }
    }

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        if (erDev()) CompletableDeferred<Reservasjon?>().also { it.complete(null) }
        else CoroutineScope(Dispatchers.IO).async {
            reservasjonClient.hentReservasjonsstatus(fødselsnummer)
        }

    private fun getFlereFødselsnumreError(fødselsnumre: Set<String>): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Mer enn ett fødselsnummer for personen")
        .extensions(
            mapOf(
                "code" to 500,
                "feilkode" to "HarFlereFodselsnumre",
                "fodselsnumre" to fødselsnumre,
            )
        ).build()

    private fun getSnapshotValidationError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Lagret snapshot stemmer ikke overens med forventet format. Dette kommer som regel av at noen har gjort endringer på formatet men glemt å bumpe versjonsnummeret.")
        .extensions(mapOf("code" to 501, "field" to "person"))
        .build()

    private fun getBadRequestError(melding: String): GraphQLError = GraphqlErrorException.newErrorException()
        .message(melding)
        .extensions(mapOf("code" to 400))
        .build()

}
