package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.PersonTilgangskontroll
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.ZoneId

class TilkommenInntektQueryHandler(
    private val sessionFactory: SessionFactory,
    private val daos: Daos,
) : TilkommenInntektQuerySchema {
    override suspend fun tilkomneInntektskilder(
        aktorId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiTilkommenInntektskilde>> {
        val tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER)
        val fødselsnumre = daos.personApiDao.finnFødselsnumre(aktorId).toSet()
        return tilkomneInntekterForFødselsnummer(fødselsnumre, tilgangsgrupper)
    }

    override suspend fun tilkomneInntektskilderV2(
        fodselsnummer: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiTilkommenInntektskilde>> {
        val tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER)
        return tilkomneInntekterForFødselsnummer(setOf(fodselsnummer), tilgangsgrupper)
    }

    private fun tilkomneInntekterForFødselsnummer(
        fødselsnumre: Set<String>,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): DataFetcherResult<List<ApiTilkommenInntektskilde>> {
        fødselsnumre.forEach { fødselsnummer ->
            if (!PersonTilgangskontroll(
                    egenAnsattApiDao = daos.egenAnsattApiDao,
                    personApiDao = daos.personApiDao,
                ).harTilgangTilPerson(
                    tilgangsgrupper = tilgangsgrupper,
                    fødselsnummer = fødselsnummer,
                )
            ) {
                sikkerlogg.info("Saksbehandler mangler nødvendig tilgang til fødselsnummer $fødselsnummer")
                error("Ikke tilgang")
            }
        }
        val tilkomneInntektskilder =
            sessionFactory.transactionalSessionScope { sessionContext ->
                fødselsnumre.flatMap { fødselsnummer ->
                    hentTilkomneInntektskilder(fødselsnummer = fødselsnummer, sessionContext = sessionContext)
                }
            }
        return DataFetcherResult.newResult<List<ApiTilkommenInntektskilde>>().data(tilkomneInntektskilder).build()
    }

    private fun hentTilkomneInntektskilder(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ): List<ApiTilkommenInntektskilde> =
        sessionContext.tilkommenInntektRepository
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
                                        sessionFactory.transactionalSessionScope {
                                            it.totrinnsvurderingRepository
                                                .finn(
                                                    id = tilkommenInntekt.totrinnsvurderingId,
                                                )?.tilstand != TotrinnsvurderingTilstand.GODKJENT
                                        },
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
