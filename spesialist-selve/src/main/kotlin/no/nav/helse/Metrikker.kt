package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import io.prometheus.client.Summary
import java.time.temporal.ChronoUnit.MILLIS
import no.nav.helse.mediator.GodkjenningsbehovUtfall
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val automatiseringsteller = Counter.build("automatiseringer", "Teller antall automatiseringer")
    .register()

internal val automatiskAvvistÅrsakerTeller =
    Counter.build("automatisk_avvist_aarsaker", "Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker")
        .labelNames("aarsak")
        .register()

private val varselteller = Counter.build("aktivitet_totals", "Teller antall warnings opprettet i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

private val inaktiveVarslerteller = Counter.build("inaktive_warning_totals", "Teller antall warnings satt inaktive i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

private val registrerTidsbrukForHendelse = Summary.build("command_tidsbruk", "Måler hvor lang tid en command bruker på å kjøre i ms")
    .labelNames("command")
    .register()

private val godkjenningsbehovUtfall = Histogram.build("godkjenningsbehov_utfall", "Måler hvor raskt godkjenningsbehov behandles, fordelt på utfallet")
    .buckets(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0, 4500.0, 5000.0, 6000.0, 10_000.0, 30_000.0)
    .labelNames("utfall")
    .register()

private val registrerTidsbrukForBehov = Summary.build("behov_tidsbruk", "Måler hvor lang tid et behov tok å løse i ms")
    .labelNames("behov")
    .register()

internal fun tellVarsel(varselkode: String) = varselteller.labels("WARN", varselkode).inc()

internal fun tellInaktivtVarsel(varselkode: String) = inaktiveVarslerteller.labels("WARN", varselkode).inc()

internal fun registrerTidsbrukForHendelse(command: String, tidBruktMs: Int) = registrerTidsbrukForHendelse.labels(command).observe(tidBruktMs.toDouble())

internal fun registrerTidsbrukForGodkjenningsbehov(utfall: GodkjenningsbehovUtfall, tidBruktMs: Int) =
    godkjenningsbehovUtfall.labels(utfall.name).observe(tidBruktMs.toDouble())

internal class MetrikkRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
    val log: Logger = LoggerFactory.getLogger("MetrikkRiver")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandValue("@final", true)
                it.requireKey("@besvart", "@behov", "system_participating_services")
                it.interestedIn("@løsning.Godkjenning.godkjent")
                it.interestedIn("@løsning.Godkjenning.automatiskBehandling")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val besvart = packet["@besvart"].asLocalDateTime()
        val opprettet = packet["system_participating_services"][0].let { it["time"].asLocalDateTime() }
        val delay = MILLIS.between(opprettet, besvart)
        val behov = packet["@behov"].map(JsonNode::asText)
        val godkjent: Boolean? = packet["@løsning.Godkjenning.godkjent"].takeUnless{ it.isMissingOrNull() }?.asBoolean()
        val automatisk: Boolean? = packet["@løsning.Godkjenning.automatiskBehandling"].takeUnless{ it.isMissingOrNull() }?.asBoolean()

        val godkjenningslog = if (godkjent != null && automatisk != null) {
            " Løsning er ${if (automatisk) "automatisk" else "manuelt"} ${if (godkjent) "godkjent" else "avvist"}."
        } else {
            ""
        }

        log.info("Registrerer svartid for $behov som $delay ms.$godkjenningslog")
        registrerTidsbrukForBehov.labels(behov.first()).observe(delay.toDouble())
    }
}
