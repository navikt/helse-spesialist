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
}

enum class Loggnivå {
    Warning
}

sealed class Feil(val feilkode: String, val kategori: Feilkategori, val loggnivå: Loggnivå)
object OppgaveErAlleredeTildelt : Feil("oppgave_er_allerede_tildelt", Brukerfeil, Loggnivå.Warning)

sealed class Feilkategori(val httpStatus: HttpStatusCode)
object Brukerfeil : Feilkategori(HttpStatusCode.BadRequest)

suspend inline fun PipelineContext<*, ApplicationCall>.modellfeilForRest(lambda: () -> Unit) {
    try {
        lambda()
    } catch (f: ModellFeil) {
        when (f.feil.loggnivå) {
            Loggnivå.Warning -> logg(this).warn(
                "Returnerer {} for {}",
                keyValue("httpKode", f.httpKode().value),
                keyValue("feilkode", f.feilkode())
            )
        }
        call.respond(status = f.httpKode(), message = FeilDto(f.feilkode()))
    }
}

fun logg(that: Any): Logger =
    LoggerFactory.getLogger(that::class.java)

