package no.nav.helse.mediator.meldinger

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

/**
 * Støttekode for å kunne sjekke Gosys-oppgaver for alle saker som ligger til manuell behandling.
 */
internal class MidlertidigRiver(rapidsConnection: RapidsConnection, private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    private val eventName = "send_gosys_oppgave_for_ALLE_personer_med_oppgave"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", eventName)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        logg.info("Behandler $eventName")
        val fødselsnumre = sessionOf(dataSource).use { session ->
            @Language("postgresql")
            val query = """
                 select p.fodselsnummer from oppgave o
                 join vedtak v on o.vedtak_ref = v.id
                 join person p on v.person_ref = p.id
                 where o.status = 'AvventerSaksbehandler'
            """.trimIndent()
            session.run(queryOf(query).map { it.long(1) }.asList).map { if (it < 10000000000) "0$it" else it.toString()}
        }
        logg.info("Sender gosys_oppgave_endret for ${fødselsnumre.size} personer")
        fødselsnumre.forEach { fnr ->
            context.publish(fnr, gosysOppgaveEndret(fnr))
        }
        logg.info("Har sendt gosys_oppgave_endret for ${fødselsnumre.size} personer")
    }

    @Language("json")
    private fun gosysOppgaveEndret(fnr: String) = """
        {
          "@event_name": "gosys_oppgave_endret",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "$fnr",
          "@opprettet": "${LocalDateTime.now()}"
        }
    """.trimIndent()

}
