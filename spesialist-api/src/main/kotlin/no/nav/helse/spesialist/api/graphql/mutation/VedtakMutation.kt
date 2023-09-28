package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.erDev
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgangTilRiskQa
import no.nav.helse.spesialist.api.feilhåndtering.IkkeÅpenOppgave
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory

class VedtakMutation(
    private val oppgavehåndterer: Oppgavehåndterer,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val godkjenninghåndterer: Godkjenninghåndterer,
) : Mutation {

    private companion object {
        private val logg = LoggerFactory.getLogger(VedtakMutation::class.java)

    }

    @Suppress("unused")
    suspend fun innvilgVedtak(
        oppgavereferanse: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(ContextValues.TILGANGER.key)
        logg.info("Fatter vedtak for oppgave $oppgavereferanse")

        when (val vedtak = kanFatteVedtak(oppgavereferanse.toLong(), saksbehandler.value, tilganger)) {
            is VedtakResultat.Success -> {
                val behandlingId = UUID.randomUUID()
                val godkjenning = GodkjenningDto(oppgavereferanse.toLong(), true, saksbehandler.value.ident, null, null, null)

                saksbehandlerhåndterer.håndter(godkjenning, behandlingId, saksbehandler.value)
                godkjenninghåndterer.håndter(godkjenning, saksbehandler.value.epost, saksbehandler.value.oid, behandlingId)

                newResult<Boolean>().data(true).build()
            }

            is VedtakResultat.Error -> {
                logg.warn("Kunne ikke innvilge vedtak: ${vedtak.error.melding}")
                newResult<Boolean>().error(
                    vedtakGraphQLError(
                        vedtak.error.melding,
                        vedtak.error.code,
                        vedtak.error.exception
                    )
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
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(ContextValues.TILGANGER.key)
        logg.info("Sender oppgave $oppgavereferanse til Infotrygd")

        when (val vedtak = kanFatteVedtak(oppgavereferanse.toLong(), saksbehandler.value, tilganger)) {
            is VedtakResultat.Success -> {
                val behandlingId = UUID.randomUUID()
                val godkjenning = GodkjenningDto(
                    oppgavereferanse.toLong(),
                    false,
                    saksbehandler.value.ident,
                    arsak,
                    begrunnelser,
                    kommentar
                )

                saksbehandlerhåndterer.håndter(godkjenning, behandlingId, saksbehandler.value)
                godkjenninghåndterer.håndter(godkjenning, saksbehandler.value.epost, saksbehandler.value.oid, behandlingId)

                newResult<Boolean>().data(true).build()
            }

            is VedtakResultat.Error -> {
                logg.warn("Kunne ikke sende oppgave til Infotrygd: ${vedtak.error.melding}")
                newResult<Boolean>().error(
                    vedtakGraphQLError(
                        vedtak.error.melding,
                        vedtak.error.code,
                        vedtak.error.exception
                    )
                ).build()
            }
        }
    }

    private suspend fun kanFatteVedtak(
        oppgavereferanse: Long,
        saksbehandler: SaksbehandlerFraApi,
        tilganger: SaksbehandlerTilganger,
    ): VedtakResultat {
        val erÅpenOppgave = oppgavehåndterer.venterPåSaksbehandler(oppgavereferanse)
        if (!erÅpenOppgave) {
            return VedtakResultat.Error(
                VedtakError.IkkeÅpenOppgave(
                    "Oppgaven er ikke åpen.",
                    500,
                    IkkeÅpenOppgave("Oppgaven er ikke åpen.", 500)
                )
            )
        }

        val erRiskOppgave = withContext(Dispatchers.IO) { oppgavehåndterer.erRiskoppgave(oppgavereferanse) }
        if (erRiskOppgave && !tilganger.harTilgangTilRiskOppgaver()) {
            return VedtakResultat.Error(
                VedtakError.IkkeTilgangTilRiskQa(
                    "Saksbehandler har ikke tilgang til risk-qa oppgaver.",
                    500,
                    IkkeTilgangTilRiskQa("Saksbehandler har ikke tilgang til risk-qa oppgaver.", 500)
                )
            )
        }

        if (totrinnsvurderinghåndterer.erBeslutterOppgave(oppgavereferanse)) {
            if (!tilganger.harTilgangTilBeslutterOppgaver() && !erDev()) {
                return VedtakResultat.Error(
                    VedtakError.TrengerBeslutterRolle(
                        "Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver",
                        401
                    )
                )
            }
            if (totrinnsvurderinghåndterer.erEgenOppgave(oppgavereferanse, saksbehandler.oid)) {
                return VedtakResultat.Error(VedtakError.EgenOppgave("Kan ikke beslutte egne oppgaver.", 401))
            }

            totrinnsvurderinghåndterer.settBeslutter(oppgavereferanse, saksbehandler.oid)
        }

        return VedtakResultat.Success
    }

    sealed class VedtakResultat {
        data object Success : VedtakResultat()
        data class Error(val error: VedtakError) : VedtakResultat()
    }

    sealed class VedtakError(val melding: String, val code: Int, val exception: Exception?) {
        class IkkeÅpenOppgave(melding: String, code: Int, exception: Exception) :
            VedtakError(melding, code, exception)

        class IkkeTilgangTilRiskQa(melding: String, code: Int, exception: Exception) :
            VedtakError(melding, code, exception)

        class TrengerBeslutterRolle(melding: String, code: Int) :
            VedtakError(melding, code, null)

        class EgenOppgave(melding: String, code: Int) :
            VedtakError(melding, code, null)
    }

    private fun vedtakGraphQLError(melding: String, code: Int, exception: Exception?): GraphQLError =
        GraphqlErrorException.newErrorException().message(melding)
            .extensions(mapOf("code" to code, "exception" to exception)).build()
}