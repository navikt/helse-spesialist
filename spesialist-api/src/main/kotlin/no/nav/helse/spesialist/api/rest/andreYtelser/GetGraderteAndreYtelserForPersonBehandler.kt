package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserEvent.Metadata
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.andreytelser.*
import java.time.ZoneId

class GetGraderteAndreYtelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.GraderteAndreYtelser, List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: Personer.PersonPseudoId.GraderteAndreYtelser,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetGraderteAndreYtelserForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetGraderteAndreYtelserForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val ytelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository
                    .finnAlleForIdentitetsnummer(person.id)
                    .map { it.tilApiGraderteAndreYtelser() }

            loggInfo("Hentet ${ytelser.size} graderte andre ytelser")

            RestResponse.OK(ytelser)
        }

    private fun GraderteAndreYtelser.tilApiGraderteAndreYtelser() =
        ApiGraderteAndreYtelser(
            andreYtelserId = id.value,
            andreYtelserType = Mapping.tilApiGraderteAndreYtelserType(graderteAndreYtelserType),
            events = events.map(Mapping::tilApiGraderteAndreYtelserEvent),
        )

    object Mapping {
        fun tilApiGraderteAndreYtelserEvent(event: GraderteAndreYtelserEvent): ApiGraderteAndreYtelserEvent =
            when (event) {
                is GraderteAndreYtelserOpprettetEvent -> event.tilApiGraderteAndreYtelserOpprettetEvent()
            }

        private fun GraderteAndreYtelserOpprettetEvent.tilApiGraderteAndreYtelserOpprettetEvent(): ApiGraderteAndreYtelserOpprettetEvent =
            ApiGraderteAndreYtelserOpprettetEvent(
                metadata = tilApiGraderteAndreYtelserEventMetadata(),
                andreYtelserType = tilApiGraderteAndreYtelserType(graderteAndreYtelserType),
            )

        private fun GraderteAndreYtelserEvent.tilApiGraderteAndreYtelserEventMetadata(): Metadata =
            Metadata(
                sekvensnummer = metadata.sekvensnummer,
                tidspunkt = metadata.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                utfortAvSaksbehandlerIdent = metadata.utførtAvSaksbehandlerIdent.value,
                notatTilBeslutter = metadata.notatTilBeslutter,
            )

        fun tilApiGraderteAndreYtelserType(type: GraderteAndreYtelserType) =
            when (type) {
                GraderteAndreYtelserType.FORELDREPENGER -> ApiGraderteAndreYtelserType.FORELDREPENGER
                GraderteAndreYtelserType.SVANGERSKAPSPENGER -> ApiGraderteAndreYtelserType.SVANGERSKAPSPENGER
                GraderteAndreYtelserType.OMSORGSPENGER -> ApiGraderteAndreYtelserType.OMSORGSPENGER
                GraderteAndreYtelserType.PLEIEPENGER -> ApiGraderteAndreYtelserType.PLEIEPENGER
                GraderteAndreYtelserType.OPPLARINGSPENGER -> ApiGraderteAndreYtelserType.OPPLARINGSPENGER
            }
    }
}

enum class ApiGetGraderteAndreYtelserForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
