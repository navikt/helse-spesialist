package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.LoggerFactory

class TotrinnsvurderingMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
) : Mutation {

    companion object {
        private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun sendTilGodkjenning(
        oppgavereferanse: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val behandlendeSaksbehandler: SaksbehandlerFraApi =
            env.graphQlContext.get<Lazy<SaksbehandlerFraApi>?>(ContextValues.SAKSBEHANDLER.key).value

        saksbehandlerhåndterer.håndterTotrinnsvurdering(oppgavereferanse.toLong())
        oppgavehåndterer.sendTilBeslutter(oppgavereferanse.toLong(), behandlendeSaksbehandler)

        sikkerlogg.info(
            "Oppgave med {} sendes til godkjenning av saksbehandler med {}",
            StructuredArguments.kv("oppgaveId", oppgavereferanse),
            StructuredArguments.kv("oid", behandlendeSaksbehandler.oid),
        )

        totrinnsvurderinghåndterer.lagrePeriodehistorikk(
            oppgavereferanse.toLong(),
            behandlendeSaksbehandler.oid,
            PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
        )

        log.info("OppgaveId $oppgavereferanse sendt til godkjenning")

        DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    @Suppress("unused")
    suspend fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val besluttendeSaksbehandler: SaksbehandlerFraApi =
            env.graphQlContext.get<Lazy<SaksbehandlerFraApi>?>(ContextValues.SAKSBEHANDLER.key).value

        sikkerlogg.info(
            "Oppgave med {} sendes i retur av beslutter med {}",
            StructuredArguments.kv("oppgaveId", oppgavereferanse),
            StructuredArguments.kv("oid", besluttendeSaksbehandler.oid),
        )

        oppgavehåndterer.sendIRetur(oppgavereferanse.toLong(), besluttendeSaksbehandler)

        totrinnsvurderinghåndterer.lagrePeriodehistorikk(
            oppgaveId = oppgavereferanse.toLong(),
            saksbehandleroid = besluttendeSaksbehandler.oid,
            type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            notat = notatTekst to NotatType.Retur
        )

        log.info("OppgaveId $oppgavereferanse sendt i retur")

        DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}
