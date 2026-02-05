package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektEvent.Metadata
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.SortedSet

class GetTilkomneInntektskilderForPersonBehandler : GetBehandler<Personer.PersonPseudoId.TilkomneInntektskilder, List<ApiTilkommenInntektskilde>, ApiGetTilkomneInntektskilderForPersonErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Personer.PersonPseudoId.TilkomneInntektskilder,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiTilkommenInntektskilde>, ApiGetTilkomneInntektskilderForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetTilkomneInntektskilderForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetTilkomneInntektskilderForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(person, kallKontekst)
        }

    private fun behandleForPerson(
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiTilkommenInntektskilde>, ApiGetTilkomneInntektskilderForPersonErrorCode> {
        val tilkomneInntektskilder =
            kallKontekst.transaksjon.tilkommenInntektRepository
                .finnAlleForIdentitetsnummer(identitetsnummer = person.id)
                .groupBy { it.organisasjonsnummer }
                .map { (organisasjonsnummer, inntekter) ->
                    tilApiTilkommenInntektskilde(
                        organisasjonsnummer = organisasjonsnummer,
                        inntekter = inntekter,
                        kallKontekst = kallKontekst,
                    )
                }.sortedBy { it.organisasjonsnummer }

        loggInfo("Hentet ${tilkomneInntektskilder.size} tilkomne inntektskilder")

        return RestResponse.OK(tilkomneInntektskilder)
    }

    private fun tilApiTilkommenInntektskilde(
        organisasjonsnummer: String,
        inntekter: List<TilkommenInntekt>,
        kallKontekst: KallKontekst,
    ): ApiTilkommenInntektskilde =
        ApiTilkommenInntektskilde(
            organisasjonsnummer = organisasjonsnummer,
            inntekter =
                inntekter
                    .map { tilApiTilkommenInntekt(it, organisasjonsnummer, kallKontekst) }
                    .sortedBy { it.periode.fom },
        )

    private fun tilApiTilkommenInntekt(
        tilkommenInntekt: TilkommenInntekt,
        organisasjonsnummer: String,
        kallKontekst: KallKontekst,
    ): ApiTilkommenInntekt =
        ApiTilkommenInntekt(
            tilkommenInntektId = tilkommenInntekt.id.value,
            organisasjonsnummer = organisasjonsnummer,
            periode = tilkommenInntekt.periode.tilApiDatoPeriode(),
            periodebelop = tilkommenInntekt.periodebeløp,
            ekskluderteUkedager = tilkommenInntekt.ekskluderteUkedager.sorted(),
            fjernet = tilkommenInntekt.fjernet,
            erDelAvAktivTotrinnsvurdering =
                kallKontekst.transaksjon.totrinnsvurderingRepository
                    .finn(id = tilkommenInntekt.totrinnsvurderingId)
                    ?.tilstand != TotrinnsvurderingTilstand.GODKJENT,
            events = tilkommenInntekt.events.map { it.tilApiTilkommenInntektEvent() },
        )

    private fun Periode.tilApiDatoPeriode(): ApiDatoPeriode = ApiDatoPeriode(fom = fom, tom = tom)

    private fun TilkommenInntektEvent.tilApiTilkommenInntektEvent(): ApiTilkommenInntektEvent =
        when (this) {
            is TilkommenInntektOpprettetEvent -> tilApiTilkommenInntektOpprettetEvent()
            is TilkommenInntektEndretEvent -> tilApiTilkommenInntektEndretEvent()
            is TilkommenInntektFjernetEvent -> tilApiTilkommenInntektFjernetEvent()
            is TilkommenInntektGjenopprettetEvent -> tilApiTilkommenInntektGjenopprettetEvent()
        }

    private fun TilkommenInntektGjenopprettetEvent.tilApiTilkommenInntektGjenopprettetEvent(): ApiTilkommenInntektGjenopprettetEvent =
        ApiTilkommenInntektGjenopprettetEvent(
            metadata = tilApiTilkommenInntektEventMetadata(),
            endringer = endringer.tilApiEndringer(),
        )

    private fun TilkommenInntektFjernetEvent.tilApiTilkommenInntektFjernetEvent(): ApiTilkommenInntektFjernetEvent =
        ApiTilkommenInntektFjernetEvent(
            metadata = tilApiTilkommenInntektEventMetadata(),
        )

    private fun TilkommenInntektEndretEvent.tilApiTilkommenInntektEndretEvent(): ApiTilkommenInntektEndretEvent =
        ApiTilkommenInntektEndretEvent(
            metadata = tilApiTilkommenInntektEventMetadata(),
            endringer = endringer.tilApiEndringer(),
        )

    private fun TilkommenInntektOpprettetEvent.tilApiTilkommenInntektOpprettetEvent(): ApiTilkommenInntektOpprettetEvent =
        ApiTilkommenInntektOpprettetEvent(
            metadata = tilApiTilkommenInntektEventMetadata(),
            organisasjonsnummer = organisasjonsnummer,
            periode = periode.tilApiDatoPeriode(),
            periodebelop = periodebeløp,
            ekskluderteUkedager = ekskluderteUkedager.sorted(),
        )

    private fun TilkommenInntektEvent.tilApiTilkommenInntektEventMetadata(): Metadata =
        Metadata(
            sekvensnummer = metadata.sekvensnummer,
            tidspunkt = metadata.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
            utfortAvSaksbehandlerIdent = metadata.utførtAvSaksbehandlerIdent.value,
            notatTilBeslutter = metadata.notatTilBeslutter,
        )

    private fun TilkommenInntektEvent.Endringer.tilApiEndringer() =
        ApiTilkommenInntektEvent.Endringer(
            organisasjonsnummer = organisasjonsnummer?.tilApiStringEndring(),
            periode = periode?.tilApiDatoPeriodeEndring(),
            periodebelop = periodebeløp?.tilApiBigDecimalEndring(),
            ekskluderteUkedager = ekskluderteUkedager?.tilApiListLocalDateEndring(),
        )

    private fun Endring<String>.tilApiStringEndring(): ApiTilkommenInntektEvent.Endringer.StringEndring = ApiTilkommenInntektEvent.Endringer.StringEndring(fra = fra, til = til)

    private fun Endring<Periode>.tilApiDatoPeriodeEndring(): ApiTilkommenInntektEvent.Endringer.DatoPeriodeEndring =
        ApiTilkommenInntektEvent.Endringer.DatoPeriodeEndring(
            fra = fra.tilApiDatoPeriode(),
            til = til.tilApiDatoPeriode(),
        )

    private fun Endring<BigDecimal>.tilApiBigDecimalEndring(): ApiTilkommenInntektEvent.Endringer.BigDecimalEndring = ApiTilkommenInntektEvent.Endringer.BigDecimalEndring(fra = fra, til = til)

    private fun Endring<SortedSet<LocalDate>>.tilApiListLocalDateEndring(): ApiTilkommenInntektEvent.Endringer.ListLocalDateEndring = ApiTilkommenInntektEvent.Endringer.ListLocalDateEndring(fra = fra.toList(), til = til.toList())

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
