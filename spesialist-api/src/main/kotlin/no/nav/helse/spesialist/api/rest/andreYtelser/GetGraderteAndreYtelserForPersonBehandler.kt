package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserEvent.Metadata
import no.nav.helse.spesialist.api.rest.andreYtelser.GetGraderteAndreYtelserForPersonBehandler.Mapping.mapTilApiPerioder
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.andreytelser.*
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import java.time.ZoneId

class GetGraderteAndreYtelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.GraderteAndreYtelser, List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: Personer.PersonPseudoId.GraderteAndreYtelser,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetGraderteAndreYtelserForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetGraderteAndreYtelserForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val ytelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository
                    .finnAlleForIdentitetsnummer(person.id)
                    .map { it.tilApiGraderteAndreYtelser() }
                    .sortedBy { graderteAndreYtelser -> graderteAndreYtelser.perioder.minOf { it.fom } }

            loggInfo("Hentet ${ytelser.size} graderte andre ytelser")

            RestResponse.OK(ytelser)
        }

    private fun GraderteAndreYtelser.tilApiGraderteAndreYtelser() =
        ApiGraderteAndreYtelser(
            andreYtelserId = id.value,
            perioder = mapTilApiPerioder(perioder),
            andreYtelserType = Mapping.tilApiGraderteAndreYtelserType(graderteAndreYtelserType),
            fjernet = fjernet,
            events = events.map(Mapping::tilApiGraderteAndreYtelserEvent),
        )

    object Mapping {
        fun tilApiGraderteAndreYtelserEvent(event: GraderteAndreYtelserEvent): ApiGraderteAndreYtelserEvent =
            when (event) {
                is GraderteAndreYtelserEndretEvent -> event.tilApiGraderteAndreYtelserEndretEvent()
                is GraderteAndreYtelserFjernetEvent -> event.tilApiGraderteAndreYtelserFjernetEvent()
                is GraderteAndreYtelserGjenopprettetEvent -> event.tilApiGraderteAndreYtelserGjenopprettetEvent()
                is GraderteAndreYtelserOpprettetEvent -> event.tilApiGraderteAndreYtelserOpprettetEvent()
            }

        private fun GraderteAndreYtelserGjenopprettetEvent.tilApiGraderteAndreYtelserGjenopprettetEvent(): ApiGraderteAndreYtelserGjenopprettetEvent =
            ApiGraderteAndreYtelserGjenopprettetEvent(
                metadata = tilApiGraderteAndreYtelserEventMetadata(),
                endringer = endringer.tilApiEndringer(),
            )

        private fun GraderteAndreYtelserFjernetEvent.tilApiGraderteAndreYtelserFjernetEvent(): ApiGraderteAndreYtelserFjernetEvent =
            ApiGraderteAndreYtelserFjernetEvent(
                metadata = tilApiGraderteAndreYtelserEventMetadata(),
            )

        private fun GraderteAndreYtelserEndretEvent.tilApiGraderteAndreYtelserEndretEvent(): ApiGraderteAndreYtelserEndretEvent =
            ApiGraderteAndreYtelserEndretEvent(
                metadata = tilApiGraderteAndreYtelserEventMetadata(),
                endringer = endringer.tilApiEndringer(),
            )

        private fun GraderteAndreYtelserOpprettetEvent.tilApiGraderteAndreYtelserOpprettetEvent(): ApiGraderteAndreYtelserOpprettetEvent =
            ApiGraderteAndreYtelserOpprettetEvent(
                metadata = tilApiGraderteAndreYtelserEventMetadata(),
                andreYtelserType = tilApiGraderteAndreYtelserType(graderteAndreYtelserType),
                perioder = mapTilApiPerioder(graderteAndreYtelserPerioder),
            )

        private fun GraderteAndreYtelserEvent.tilApiGraderteAndreYtelserEventMetadata(): Metadata =
            Metadata(
                sekvensnummer = metadata.sekvensnummer,
                tidspunkt = metadata.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                utfortAvSaksbehandlerIdent = metadata.utførtAvSaksbehandlerIdent.value,
                notatTilBeslutter = metadata.notatTilBeslutter,
            )

        private fun mapPeriodeliste(domenetype: List<GraderteAndreYtelserPeriode>): List<ApiGraderteAndreYtelserEvent.ApiGradertAnnenYtelse> =
            domenetype.map {
                ApiGraderteAndreYtelserEvent.ApiGradertAnnenYtelse(
                    periode = ApiDatoPeriode(it.periode.fom, it.periode.tom),
                    grad = it.grad,
                )
            }

        private fun GraderteAndreYtelserEvent.Endringer.tilApiEndringer(): ApiGraderteAndreYtelserEvent.Endringer {
            val perioder =
                graderteAndreYtelserPerioder?.let { perioder ->
                    ApiGraderteAndreYtelserEvent.ListGradertAnnenYtelseEndring(
                        fra = mapPeriodeliste(perioder.fra),
                        til = mapPeriodeliste(perioder.til),
                    )
                }
            val type =
                graderteAndreYtelserType?.let { type ->
                    ApiGraderteAndreYtelserEvent.GradertAnnenYtelseTypeEndring(
                        fra = tilApiGraderteAndreYtelserType(type.fra),
                        til = tilApiGraderteAndreYtelserType(type.til),
                    )
                }
            return ApiGraderteAndreYtelserEvent.Endringer(
                perioder = perioder,
                andreYtelserType = type,
            )
        }

        fun tilApiGraderteAndreYtelserType(type: GraderteAndreYtelserType) =
            when (type) {
                GraderteAndreYtelserType.FORELDREPENGER -> ApiGraderteAndreYtelserType.FORELDREPENGER
                GraderteAndreYtelserType.SVANGERSKAPSPENGER -> ApiGraderteAndreYtelserType.SVANGERSKAPSPENGER
                GraderteAndreYtelserType.OMSORGSPENGER -> ApiGraderteAndreYtelserType.OMSORGSPENGER
                GraderteAndreYtelserType.PLEIEPENGER -> ApiGraderteAndreYtelserType.PLEIEPENGER
                GraderteAndreYtelserType.OPPLARINGSPENGER -> ApiGraderteAndreYtelserType.OPPLARINGSPENGER
            }

        fun mapTilApiPerioder(domeneperioder: Collection<GraderteAndreYtelserPeriode>): List<ApiGraderteAndreYtelserPeriode> =
            domeneperioder.map { gradertAnnenYtelsePeriode ->
                ApiGraderteAndreYtelserPeriode(
                    fom = gradertAnnenYtelsePeriode.periode.fom,
                    tom = gradertAnnenYtelsePeriode.periode.tom,
                    grad = gradertAnnenYtelsePeriode.grad,
                )
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
