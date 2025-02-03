package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.feilhåndtering.IkkeÅpenOppgave
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val godkjenninghåndterer: Godkjenninghåndterer,
) : Mutation {
    private companion object {
        private val logg = LoggerFactory.getLogger(VedtakMutation::class.java)
    }

    @Suppress("unused")
    suspend fun fattVedtak(
        oppgavereferanse: String,
        env: DataFetchingEnvironment,
        utfall: ApiVedtakUtfall,
        begrunnelse: String? = null,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            logg.info("Fatter vedtak for oppgave $oppgavereferanse")

            val resultat =
                saksbehandlerhåndterer.vedtak(
                    saksbehandlerFraApi = saksbehandler,
                    oppgavereferanse = oppgavereferanse.toLong(),
                    godkjent = true,
                    utfall = utfall,
                    begrunnelse = begrunnelse,
                )
            when (resultat) {
                is VedtakResultat.Ok -> {
                    val dto =
                        GodkjenningDto(
                            oppgavereferanse = oppgavereferanse.toLong(),
                            godkjent = true,
                            saksbehandlerIdent = saksbehandler.ident,
                            årsak = null,
                            begrunnelser = null,
                            kommentar = null,
                            avslag = null,
                        )
                    godkjenninghåndterer.håndter(dto, saksbehandler.epost, saksbehandler.oid)
                    newResult<Boolean>().data(true).build()
                }

                is VedtakResultat.Feil -> {
                    logg.warn("Kunne ikke innvilge vedtak: ${resultat.melding}")
                    newResult<Boolean>().error(vedtakGraphQLError(resultat.melding, resultat.code, resultat.exception))
                        .data(false).build()
                }
            }
        }

    @Suppress("unused")
    suspend fun sendTilInfotrygd(
        oppgavereferanse: String,
        arsak: String,
        begrunnelser: List<String>,
        kommentar: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            logg.info("Sender oppgave $oppgavereferanse til Infotrygd")

            val resultat =
                saksbehandlerhåndterer.infotrygdVedtak(
                    saksbehandlerFraApi = saksbehandler,
                    oppgavereferanse = oppgavereferanse.toLong(),
                    godkjent = false,
                )
            when (resultat) {
                is VedtakResultat.Ok -> {
                    val godkjenning =
                        GodkjenningDto(
                            oppgavereferanse = oppgavereferanse.toLong(),
                            godkjent = false,
                            saksbehandlerIdent = saksbehandler.ident,
                            årsak = arsak,
                            begrunnelser = begrunnelser,
                            kommentar = kommentar,
                        )
                    godkjenninghåndterer.håndter(godkjenning, saksbehandler.epost, saksbehandler.oid)
                    newResult<Boolean>().data(true).build()
                }

                is VedtakResultat.Feil -> {
                    logg.warn("Kunne ikke sende oppgave til Infotrygd: ${resultat.melding}")
                    newResult<Boolean>().error(vedtakGraphQLError(resultat.melding, resultat.code, resultat.exception))
                        .data(false).build()
                }
            }
        }

    sealed interface VedtakResultat {
        data class Ok(val spleisBehandlingId: UUID) : VedtakResultat

        sealed class Feil(val melding: String, val code: Int, val exception: Exception?) : VedtakResultat {
            class IkkeÅpenOppgave : Feil(
                melding = "Oppgaven er ikke åpen.",
                code = 500,
                exception = IkkeÅpenOppgave("Oppgaven er ikke åpen.", 500),
            )

            class HarAktiveVarsler(oppgavereferanse: Long) : Feil(
                melding = "Har aktive varsler",
                code = 400,
                exception = ManglerVurderingAvVarsler(oppgavereferanse),
            )

            sealed class BeslutterFeil(melding: String, code: Int, exception: Exception?) :
                Feil(melding, code, exception) {
                class TrengerBeslutterRolle : BeslutterFeil(
                    melding = "Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver",
                    code = 401,
                    exception = null,
                )

                class KanIkkeBeslutteEgenOppgave : BeslutterFeil(
                    melding = "Kan ikke beslutte egne oppgaver.",
                    code = 401,
                    exception = null,
                )
            }
        }
    }

    private fun vedtakGraphQLError(
        melding: String,
        code: Int,
        exception: Exception?,
    ): GraphQLError =
        GraphqlErrorException.newErrorException().message(melding)
            .extensions(mapOf("code" to code, "exception" to exception)).build()
}

data class Avslag(
    val handling: Avslagshandling,
    val data: Avslagsdata?,
)

data class Avslagsdata(
    val type: ApiAvslagstype,
    val begrunnelse: String,
)

enum class Avslagshandling {
    OPPRETT,
    INVALIDER,
}
