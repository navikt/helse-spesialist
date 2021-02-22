package no.nav.helse.modell.feilhåndtering

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ModellFeil(val feil: Feil) : RuntimeException(feil.feilkode) {
    fun feilkode() = this.feil.feilkode

    fun httpKode() = this.feil.kategori.httpStatus

    fun loggNivå() = this.feil.kategori.loggnivå
}

enum class Loggnivå {
    Warning, Info
}

sealed class Feil(val feilkode: String, val kategori: Feilkategori, val eksternKontekst: Map<String, String> = mapOf())
data class OppgaveErAlleredeTildelt(val tildeltTil: String) :
    Feil("oppgave_er_allerede_tildelt", Feilkategori(HttpStatusCode.Conflict, Loggnivå.Info), mapOf("tildeltTil" to tildeltTil))

data class OppgaveErIkkeTildelt(val oppgaveId: Long) :
    Feil("oppgave_er_ikke_tildelt", Feilkategori(HttpStatusCode.FailedDependency, Loggnivå.Info), mapOf("oppgaveId" to oppgaveId.toString()))

class Feilkategori(val httpStatus: HttpStatusCode, val loggnivå: Loggnivå)

suspend inline fun PipelineContext<*, ApplicationCall>.modellfeilForRest(lambda: () -> Unit) {
    try {
        lambda()
    } catch (feil: ModellFeil) {
        val f: (String, Any, Any) -> Unit = when (feil.loggNivå()) {
            Loggnivå.Warning -> logg(this)::warn
            Loggnivå.Info -> logg(this)::info
        }

        f("Returnerer {} for {}",
            keyValue("httpKode", feil.httpKode().value),
            keyValue("feilkode", feil.feilkode()))

        call.respond(status = feil.httpKode(), message = FeilDto(feil.feilkode(), feil.feil.eksternKontekst))
    }
}

fun logg(that: Any): Logger =
    LoggerFactory.getLogger(that::class.java)

