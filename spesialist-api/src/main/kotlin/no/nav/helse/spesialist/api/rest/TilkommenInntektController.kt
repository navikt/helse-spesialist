package no.nav.helse.spesialist.api.rest

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.graphql.mutation.LeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequired
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequiredUUID
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.ZoneId
import java.util.UUID

class TilkommenInntektController(
    private val handler: RestHandler,
    private val tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) {
    data class LeggTilTilkommenInntektInput(
        val fodselsnummer: String,
        val verdier: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    data class TilkommenInntektUrlParametre(
        val tilkommenInntektId: UUID,
    )

    data class AktørUrlParametre(
        val aktørId: String,
    )

    data class EndreTilkommenInntektInput(
        val endretTil: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    data class FjernTilkommenInntektInput(
        val notatTilBeslutter: String,
    )

    data class GjenopprettTilkommenInntektInput(
        val endretTil: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    fun addToRoute(route: Route) {
        route.post("tidligere-mutations/tilkommen-inntekt/legg-til") {
            handler.handlePost(
                call = call,
                requestType = LeggTilTilkommenInntektInput::class,
            ) { _, request, saksbehandler, tilgangsgrupper, transaksjon ->
                leggTil(
                    request = request,
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    transaksjon = transaksjon,
                )
            }
        }
        route.route("tilkomne-inntekter/{tilkommenInntektId}") {
            post("endre") {
                handler.handlePost(
                    call = call,
                    requestType = EndreTilkommenInntektInput::class,
                ) { parametre, request, saksbehandler, tilgangsgrupper, transaksjon ->
                    endre(
                        parametre =
                            TilkommenInntektUrlParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            ),
                        request = request,
                        saksbehandler = saksbehandler,
                        tilgangsgrupper = tilgangsgrupper,
                        transaksjon = transaksjon,
                    )
                }
            }
            post("fjern") {
                handler.handlePost(
                    call = call,
                    requestType = FjernTilkommenInntektInput::class,
                ) { parametre, request, saksbehandler, tilgangsgrupper, transaksjon ->
                    fjern(
                        parametre =
                            TilkommenInntektUrlParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            ),
                        transaksjon = transaksjon,
                        saksbehandler = saksbehandler,
                        tilgangsgrupper = tilgangsgrupper,
                        request = request,
                    )
                }
            }
            post("gjenopprett") {
                handler.handlePost(
                    call = call,
                    requestType = GjenopprettTilkommenInntektInput::class,
                ) { parametre, request, saksbehandler, tilgangsgrupper, transaksjon ->
                    gjenopprett(
                        parametre =
                            TilkommenInntektUrlParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            ),
                        transaksjon = transaksjon,
                        saksbehandler = saksbehandler,
                        tilgangsgrupper = tilgangsgrupper,
                        request = request,
                    )
                }
            }
        }
        route.route("personer/{aktørId}/tilkomne-inntektskilder") {
            get {
                handler.handleGet(call) { parametre, saksbehandler, tilgangsgrupper, transaksjon ->
                    getTilkomneInntektskilder(
                        parametre =
                            AktørUrlParametre(
                                aktørId = parametre.getRequired("aktørId"),
                            ),
                        transaksjon = transaksjon,
                        saksbehandler = saksbehandler,
                        tilgangsgrupper = tilgangsgrupper,
                    )
                }
            }
        }
    }

    private fun getTilkomneInntektskilder(
        parametre: AktørUrlParametre,
        transaksjon: SessionContext,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): List<ApiTilkommenInntektskilde> {
        val aktørId = parametre.aktørId
        val fødselsnumre = transaksjon.personRepository.finnFødselsnumre(aktørId = aktørId).toSet()

        fødselsnumre.forEach { fødselsnummer ->
            handler.kontrollerTilgangTilPerson(
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                feilSupplier = ::HttpNotFound,
            )
        }

        return fødselsnumre.flatMap { fødselsnummer ->
            hentTilkomneInntektskilder(
                fødselsnummer = fødselsnummer,
                transaksjon = transaksjon,
            )
        }
    }

    private fun gjenopprett(
        parametre: TilkommenInntektUrlParametre,
        transaksjon: SessionContext,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        request: GjenopprettTilkommenInntektInput,
    ) {
        val tilkommenInntektId = parametre.tilkommenInntektId

        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId $tilkommenInntektId")

        handler.kontrollerTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        tilkommenInntektMutationHandler.gjenopprettTilkommenInntekt(
            tilkommenInntekt = tilkommenInntekt,
            endretTil = request.endretTil,
            notatTilBeslutter = request.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }

    private fun fjern(
        parametre: TilkommenInntektUrlParametre,
        transaksjon: SessionContext,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        request: FjernTilkommenInntektInput,
    ) {
        val tilkommenInntektId = parametre.tilkommenInntektId

        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId $tilkommenInntektId")

        handler.kontrollerTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        tilkommenInntektMutationHandler.fjernTilkommenInntekt(
            tilkommenInntekt = tilkommenInntekt,
            notatTilBeslutter = request.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }

    private fun leggTil(
        request: LeggTilTilkommenInntektInput,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): LeggTilTilkommenInntektResponse {
        handler.kontrollerTilgangTilPerson(
            fødselsnummer = request.fodselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        return tilkommenInntektMutationHandler.leggTilTilkommenInntekt(
            fodselsnummer = request.fodselsnummer,
            verdier = request.verdier,
            notatTilBeslutter = request.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }

    fun endre(
        parametre: TilkommenInntektUrlParametre,
        request: EndreTilkommenInntektInput,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ) {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(parametre.tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId ${parametre.tilkommenInntektId}")

        handler.kontrollerTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        tilkommenInntektMutationHandler.endreTilkommenInntekt(
            tilkommenInntekt = tilkommenInntekt,
            endretTil = request.endretTil,
            notatTilBeslutter = request.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }

    private fun hentTilkomneInntektskilder(
        fødselsnummer: String,
        transaksjon: SessionContext,
    ): List<ApiTilkommenInntektskilde> =
        transaksjon.tilkommenInntektRepository
            .finnAlleForFødselsnummer(fødselsnummer)
            .groupBy { it.organisasjonsnummer }
            .map { (organisasjonsnummer, inntekter) ->
                ApiTilkommenInntektskilde(
                    organisasjonsnummer = organisasjonsnummer,
                    inntekter =
                        inntekter
                            .map { tilkommenInntekt ->
                                ApiTilkommenInntekt(
                                    tilkommenInntektId = tilkommenInntekt.id().value,
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
                                                    utfortAvSaksbehandlerIdent = event.metadata.utførtAvSaksbehandlerIdent,
                                                    notatTilBeslutter = event.metadata.notatTilBeslutter,
                                                )
                                            when (event) {
                                                is TilkommenInntektOpprettetEvent ->
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

                                                is TilkommenInntektEndretEvent ->
                                                    ApiTilkommenInntektEndretEvent(
                                                        metadata = metadata,
                                                        endringer = event.endringer.toApiEndringer(),
                                                    )

                                                is TilkommenInntektFjernetEvent ->
                                                    ApiTilkommenInntektFjernetEvent(
                                                        metadata = metadata,
                                                    )

                                                is TilkommenInntektGjenopprettetEvent ->
                                                    ApiTilkommenInntektGjenopprettetEvent(
                                                        metadata = metadata,
                                                        endringer = event.endringer.toApiEndringer(),
                                                    )
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
}
