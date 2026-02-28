package no.nav.helse.spesialist.api

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal object AuditLogger {
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    private val teller: Counter =
        Counter
            .builder("auditlog_total")
            .description("Teller antall auditlogginnslag")
            .register(Metrics.globalRegistry)

    fun loggOk(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ) {
        logg(Level.INFO, saksbehandler, identitetsnummer.value, "")
    }

    fun loggManglendeTilgang(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ) {
        logg(Level.WARN, saksbehandler, identitetsnummer.value, " flexString1=Deny")
    }

    private fun logg(
        level: Level,
        saksbehandler: Saksbehandler,
        duid: String,
        suffix: String,
    ) {
        teller.increment()
        val message =
            "end=${System.currentTimeMillis()}" +
                " suid=${saksbehandler.ident.value}" +
                " duid=$duid" +
                suffix
        auditLog.atLevel(level).log(message)
        teamLogs.debug("audit-logget: $level - $message")
    }
}
