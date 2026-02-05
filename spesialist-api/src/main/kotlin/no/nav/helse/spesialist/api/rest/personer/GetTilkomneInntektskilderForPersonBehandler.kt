package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.api.rest.GetForPersonBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.ZoneId

class GetTilkomneInntektskilderForPersonBehandler :
    GetForPersonBehandler<Personer.PersonPseudoId.TilkomneInntektskilder, List<ApiTilkommenInntektskilde>, ApiGetTilkomneInntektskilderForPersonErrorCode>(
        personPseudoId = { resource -> resource.parent },
        personPseudoIdIkkeFunnet = ApiGetTilkomneInntektskilderForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET,
        manglerTilgangTilPerson = ApiGetTilkomneInntektskilderForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON,
    ) {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.TilkomneInntektskilder,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiTilkommenInntektskilde>, ApiGetTilkomneInntektskilderForPersonErrorCode> =
        RestResponse.OK(
            hentTilkomneInntektskilder(
                identitetsnummer = person.id,
                transaksjon = kallKontekst.transaksjon,
            ),
        )

    private fun hentTilkomneInntektskilder(
        identitetsnummer: Identitetsnummer,
        transaksjon: SessionContext,
    ): List<ApiTilkommenInntektskilde> =
        transaksjon.tilkommenInntektRepository
            .finnAlleForIdentitetsnummer(identitetsnummer)
            .groupBy { it.organisasjonsnummer }
            .map { (organisasjonsnummer, inntekter) ->
                ApiTilkommenInntektskilde(
                    organisasjonsnummer = organisasjonsnummer,
                    inntekter =
                        inntekter
                            .map { tilkommenInntekt ->
                                ApiTilkommenInntekt(
                                    tilkommenInntektId = tilkommenInntekt.id.value,
                                    organisasjonsnummer = organisasjonsnummer,
                                    periode =
                                        ApiDatoPeriode(
                                            fom = tilkommenInntekt.periode.fom,
                                            tom = tilkommenInntekt.periode.tom,
                                        ),
                                    periodebelop = tilkommenInntekt.periodebeløp,
                                    ekskluderteUkedager = tilkommenInntekt.ekskluderteUkedager.sorted(),
                                    fjernet = tilkommenInntekt.fjernet,
                                    erDelAvAktivTotrinnsvurdering =
                                        transaksjon.totrinnsvurderingRepository
                                            .finn(
                                                id = tilkommenInntekt.totrinnsvurderingId,
                                            )?.tilstand != TotrinnsvurderingTilstand.GODKJENT,
                                    events =
                                        tilkommenInntekt.events.map { event ->
                                            val metadata =
                                                ApiTilkommenInntektEvent.Metadata(
                                                    sekvensnummer = event.metadata.sekvensnummer,
                                                    tidspunkt =
                                                        event.metadata.tidspunkt
                                                            .atZone(ZoneId.of("Europe/Oslo"))
                                                            .toLocalDateTime(),
                                                    utfortAvSaksbehandlerIdent = event.metadata.utførtAvSaksbehandlerIdent.value,
                                                    notatTilBeslutter = event.metadata.notatTilBeslutter,
                                                )
                                            when (event) {
                                                is TilkommenInntektOpprettetEvent -> {
                                                    ApiTilkommenInntektOpprettetEvent(
                                                        metadata = metadata,
                                                        organisasjonsnummer = event.organisasjonsnummer,
                                                        periode =
                                                            ApiDatoPeriode(
                                                                fom = event.periode.fom,
                                                                tom = event.periode.tom,
                                                            ),
                                                        periodebelop = event.periodebeløp,
                                                        ekskluderteUkedager = event.ekskluderteUkedager.sorted(),
                                                    )
                                                }

                                                is TilkommenInntektEndretEvent -> {
                                                    ApiTilkommenInntektEndretEvent(
                                                        metadata = metadata,
                                                        endringer = event.endringer.toApiEndringer(),
                                                    )
                                                }

                                                is TilkommenInntektFjernetEvent -> {
                                                    ApiTilkommenInntektFjernetEvent(
                                                        metadata = metadata,
                                                    )
                                                }

                                                is TilkommenInntektGjenopprettetEvent -> {
                                                    ApiTilkommenInntektGjenopprettetEvent(
                                                        metadata = metadata,
                                                        endringer = event.endringer.toApiEndringer(),
                                                    )
                                                }
                                            }
                                        },
                                )
                            }.sortedBy { it.periode.fom },
                )
            }.sortedBy { it.organisasjonsnummer }

    private fun TilkommenInntektEvent.Endringer.toApiEndringer() =
        ApiTilkommenInntektEvent.Endringer(
            organisasjonsnummer = organisasjonsnummer?.tilApiEndring(),
            periode =
                periode?.let {
                    ApiTilkommenInntektEvent.Endringer.DatoPeriodeEndring(
                        fra = ApiDatoPeriode(fom = it.fra.fom, tom = it.fra.tom),
                        til = ApiDatoPeriode(fom = it.til.fom, tom = it.til.tom),
                    )
                },
            periodebelop = periodebeløp?.tilApiEndring(),
            ekskluderteUkedager =
                ekskluderteUkedager?.let {
                    ApiTilkommenInntektEvent.Endringer.ListLocalDateEndring(
                        fra = it.fra.toList(),
                        til = it.til.toList(),
                    )
                },
        )

    private fun Endring<String>.tilApiEndring(): ApiTilkommenInntektEvent.Endringer.StringEndring = ApiTilkommenInntektEvent.Endringer.StringEndring(fra = fra, til = til)

    private fun Endring<BigDecimal>.tilApiEndring(): ApiTilkommenInntektEvent.Endringer.BigDecimalEndring = ApiTilkommenInntektEvent.Endringer.BigDecimalEndring(fra = fra, til = til)

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
        }
    }
}

enum class ApiGetTilkomneInntektskilderForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
