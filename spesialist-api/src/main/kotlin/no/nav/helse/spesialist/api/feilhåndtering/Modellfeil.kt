package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.logg.loggInfo
import java.util.UUID

abstract class Modellfeil protected constructor() : RuntimeException() {
    protected abstract val eksternKontekst: Map<String, Any>
    protected abstract val feilkode: String
    abstract val httpkode: HttpStatusCode

    open fun logger() = Unit

    open fun tilFeilDto(): FeilDto = FeilDto(feilkode, eksternKontekst)

    override val message: String get() = feilkode
}

class OppgaveIkkeTildelt(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.FailedDependency
    override val feilkode: String = "oppgave_er_ikke_tildelt"

    override fun logger() {
        loggInfo(
            "Returnerer ${httpkode.value} for $feilkode for oppgave",
            "oppgaveId: $oppgaveId",
        )
    }
}

class OppgaveTildeltNoenAndre(
    val tildeling: TildelingApiDto,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("tildeling" to tildeling)
    override val httpkode = HttpStatusCode.Conflict
    override val feilkode: String = "oppgave_tildelt_noen_andre"

    override fun logger() {
        loggInfo(
            "Returnerer ${httpkode.value} for $feilkode for tildeling",
            "tildelingsinfo: $eksternKontekst",
        )
    }
}

class ManglerVurderingAvVarsler(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.BadRequest
    override val feilkode: String = "mangler_vurdering_av_varsler"

    override fun logger() {
        loggInfo(
            "Returnerer ${httpkode.value} for $feilkode for tildeling",
            "oppgaveId: $oppgaveId",
        )
    }
}

class FinnerIkkeLagtPåVent(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = mapOf("oppgaveId" to oppgaveId.toString())
    override val httpkode = HttpStatusCode.BadRequest
    override val feilkode: String = "finner_ikke_paa_vent"

    override fun logger() {
        loggInfo(
            "Finner ikke påvent-innslag for oppgave",
            "oppgaveId: $oppgaveId",
        )
    }
}

class IkkeTilgang(
    private val oid: UUID,
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val httpkode = HttpStatusCode.Forbidden
    override val feilkode: String = "ikke_tilgang_til_oppgave"

    override fun logger() {
        loggInfo(
            "Saksbehandler har ikke tilgang til å behandle oppgaven",
            "oppgaveId: $oppgaveId, saksbehandlerOid: $oid",
        )
    }
}

class OppgaveAlleredeSendtBeslutter(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_allerede_sendt_beslutter"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        loggInfo(
            "Oppgave er allerede sendt til beslutter for totrinnsvurdering",
            "oppgaveId: $oppgaveId",
        )
    }
}

class OppgaveAlleredeSendtIRetur(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_allerede_sendt_i_retur"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        loggInfo(
            "Oppgave er allerede sendt i retur av beslutter til opprinnelig saksbehandler",
            "oppgaveId: $oppgaveId",
        )
    }
}

class OppgaveKreverVurderingAvToSaksbehandlere(
    private val oppgaveId: Long,
) : Modellfeil() {
    override val eksternKontekst: Map<String, Any> = emptyMap()
    override val feilkode: String = "oppgave_krever_totrinnsvurdering"
    override val httpkode: HttpStatusCode = HttpStatusCode.Conflict

    override fun logger() {
        loggInfo(
            "Oppgave må behandles og besluttes av to forskjellige saksbehandlere",
            "oppgaveId: $oppgaveId",
        )
    }
}
