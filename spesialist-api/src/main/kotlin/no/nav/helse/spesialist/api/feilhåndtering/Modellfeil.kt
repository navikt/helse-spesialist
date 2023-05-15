package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.HttpStatusCode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Modellfeil protected constructor() : RuntimeException() {
    protected companion object {
        val logg: Logger = LoggerFactory.getLogger(this::class.java)
        val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    protected abstract val eksternKontekst: Map<String, Any>
    protected abstract val feilkode: String
    abstract val httpkode: HttpStatusCode

    open fun logger() = Unit
    open fun tilFeilDto(): FeilDto = FeilDto(feilkode, eksternKontekst)
    override val message: String get() = feilkode
}

class OppgaveAlleredeTildelt(tildeling: TildelingApiDto) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf(
        "tildeling" to tildeling
    )
    override val httpkode = HttpStatusCode.Conflict
    override val feilkode: String = "oppgave_er_allerede_tildelt"
    override fun logger() {
        logg.info(
            "Returnerer {} for {}",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode)
        )
        sikkerLogg.info(
            "Returnerer {} for {}, tildelingsinfo=$eksternKontekst",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode)
        )
    }
}

class OppgaveIkkeTildelt(private val oppgaveId: Long): Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.FailedDependency
    override val feilkode: String = "oppgave_er_ikke_tildelt"
    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode)
        )
    }
}

class ManglerVurderingAvVarsler(private val oppgaveId: Long): Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.BadRequest
    override val feilkode: String = "mangler_vurdering_av_varsler"
    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode)
        )
    }
}
