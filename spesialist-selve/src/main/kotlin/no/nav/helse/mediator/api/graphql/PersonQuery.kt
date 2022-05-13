package no.nav.helse.mediator.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.api.graphql.schema.Person
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PersonQuery(
    personApiDao: PersonApiDao,
    egenAnsattDao: EgenAnsattDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val oppgaveDao: OppgaveDao,
    private val snapshotMediator: SnapshotMediator,
) : AbstractPersonQuery(personApiDao, egenAnsattDao) {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    fun person(fnr: String? = null, aktorId: String? = null, env: DataFetchingEnvironment): DataFetcherResult<Person?> {
        if (fnr == null && aktorId == null) {
            return DataFetcherResult.newResult<Person?>().error(getBadRequestError()).build()
        }

        val fødselsnummer = fnr.takeIf { it != null && personApiDao.finnesPersonMedFødselsnummer(it) }
            ?: aktorId?.let { personApiDao.finnFødselsnummer(it.toLong()) }
            ?: return DataFetcherResult.newResult<Person?>().error(getNotFoundError(fnr)).build()

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
                personinfo = personinfo,
                personApiDao = personApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselDao = varselDao,
                oppgaveDao = oppgaveDao,
            )
        }

        return if (person == null) {
            DataFetcherResult.newResult<Person?>().error(getNotFoundError(fødselsnummer)).build()
        } else {
            DataFetcherResult.newResult<Person?>().data(person).build()
        }
    }

    private fun getSnapshotValidationError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Lagret snapshot stemmer ikke overens med forventet format. Dette kommer som regel av at noen har gjort endringer på formatet men glemt å bumpe versjonsnummeret.")
        .extensions(mapOf("code" to 501, "field" to "person"))
        .build()

    private fun getBadRequestError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Requesten mangler både fødselsnummer og aktørId")
        .extensions(mapOf("code" to 400))
        .build()

}
