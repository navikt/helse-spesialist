package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.feilhåndtering.IkkeÅpenOppgave
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakMutation(
    private val oppgavehåndterer: Oppgavehåndterer,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val godkjenninghåndterer: Godkjenninghåndterer,
) : Mutation {
    private companion object {
        private val env = Environment()
        private val logg = LoggerFactory.getLogger(VedtakMutation::class.java)
    }

    @Suppress("unused")
    suspend fun innvilgVedtak(
        oppgavereferanse: String,
        env: DataFetchingEnvironment,
        avslag: Avslag?,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER)
            logg.info("Fatter vedtak for oppgave $oppgavereferanse")

            when (val vedtak = kanFatteVedtak(oppgavereferanse.toLong(), saksbehandler, tilganger)) {
                is VedtakResultat.Success -> {
                    val behandlingId = vedtak.spleisBehandlingId
                    val godkjenning = GodkjenningDto(oppgavereferanse.toLong(), true, saksbehandler.ident, null, null, null, avslag)

                    saksbehandlerhåndterer.håndter(godkjenning, behandlingId, saksbehandler)
                    godkjenninghåndterer.håndter(godkjenning, saksbehandler.epost, saksbehandler.oid)

                    newResult<Boolean>().data(true).build()
                }

                is VedtakResultat.Error -> {
                    logg.warn("Kunne ikke innvilge vedtak: ${vedtak.error.melding}")
                    newResult<Boolean>().error(
                        vedtakGraphQLError(
                            vedtak.error.melding,
                            vedtak.error.code,
                            vedtak.error.exception,
                        ),
                    ).build()
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
            val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER)
            logg.info("Sender oppgave $oppgavereferanse til Infotrygd")

            when (val vedtak = kanFatteVedtak(oppgavereferanse.toLong(), saksbehandler, tilganger)) {
                is VedtakResultat.Success -> {
                    val behandlingId = vedtak.spleisBehandlingId
                    val godkjenning =
                        GodkjenningDto(
                            oppgavereferanse.toLong(),
                            false,
                            saksbehandler.ident,
                            arsak,
                            begrunnelser,
                            kommentar,
                        )

                    saksbehandlerhåndterer.håndter(godkjenning, behandlingId, saksbehandler)
                    godkjenninghåndterer.håndter(godkjenning, saksbehandler.epost, saksbehandler.oid)

                    newResult<Boolean>().data(true).build()
                }

                is VedtakResultat.Error -> {
                    logg.warn("Kunne ikke sende oppgave til Infotrygd: ${vedtak.error.melding}")
                    newResult<Boolean>().error(
                        vedtakGraphQLError(
                            vedtak.error.melding,
                            vedtak.error.code,
                            vedtak.error.exception,
                        ),
                    ).build()
                }
            }
        }

    private fun kanFatteVedtak(
        oppgavereferanse: Long,
        saksbehandler: SaksbehandlerFraApi,
        tilganger: SaksbehandlerTilganger,
    ): VedtakResultat {
        val erÅpenOppgave = oppgavehåndterer.venterPåSaksbehandler(oppgavereferanse)
        val spleisBehandlingId = oppgavehåndterer.spleisBehandlingId(oppgavereferanse)
        if (!erÅpenOppgave) {
            return VedtakResultat.Error(
                VedtakError.IkkeÅpenOppgave(
                    "Oppgaven er ikke åpen.",
                    500,
                    IkkeÅpenOppgave("Oppgaven er ikke åpen.", 500),
                ),
            )
        }

        if (totrinnsvurderinghåndterer.erBeslutterOppgave(oppgavereferanse)) {
            if (!tilganger.harTilgangTilBeslutterOppgaver() && !env.erDev) {
                return VedtakResultat.Error(
                    VedtakError.TrengerBeslutterRolle(
                        "Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver",
                        401,
                    ),
                )
            }
            if (totrinnsvurderinghåndterer.erEgenOppgave(oppgavereferanse, saksbehandler.oid) && !env.erDev) {
                return VedtakResultat.Error(VedtakError.EgenOppgave("Kan ikke beslutte egne oppgaver.", 401))
            }

            totrinnsvurderinghåndterer.settBeslutter(oppgavereferanse, saksbehandler.oid)
        }

        return VedtakResultat.Success(spleisBehandlingId = spleisBehandlingId)
    }

    sealed class VedtakResultat {
        data class Success(val spleisBehandlingId: UUID) : VedtakResultat()

        data class Error(val error: VedtakError) : VedtakResultat()
    }

    sealed class VedtakError(val melding: String, val code: Int, val exception: Exception?) {
        class IkkeÅpenOppgave(melding: String, code: Int, exception: Exception) :
            VedtakError(melding, code, exception)

        class TrengerBeslutterRolle(melding: String, code: Int) :
            VedtakError(melding, code, null)

        class EgenOppgave(melding: String, code: Int) :
            VedtakError(melding, code, null)
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
    val type: Avslagstype,
    val begrunnelse: String,
)

enum class Avslagstype {
    AVSLAG,
    DELVIS_AVSLAG,
}

enum class Avslagshandling {
    OPPRETT,
    INVALIDER,
}
