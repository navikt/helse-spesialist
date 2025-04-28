package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEndretEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektFjernetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOpprettetEvent
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.api.saksbehandler.manglerTilgang
import no.nav.helse.spesialist.application.logg.sikkerlogg
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
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER)
        val fødselsnumre = daos.personApiDao.finnFødselsnumre(aktorId).toSet()
        fødselsnumre.forEach { fødselsnummer ->
            if (manglerTilgang(
                    egenAnsattApiDao = daos.egenAnsattApiDao,
                    personApiDao = daos.personApiDao,
                    fnr = fødselsnummer,
                    tilganger = tilganger,
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
        sessionContext.tilkommenInntektRepository.finnAlleForFødselsnummer(fødselsnummer)
            .groupBy { it.organisasjonsnummer }
            .map { (organisasjonsnummer, inntekter) ->
                ApiTilkommenInntektskilde(
                    organisasjonsnummer = organisasjonsnummer,
                    inntekter =
                        inntekter.map {
                            ApiTilkommenInntekt(
                                tilkommenInntektId = it.id().value,
                                periode =
                                    ApiDatoPeriode(
                                        fom = it.periode.fom,
                                        tom = it.periode.tom,
                                    ),
                                periodebelop = it.periodebeløp,
                                dager = it.dager.sorted(),
                                fjernet = it.fjernet,
                                events =
                                    it.events.map { event ->
                                        val metadata =
                                            ApiTilkommenInntektEvent.Metadata(
                                                sekvensnummer = event.metadata.sekvensnummer,
                                                tidspunkt =
                                                    event.metadata.tidspunkt.atZone(ZoneId.of("Europe/Oslo"))
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
                                                    dager = event.dager.sorted(),
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
            }
            .sortedBy { it.organisasjonsnummer }

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
            dager =
                dager?.let {
                    ApiTilkommenInntektEvent.Endringer.ListLocalDateEndring(
                        fra = it.fra.toList(),
                        til = it.til.toList(),
                    )
                },
        )

    private fun Endring<String>.tilApiEndring(): ApiTilkommenInntektEvent.Endringer.StringEndring =
        ApiTilkommenInntektEvent.Endringer.StringEndring(fra = fra, til = til)

    private fun Endring<BigDecimal>.tilApiEndring(): ApiTilkommenInntektEvent.Endringer.BigDecimalEndring =
        ApiTilkommenInntektEvent.Endringer.BigDecimalEndring(fra = fra, til = til)
}
