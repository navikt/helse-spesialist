package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.LoggerFactory

abstract class Modellfeil protected constructor() : RuntimeException() {
    protected companion object {
        val logg = LoggerFactory.getLogger(this::class.java)
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    protected abstract val eksternKontekst: Map<String, Any>
    protected abstract val melding: String
    abstract val httpkode: HttpStatusCode
    open fun logger() = Unit
    open fun tilFeilDto(): FeilDto = FeilDto(melding, eksternKontekst)

    override val message: String get() = melding
}

class OppgaveAlleredeTildelt(tildeling: TildelingApiDto) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf(
        "tildeling" to tildeling
    )

    override val httpkode = HttpStatusCode.Conflict
    override val melding: String = "oppgave_er_allerede_tildelt"

    override fun logger() {
        logg.info(
            "Returnerer {} for {}",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("melding", melding)
        )
        sikkerLogg.info(
            "Returnerer {} for {}, tildelingsinfo=$eksternKontekst",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("melding", melding)
        )
    }
}

class OppgaveIkkeTildelt(private val oppgaveId: Long): Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.FailedDependency
    override val melding: String = "oppgave_er_ikke_tildelt"
    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("melding", melding)
        )
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.modellfeilForRest(lambda: () -> Unit) {
    try {
        lambda()
    } catch (feil: Modellfeil) {
        feil.logger()
        call.respond(status = feil.httpkode, message = feil.tilFeilDto())
    }
}
