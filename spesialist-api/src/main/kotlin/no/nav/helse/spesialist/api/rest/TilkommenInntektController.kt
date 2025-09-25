package no.nav.helse.spesialist.api.rest

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequired
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequiredUUID
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektEndreHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektFjernHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektGjenopprettHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektLeggTilHåndterer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.ZoneId

class TilkommenInntektController(
    private val handler: RestHandler,
    private val tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) {
    data class AktørUrlParametre(
        val aktørId: String,
    )

    fun addToRoute(route: Route) {
        route.post("tidligere-mutations/tilkommen-inntekt/legg-til") {
            handler.håndterPost(
                call = call,
                håndterer = TilkommenInntektLeggTilHåndterer(handler, tilkommenInntektMutationHandler),
                parameterTolkning = { },
            )
        }
        route.route("tilkomne-inntekter") {
            post("legg-til") {
                handler.håndterPost(
                    call = call,
                    håndterer = TilkommenInntektLeggTilHåndterer(handler, tilkommenInntektMutationHandler),
                    parameterTolkning = { },
                )
            }
            route("{tilkommenInntektId}") {
                post("endre") {
                    handler.håndterPost(
                        call = call,
                        håndterer = TilkommenInntektEndreHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            TilkommenInntektEndreHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
                    )
                }
                post("fjern") {
                    handler.håndterPost(
                        call = call,
                        håndterer = TilkommenInntektFjernHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            TilkommenInntektFjernHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
                    )
                }
                post("gjenopprett") {
                    handler.håndterPost(
                        call = call,
                        håndterer = TilkommenInntektGjenopprettHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            TilkommenInntektGjenopprettHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
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
