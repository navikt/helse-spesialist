package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.andreYtelser.GetGraderteAndreYtelserForPersonBehandler.Mapping.mapTilApiDag
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
                    .sortedBy { graderteAndreYtelser -> graderteAndreYtelser.dager.minOf { it.dato } }

            loggInfo("Hentet ${ytelser.size} graderte andre ytelser")

            RestResponse.OK(ytelser)
        }

    private fun GraderteAndreYtelser.tilApiGraderteAndreYtelser() =
        ApiGraderteAndreYtelser(
            yrkesaktivitet = ApiYrkesaktivitetstype.valueOf(id.yrkesaktivitet.name),
            organisasjonsnummer = id.organisasjonsnummer,
            versjon = versjon,
            dager = mapTilApiDag(dager),
            events = events.map(Mapping::tilApiGraderteAndreYtelserEndretEvent),
        )

    object Mapping {
        fun tilApiGraderteAndreYtelserEndretEvent(event: GraderteAndreYtelserEndretEvent) =
            ApiGraderteAndreYtelserEndretEvent(
                versjon = event.versjon,
                tidspunkt = event.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                utfortAvSaksbehandlerIdent = event.utførtAvSaksbehandlerIdent.value,
                notatTilBeslutter = event.notatTilBeslutter,
                dager = event.dager.map(::tilApiGraderteAndreYtelserEndretDag),
            )

        private fun tilApiGraderteAndreYtelserEndretDag(dag: GraderteAndreYtelserEndretDag) =
            ApiGraderteAndreYtelserEndretDag(
                dato = dag.dato,
                andreYtelserGrader = dag.andreYtelserGrader.mapKeys { (type, _) -> tilApiGraderteAndreYtelserType(type) },
                sykdomsgrad = dag.sykdomsgrad,
                arbeidsgrad = dag.arbeidsgrad,
                fraAndreYtelserGrader = dag.fraAndreYtelserGrader?.mapKeys { (type, _) -> tilApiGraderteAndreYtelserType(type) },
                fraSykdomsgrad = dag.fraSykdomsgrad,
                fraArbeidsgrad = dag.fraArbeidsgrad,
            )

        fun tilApiGraderteAndreYtelserType(type: GraderteAndreYtelserType) =
            when (type) {
                GraderteAndreYtelserType.FORELDREPENGER -> ApiGraderteAndreYtelserType.FORELDREPENGER
                GraderteAndreYtelserType.SVANGERSKAPSPENGER -> ApiGraderteAndreYtelserType.SVANGERSKAPSPENGER
                GraderteAndreYtelserType.OMSORGSPENGER -> ApiGraderteAndreYtelserType.OMSORGSPENGER
                GraderteAndreYtelserType.PLEIEPENGER -> ApiGraderteAndreYtelserType.PLEIEPENGER
                GraderteAndreYtelserType.OPPLARINGSPENGER -> ApiGraderteAndreYtelserType.OPPLARINGSPENGER
            }

        fun mapTilApiDag(dag: Collection<GraderteAndreYtelserDag>): List<ApiGraderteAndreYtelserDag> =
            dag.map { gradertAnnenYtelserDag ->
                ApiGraderteAndreYtelserDag(
                    dato = gradertAnnenYtelserDag.dato,
                    andreYtelserGrader = gradertAnnenYtelserDag.andreYtelserGrader.mapKeys { (key, _) -> tilApiGraderteAndreYtelserType(key) },
                    sykdomsgrad = gradertAnnenYtelserDag.sykdomsgrad,
                    arbeidsgrad = gradertAnnenYtelserDag.arbeidsgrad,
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
