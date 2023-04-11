package no.nav.helse.mediator.api

import io.ktor.server.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.gruppemedlemskap

class ApiTilgangskontroll(private val tilgangsgrupper: Tilgangsgrupper, private val gruppemedlemskap: List<UUID>) {

    val harTilgangTilRisksaker by lazy { gruppemedlemskap.contains(tilgangsgrupper.riskQaGruppeId) }
    val harTilgangTilBeslutterOppgaver by lazy { gruppemedlemskap.contains(tilgangsgrupper.beslutterGruppeId) }

    companion object {
        internal fun PipelineContext<Unit, ApplicationCall>.tilganger(tilgangsgrupper: Tilgangsgrupper) =
            ApiTilgangskontroll(tilgangsgrupper, gruppemedlemskap())
    }
}
