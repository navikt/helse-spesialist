package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.HttpStatusCode
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

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

class OppgaveIkkeTildelt(private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.FailedDependency
    override val feilkode: String = "oppgave_er_ikke_tildelt"

    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode),
        )
    }
}

class OppgaveTildeltNoenAndre(val tildeling: TildelingApiDto) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("tildeling" to tildeling)
    override val httpkode = HttpStatusCode.Conflict
    override val feilkode: String = "oppgave_tildelt_noen_andre"

    override fun logger() {
        logg.info(
            "Returnerer {} for {}",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode),
        )
        sikkerLogg.info(
            "Returnerer {} for {}, tildelingsinfo=$eksternKontekst",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode),
        )
    }
}

class ManglerVurderingAvVarsler(private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.BadRequest
    override val feilkode: String = "mangler_vurdering_av_varsler"

    override fun logger() {
        logg.info(
            "Returnerer {} for {} for oppgaveId=$oppgaveId",
            keyValue("httpkode", "${httpkode.value}"),
            keyValue("feilkode", feilkode),
        )
    }
}

class IkkeTilgang(private val oid: UUID, private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val httpkode = HttpStatusCode.Forbidden
    override val feilkode: String = "ikke_tilgang_til_oppgave"

    override fun logger() {
        logg.info(
            "Saksbehandler har ikke tilgang til å behandle oppgaven, {}",
            keyValue("oppgaveId", oppgaveId),
        )
        sikkerLogg.info(
            "Saksbehandler {} har ikke tilgang til å behandle oppgaven, {}",
            keyValue("oid", oid),
            keyValue("oppgaveId", oppgaveId),
        )
    }
}

class OppgaveAlleredeSendtBeslutter(private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_allerede_sendt_beslutter"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        logg.info("Oppgave med {} er allerede sendt til beslutter for totrinnsvurdering", kv("oppgaveId", oppgaveId))
    }
}

class OppgaveAlleredeSendtIRetur(private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_allerede_sendt_i_retur"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        logg.info("Oppgave med {} er allerede sendt i retur av beslutter til opprinnelig saksbehandler", kv("oppgaveId", oppgaveId))
    }
}

class OppgaveKreverVurderingAvToSaksbehandlere(private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_krever_totrinnsvurdering"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        logg.info("Oppgave med {} må behandles og besluttes av to forskjellige saksbehandlere", kv("oppgaveId", oppgaveId))
    }
}

class IkkeÅpenOppgave(private val saksbehandlerIdent: String, private val oppgaveId: Long) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val httpkode = HttpStatusCode.Conflict
    override val feilkode: String = "ikke_aapen_saksbehandleroppgave"

    override fun logger() {
        logg.info(
            "Behandler ikke godkjenning/avslag for {}, den er enten behandlet eller invalidert",
            keyValue("oppgaveId", oppgaveId),
        )
        sikkerLogg.info(
            "Saksbehandler {} forsøkte å utbetale/avvise {} eller sende den til godkjenning, men den er behandlet eller invalidert",
            keyValue("saksbehandlerIdent", saksbehandlerIdent),
            keyValue("oppgaveId", oppgaveId),
        )
    }
}
