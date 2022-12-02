package no.nav.helse.migrering

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.migrering.Personavstemming.Vedtaksperiode.Generasjon.Companion.lagre
import no.nav.helse.migrering.Personavstemming.Vedtaksperiode.Utbetaling
import no.nav.helse.migrering.Personavstemming.Vedtaksperiode.Utbetaling.Companion.sortert
import no.nav.helse.migrering.Varsel.Companion.konsumer
import no.nav.helse.migrering.Varsel.Companion.lagre
import no.nav.helse.migrering.Varsel.Companion.sortert
import no.nav.helse.migrering.Varsel.Companion.varslerFor
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class Personavstemming {

    internal class River(
        rapidsConnection: RapidsConnection,
        private val sparsomDao: SparsomDao,
        private val spesialistDao: SpesialistDao,
    ) : PacketListener {

        private companion object {
            private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        }

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "person_avstemt")
                    it.requireKey("@id", "fødselsnummer")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireArray("arbeidsgivere") {
                        requireArray("vedtaksperioder") {
                            requireKey("tilstand")
                            require("opprettet", JsonNode::asLocalDateTime)
                            require("oppdatert", JsonNode::asLocalDateTime)
                            require("id") { jsonNode ->
                                UUID.fromString(jsonNode.asText())
                            }
                            requireArray("utbetalinger")
                        }
                        requireArray("utbetalinger") {
                            require("opprettet", JsonNode::asLocalDateTime)
                            require("oppdatert", JsonNode::asLocalDateTime)
                            requireKey("type", "status")
                            require("id") { jsonNode ->
                                UUID.fromString(jsonNode.asText())
                            }
                        }
                    }
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val arbeidsgivereJson = packet["arbeidsgivere"]
            if (arbeidsgivereJson.isEmpty) {
                sikkerlogg.info(
                    "Person med {} har ingen arbeidsgivere, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }
            val vedtaksperioderJson = arbeidsgivereJson.flatMap { it["vedtaksperioder"] }
            if (vedtaksperioderJson.isEmpty()) {
                sikkerlogg.info(
                    "Person med {} har ingen aktive vedtaksperioder, forsøker ikke å opprette generasjon.",
                    keyValue("fødselsnummer", fødselsnummer)
                )
                return
            }
            val varslerForPerson = sparsomDao.finnVarslerFor(fødselsnummer)
            val utbetalingerJson = arbeidsgivereJson.flatMap { it["utbetalinger"] }
            val vedtaksperioder = vedtaksperioderJson.map { periodeNode ->
                val vedtaksperiodeUtbetalinger = utbetalingerJson.filter { utbetalingNode ->
                    utbetalingNode["id"].asText() in periodeNode["utbetalinger"].map { it.asText() }
                }
                Vedtaksperiode(
                    id = UUID.fromString(periodeNode["id"].asText()),
                    opprettet = periodeNode["opprettet"].asLocalDateTime(),
                    oppdatert = periodeNode["oppdatert"].asLocalDateTime(),
                    tilstand = periodeNode["tilstand"].asText(),
                    personVarsler = varslerForPerson,
                    utbetalinger = vedtaksperiodeUtbetalinger.map { utbetalingNode ->
                        Utbetaling(
                            UUID.fromString(utbetalingNode["id"].asText()),
                            utbetalingNode["opprettet"].asLocalDateTime(),
                            utbetalingNode["oppdatert"].asLocalDateTime(),
                            utbetalingNode["status"].asText(),
                        )
                    }
                )
            }
            vedtaksperioder
                .map { it.generasjoner().sortedBy { it.opprettet } }
                .forEach { it.lagre(spesialistDao, hendelseId) }
        }
    }

    internal class Vedtaksperiode(
        private val id: UUID,
        private val opprettet: LocalDateTime,
        private val oppdatert: LocalDateTime,
        private val utbetalinger: List<Utbetaling>,
        private val tilstand: String,
        personVarsler: List<Varsel>,
    ) {

        private val varsler = personVarsler.varslerFor(id).sortert().toMutableList()

        internal fun generasjoner(): List<Generasjon> {
            var sistOpprettet: LocalDateTime? = opprettet
            if (utbetalinger.isEmpty()) {
                if (tilstand == "AVSLUTTET_UTEN_UTBETALING") return listOf(auu(oppdatert, varsler))
                return listOf(åpen(varsler))
            }

            val generasjoner = mutableListOf<Generasjon>()

            utbetalinger
                .sortert()
                .also { sorterteUtbetalinger ->
                    sorterteUtbetalinger.forEach {
                        val generasjon = it.lagGenerasjon(id, sistOpprettet, varsler)
                        sistOpprettet = null
                        if (sorterteUtbetalinger.indexOf(it) == sorterteUtbetalinger.lastIndex) {
                            varsler.forEach { varsel ->
                                generasjon.nyttVarsel(varsel)
                            }
                        }
                        generasjoner.add(generasjon)
                    }
                }

            return generasjoner
        }

        private fun åpen(varsler: List<Varsel>): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                vedtaksperiodeId = id,
                utbetalingId = null,
                opprettet = opprettet,
                låstTidspunkt = null,
                varsler = varsler,
            )
        }

        private fun auu(låstTidspunkt: LocalDateTime, varsler: List<Varsel>): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                vedtaksperiodeId = id,
                utbetalingId = null,
                opprettet = opprettet,
                låstTidspunkt = låstTidspunkt,
                varsler = varsler,
            )
        }

        internal class Generasjon(
            val id: UUID,
            val vedtaksperiodeId: UUID,
            val utbetalingId: UUID?,
            val opprettet: LocalDateTime,
            val låstTidspunkt: LocalDateTime?,
            varsler: List<Varsel>,
        ) {

            private val varsler: MutableList<Varsel> = varsler.toMutableList()

            internal companion object {
                private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
                fun List<Generasjon>.lagre(spesialistDao: SpesialistDao, hendelseId: UUID) {
                    forEach { it.lagre(spesialistDao, hendelseId) }
                }
            }

            internal fun nyttVarsel(varsel: Varsel) {
                varsler.add(varsel)
            }

            internal fun lagre(spesialistDao: SpesialistDao, hendelseId: UUID) {
                val insertGenerasjonOk = spesialistDao.lagreGenerasjon(
                    id,
                    vedtaksperiodeId,
                    utbetalingId,
                    opprettet,
                    hendelseId,
                    låstTidspunkt
                )

                if (!insertGenerasjonOk) {
                    sikkerlogg.warn(
                        "Kunne ikke inserte generasjon for {}, {}, den eksisterer fra før av.",
                        keyValue("vedtaksperiodeId", vedtaksperiodeId),
                        keyValue("utbetalingId", utbetalingId)
                    )
                    return
                }

                val insertVarselOk = varsler.lagre(id, spesialistDao)
            }
        }

        internal class Utbetaling(
            private val id: UUID,
            private val opprettet: LocalDateTime,
            private val oppdatert: LocalDateTime,
            private val status: String,
        ) {
            internal companion object {
                internal fun List<Utbetaling>.sortert(): List<Utbetaling> {
                    return sortedBy { it.opprettet }
                }
            }

            internal fun lagGenerasjon(
                vedtaksperiodeId: UUID,
                sistOpprettet: LocalDateTime?,
                varsler: MutableList<Varsel>,
            ): Generasjon {
                val generasjonVarsler = varsler.konsumer(oppdatert)
                val låst = status in listOf("UTBETALT", "GODKJENT_UTEN_UTBETALING")
                return Generasjon(
                    UUID.randomUUID(),
                    vedtaksperiodeId,
                    id,
                    sistOpprettet ?: opprettet,
                    if (låst) oppdatert else null,
                    generasjonVarsler
                )
            }
        }
    }
}