package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEndretEvent
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserValidator

class PostGraderteAndreYtelserBehandler : PostBehandler<Personer.PersonPseudoId.GraderteAndreYtelser, ApiEndreGraderteAndreYtelserRequest, ApiEndreGraderteAndreYtelserResponse, ApiPostGraderteAndreYtelserErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: Personer.PersonPseudoId.GraderteAndreYtelser,
        request: ApiEndreGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiEndreGraderteAndreYtelserResponse, ApiPostGraderteAndreYtelserErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPostGraderteAndreYtelserErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostGraderteAndreYtelserErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(request, person, kallKontekst)
        }

    private fun behandleForPerson(
        request: ApiEndreGraderteAndreYtelserRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiEndreGraderteAndreYtelserResponse, ApiPostGraderteAndreYtelserErrorCode> {
        val dager = request.dager.map { it.tilGraderteAndreYtelserDag() }

        when (GraderteAndreYtelserValidator.validerDager(dager)) {
            is GraderteAndreYtelserValidator.Resultat.OK -> {}
            is GraderteAndreYtelserValidator.Resultat.ManglerEndringer ->
                return RestResponse.Error(ApiPostGraderteAndreYtelserErrorCode.MANGLER_ENDRINGER)

            is GraderteAndreYtelserValidator.Resultat.NegativGrad ->
                return RestResponse.Error(ApiPostGraderteAndreYtelserErrorCode.NEGATIV_GRAD)

            is GraderteAndreYtelserValidator.Resultat.TotalGradOver100 ->
                return RestResponse.Error(ApiPostGraderteAndreYtelserErrorCode.TOTAL_GRAD_OVER_100)
        }

        val graderteAndreYtelserId =
            GraderteAndreYtelserId(
                identitetsnummer = person.id,
                yrkesaktivitet = Yrkesaktivitetstype.valueOf(request.yrkesaktivitet.name),
                organisasjonsnummer = request.organisasjonsnummer,
            )
        val repository = kallKontekst.transaksjon.graderteAndreYtelserRepository
        val eksisterende = repository.finnOrNull(graderteAndreYtelserId)

        if (request.versjon != eksisterende?.versjon) {
            loggInfo("Versjonen i forespørselen (${request.versjon}) matcher ikke versjon (${eksisterende?.versjon})")
            return RestResponse.Error(ApiPostGraderteAndreYtelserErrorCode.UTDATERT_TILSTAND)
        }

        val graderteAndreYtelser: GraderteAndreYtelser
        val event: GraderteAndreYtelserEndretEvent?
        if (eksisterende == null) {
            graderteAndreYtelser =
                GraderteAndreYtelser.ny(
                    id = graderteAndreYtelserId,
                    dager = dager,
                    saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                    notatTilBeslutter = request.notatTilBeslutter,
                )
            event = graderteAndreYtelser.events.single()
        } else {
            graderteAndreYtelser = eksisterende
            event =
                eksisterende.endre(
                    dager = dager,
                    saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                    notatTilBeslutter = request.notatTilBeslutter,
                )
        }

        if (event == null) {
            loggInfo("Ingen endringer i graderte andre ytelser, lagrer ikke ny versjon")
            return RestResponse.OK(ApiEndreGraderteAndreYtelserResponse(graderteAndreYtelser.versjon))
        }

        repository.lagre(graderteAndreYtelser)

        kallKontekst.leggTilGraderteAndreYtelserEndringsevent(
            identitetsnummer = person.id,
            event = event,
            årsak = "graderte andre ytelser endret",
        )

        loggInfo("Lagret ny versjon av graderte andre ytelser (versjon: ${event.versjon})")

        return RestResponse.OK(ApiEndreGraderteAndreYtelserResponse(event.versjon))
    }

    private fun ApiGraderteAndreYtelserDag.tilGraderteAndreYtelserDag() =
        GraderteAndreYtelserDag(
            dato = dato,
            andreYtelserGrader = andreYtelserGrader.mapKeys { (type, _) -> type.tilGraderteAndreYtelserType() },
            sykdomsgrad = sykdomsgrad,
            arbeidsgrad = arbeidsgrad,
        )

    private fun ApiGraderteAndreYtelserType.tilGraderteAndreYtelserType() =
        when (this) {
            ApiGraderteAndreYtelserType.FORELDREPENGER -> GraderteAndreYtelserType.FORELDREPENGER
            ApiGraderteAndreYtelserType.SVANGERSKAPSPENGER -> GraderteAndreYtelserType.SVANGERSKAPSPENGER
            ApiGraderteAndreYtelserType.OMSORGSPENGER -> GraderteAndreYtelserType.OMSORGSPENGER
            ApiGraderteAndreYtelserType.PLEIEPENGER -> GraderteAndreYtelserType.PLEIEPENGER
            ApiGraderteAndreYtelserType.OPPLARINGSPENGER -> GraderteAndreYtelserType.OPPLARINGSPENGER
        }
}

enum class ApiPostGraderteAndreYtelserErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    TOTAL_GRAD_OVER_100("Total grad for en dag kan ikke overstige 100", HttpStatusCode.BadRequest),
    MANGLER_ENDRINGER("Mangler endringer", HttpStatusCode.BadRequest),
    NEGATIV_GRAD("Grad kan ikke være negativ", HttpStatusCode.BadRequest),
    UTDATERT_TILSTAND("Graderte andre ytelser er endret siden de ble hentet", HttpStatusCode.Conflict),
}
