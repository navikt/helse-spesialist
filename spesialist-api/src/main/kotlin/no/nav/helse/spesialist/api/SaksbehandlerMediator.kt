package no.nav.helse.spesialist.api

import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinje.Overstyringdag
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerHendelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerObserver
import org.slf4j.LoggerFactory

class SaksbehandlerMediator(
    dataSource: DataSource,
    private val rapidsConnection: RapidsConnection
): SaksbehandlerObserver {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun <T: SaksbehandlerHendelse> håndter(hendelse: T, saksbehandlerDto: SaksbehandlerDto) {
        val saksbehandlerOid = saksbehandlerDto.oid
        if (saksbehandlerOid != hendelse.saksbehandlerOid()) throw IllegalStateException()
        val saksbehandler = saksbehandlerDao.finnSaksbehandlerFor(saksbehandlerOid) ?: saksbehandlerDao.opprettFra(saksbehandlerDto)
        saksbehandler.register(this)
        sikkerlogg.info("$saksbehandler behandler nå ${hendelse::class.simpleName}")
        hendelse.håndter(saksbehandler)
        sikkerlogg.info("$saksbehandler har ferdigbehandlet ${hendelse::class.simpleName}")
        tellHendelse(hendelse)
    }

    override fun annulleringEvent(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        saksbehandler: Map<String, Any>,
        fagsystemId: String,
        begrunnelser: List<String>,
        kommentar: String?
    ) {
        val verdier = mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "aktørId" to aktørId,
            "saksbehandler" to saksbehandler,
            "fagsystemId" to fagsystemId,
            "begrunnelser" to begrunnelser,
        ).apply {
            compute("kommentar") { _, _ -> kommentar }
        }
        nyMelding(aktørId, fødselsnummer, organisasjonsnummer, "annullering", verdier)
    }

    override fun overstyrTidslinjeEvent(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        saksbehandler: Map<String, Any>,
        begrunnelse: String,
        dager: List<Overstyringdag>
    ) {
        nyMelding(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            "saksbehandler_overstyrer_tidslinje",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "dager" to dager.map(Overstyringdag::toJson),
                "begrunnelse" to begrunnelse,
                "saksbehandler" to saksbehandler,
            )
        )
    }

    private fun nyMelding(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, eventName: String, verdier: Map<String, Any>) {
        JsonMessage.newMessage(eventName, verdier).toJson().also {
            rapidsConnection.publish(fødselsnummer, it)
            sikkerlogg.info(
                "sender $eventName for {}, {}, {}\n\t$it",
                keyValue("aktørId", aktørId),
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }
    }
}