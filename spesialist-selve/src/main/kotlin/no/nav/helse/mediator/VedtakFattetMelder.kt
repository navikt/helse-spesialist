package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.kafka.message_builders.somJsonMessage
import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.vedtak.Sykepengevedtak
import org.slf4j.LoggerFactory

internal class VedtakFattetMelder(
    private val messageContext: MessageContext,
) : PersonObserver {
    private val sykepengevedtak = mutableListOf<Sykepengevedtak>()

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(VedtakFattetMelder::class.java)
    }

    internal fun publiserUtgåendeMeldinger() {
        if (sykepengevedtak.isEmpty()) return
        check(sykepengevedtak.size == 1) { "Forventer å publisere kun ett vedtak" }
        val sykepengevedtak = sykepengevedtak.single()
        val json = sykepengevedtak.somJsonMessage()
        logg.info("Publiserer vedtak_fattet for {}", kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId))
        sikkerLogg.info(
            "Publiserer vedtak_fattet for {}, {}, {}",
            kv("fødselsnummer", sykepengevedtak.fødselsnummer),
            kv("organisasjonsnummer", sykepengevedtak.organisasjonsnummer),
            kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId),
        )
        messageContext.publish(json)
        this.sykepengevedtak.clear()
    }

    override fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {
        this.sykepengevedtak.add(sykepengevedtak)
    }
}
