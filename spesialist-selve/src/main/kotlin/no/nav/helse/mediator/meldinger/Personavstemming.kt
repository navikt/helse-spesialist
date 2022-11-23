package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Personavstemming.Vedtaksperiode.Generasjon.Companion.lagre
import no.nav.helse.mediator.meldinger.Personavstemming.Vedtaksperiode.Utbetaling
import no.nav.helse.mediator.meldinger.Personavstemming.Vedtaksperiode.Utbetaling.Companion.sortert
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.intellij.lang.annotations.Language

internal class Personavstemming {

    internal class River(
        rapidsConnection: RapidsConnection,
        private val dataSource: DataSource,
    ) : PacketListener {

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
            val vedtaksperioderJson = packet["arbeidsgivere"].flatMap { it["vedtaksperioder"] }
            val utbetalingerJson = packet["arbeidsgivere"].flatMap { it["utbetalinger"] }
            val vedtaksperioder = vedtaksperioderJson.map { periodeNode ->
                val vedtaksperiodeUtbetalinger = utbetalingerJson.filter { utbetalingNode ->
                    utbetalingNode["id"].asText() in periodeNode["utbetalinger"].map { it.asText() }
                }
                Vedtaksperiode(
                    id = UUID.fromString(periodeNode["id"].asText()),
                    opprettet = periodeNode["opprettet"].asLocalDateTime(),
                    oppdatert = periodeNode["oppdatert"].asLocalDateTime(),
                    tilstand = periodeNode["tilstand"].asText(),
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
                .forEach { it.lagre(dataSource, hendelseId) }
        }
    }

    internal class Vedtaksperiode(
        private val id: UUID,
        private val opprettet: LocalDateTime,
        private val oppdatert: LocalDateTime,
        private val utbetalinger: List<Utbetaling>,
        private val tilstand: String,
    ) {

        internal fun generasjoner(): List<Generasjon> {
            var sistOpprettet: LocalDateTime? = opprettet
            if (utbetalinger.isEmpty()) {
                if (tilstand == "AVSLUTTET_UTEN_UTBETALING") return listOf(auu(oppdatert))
                return listOf(åpen())
            }

            val generasjoner = mutableListOf<Generasjon>()

            utbetalinger
                .sortert()
                .forEach {
                val generasjon = it.lagGenerasjon(id, sistOpprettet)
                sistOpprettet = null
                generasjoner.add(generasjon)
            }

            return generasjoner
        }

        private fun åpen(): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                vedtaksperiodeId = id,
                utbetalingId = null,
                opprettet = opprettet,
                låstTidspunkt = null,
                låst = false
            )
        }

        private fun auu(låstTidspunkt: LocalDateTime): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                vedtaksperiodeId = id,
                utbetalingId = null,
                opprettet = opprettet,
                låstTidspunkt = låstTidspunkt,
                låst = true
            )
        }

        internal class Generasjon(
            val id: UUID,
            val vedtaksperiodeId: UUID,
            val utbetalingId: UUID?,
            val opprettet: LocalDateTime,
            val låstTidspunkt: LocalDateTime?,
            val låst: Boolean,
        ) {
            internal companion object {
                fun List<Generasjon>.lagre(dataSource: DataSource, hendelseId: UUID) {
                    forEach { it.lagre(dataSource, hendelseId) }
                }
            }

            internal fun lagre(dataSource: DataSource, hendelseId: UUID) {
                sessionOf(dataSource).use { session ->
                    @Language("PostgreSQL")
                    val query = """
                        INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """
                    session.run(
                        queryOf(
                            query,
                            id,
                            vedtaksperiodeId,
                            utbetalingId,
                            opprettet,
                            hendelseId,
                            låstTidspunkt,
                            if (låst) hendelseId else null,
                            låst
                        ).asUpdate
                    )
                }
            }
        }

        internal class Utbetaling(
            private val id: UUID,
            private val opprettet: LocalDateTime,
            private val oppdatert: LocalDateTime,
            private val status: String,
        ) {
            internal companion object {
                fun List<Utbetaling>.sortert(): List<Utbetaling> {
                    return sortedBy { it.opprettet }
                }
            }

            internal fun lagGenerasjon(vedtaksperiodeId: UUID, sistOpprettet: LocalDateTime?): Generasjon {
                val låst = status in listOf("UTBETALT", "GODKJENT_UTEN_UTBETALING")
                return Generasjon(
                    UUID.randomUUID(),
                    vedtaksperiodeId,
                    id,
                    sistOpprettet ?: opprettet,
                    if (låst) oppdatert else null,
                    låst,
                )
            }
        }
    }
}