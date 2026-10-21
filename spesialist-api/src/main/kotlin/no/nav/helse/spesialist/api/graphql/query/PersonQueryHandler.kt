package no.nav.helse.spesialist.api.graphql.query

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.AuditLogger
import no.nav.helse.spesialist.api.auth.AccessToken
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.rest.personer.saksbilde.SaksbildeHenter
import no.nav.helse.spesialist.api.rest.personer.saksbilde.SnapshotKunneIkkeHentesException
import no.nav.helse.spesialist.api.rest.saksbilde.ApiSaksbildePerson
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersonPseudoIdProvider
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler

class PersonQueryHandler(
    private val saksbildeHenter: SaksbildeHenter,
    private val sessionFactory: SessionFactory,
    private val personPseudoIdProvider: PersonPseudoIdProvider,
    private val populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
) : PersonQuerySchema {
    override fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiSaksbildePerson?> {
        val personPseudoId =
            runCatching { PersonPseudoId.fraString(personPseudoId) }
                .getOrElse { badRequest("Ugyldig format på personPseudoId") }
        return medMdc(MdcKey.PERSON_PSEUDO_ID to personPseudoId.value.toString()) {
            sessionFactory.transactionalSessionScope { transaction ->
                val identitetsnummer =
                    personPseudoIdProvider.hentIdentitetsnummer(personPseudoId)
                        ?: run {
                            loggInfo("Fant ikke person basert på personPseudoId: ${personPseudoId.value}")
                            notFound("PseudoId er ugyldig eller utgått")
                        }

                medMdc(MdcKey.IDENTITETSNUMMER to identitetsnummer.value) {
                    hentPerson(
                        identitetsnummer = identitetsnummer,
                        transaction = transaction,
                        saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                        accessToken = env.graphQlContext.get(ContextValues.ACCESS_TOKEN),
                    )
                }
            }
        }
    }

    private fun hentPerson(
        identitetsnummer: Identitetsnummer,
        transaction: SessionContext,
        saksbehandler: Saksbehandler,
        accessToken: AccessToken,
    ): DataFetcherResult<ApiSaksbildePerson?> {
        val personEntity =
            transaction.personRepository.finnOrNull(identitetsnummer)
                ?: notFound("Fant ikke data for person")

        return when (
            val resultat =
                populasjonstilgangskontrollProvider.kontrollerKjerneTilgang(accessToken.value, personEntity.id.value)
        ) {
            TilgangskontrollResultat.IdentIkkeFunnet -> {
                internalServerError("Tilgangsmaskinen fant ikke saksbehandlers ident")
            }

            is TilgangskontrollResultat.ManglerTilgang -> {
                AuditLogger.loggManglendeTilgang(saksbehandler = saksbehandler, identitetsnummer = identitetsnummer)
                forbidden("Har ikke tilgang til person")
            }

            TilgangskontrollResultat.Ok -> {
                val person =
                    try {
                        saksbildeHenter.hent(personEntity, transaction)
                    } catch (_: SnapshotKunneIkkeHentesException) {
                        internalServerError("Feil ved henting av snapshot for person")
                    }
                AuditLogger.loggOk(saksbehandler = saksbehandler, identitetsnummer = identitetsnummer)
                byggRespons(person)
            }

            is TilgangskontrollResultat.UventetFeil -> {
                loggError("Uventet feil fra Tilgangsmaskinen", "feil" to resultat.menneskeligLesbarForklaring)
                internalServerError("Uventet feil fra Tilgangsmaskinen")
            }
        }
    }
}

private fun badRequest(message: String): Nothing = throw graphqlErrorException(400, message)

private fun forbidden(message: String): Nothing = throw graphqlErrorException(403, message)

private fun notFound(message: String): Nothing = throw graphqlErrorException(404, message)

private fun internalServerError(message: String): Nothing = throw graphqlErrorException(500, message)
