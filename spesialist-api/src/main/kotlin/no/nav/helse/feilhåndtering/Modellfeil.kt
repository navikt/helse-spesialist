package no.nav.helse.feilhåndtering

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.tildeling.TildelingApiDto
import org.slf4j.LoggerFactory

abstract class Modellfeil protected constructor(protected val feilkode: String) : RuntimeException(feilkode) {
    protected companion object {
        val logg = LoggerFactory.getLogger(this::class.java)
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    protected abstract val eksternKontekst: Map<String, Any>
    abstract val httpkode: HttpStatusCode
    open fun logger() = Unit
    open fun toJson(): FeilDto = FeilDto(feilkode, eksternKontekst)
}

class OppgaveAlleredeTildelt(tildeling: TildelingApiDto) : Modellfeil("oppgave_er_allerede_tildelt") {
    override val eksternKontekst: Map<String, Any> = mapOf(
        "tildeltTil" to tildeling.navn,
        "tildeling" to tildeling
    )

    override val httpkode = HttpStatusCode.Conflict

    override fun logger() {
        logg.info(
            "Returnerer {} for {}",
            keyValue("httpkode", httpkode),
            keyValue("feilkode", feilkode)
        )
        sikkerLogg.info(
            "Returnerer {} for {}, tildelingsinfo=$eksternKontekst",
            keyValue("httpkode", httpkode.value),
            keyValue("feilkode", feilkode)
        )
    }
}

class OppgaveIkkeTildelt(private val oppgaveId: Long): Modellfeil("oppgave_er_ikke_tildelt") {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.FailedDependency
    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", httpkode.value),
            keyValue("feilkode", feilkode)
        )
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.modellfeilForRest(lambda: () -> Unit) {
    try {
        lambda()
    } catch (feil: Modellfeil) {
        feil.logger()
        call.respond(status = feil.httpkode, message = feil.toJson())
    }
}
