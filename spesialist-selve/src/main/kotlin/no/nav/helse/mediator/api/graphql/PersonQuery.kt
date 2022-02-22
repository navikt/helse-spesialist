package no.nav.helse.mediator.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.api.graphql.schema.Person
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao

class PersonQuery(
    personApiDao: PersonApiDao,
    private val snapshotDao: SnapshotDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val oppgaveDao: OppgaveDao,
    private val snapshotGraphQLClient: SpeilSnapshotGraphQLClient
) : AbstractPersonQuery(personApiDao) {

    fun person(fnr: String? = null, aktorId: String? = null, env: DataFetchingEnvironment): DataFetcherResult<Person?> {
        if (fnr == null && aktorId == null) {
            return DataFetcherResult.newResult<Person?>().error(getBadRequestError()).build()
        }

        val fødselsnummer = fnr
            ?: aktorId?.let { personApiDao.finnFødselsnummer(it.toLong()) }
            ?: return DataFetcherResult.newResult<Person?>().error(getNotFoundError()).build()

        if (isForbidden(fødselsnummer, env)) {
            return DataFetcherResult.newResult<Person?>().error(getForbiddenError(fødselsnummer)).build()
        }

        if (snapshotDao.utdatert(fødselsnummer)) {
            snapshotGraphQLClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
        }

        val snapshot = try {
            snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
        } catch (e: Exception) {
            return DataFetcherResult.newResult<Person?>().error(getSnapshotValidationError()).build()
        }

        val person = snapshot?.let { (personinfo, snapshot) ->
            Person(
                snapshot = snapshot,
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
