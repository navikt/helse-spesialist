package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.micrometer.core.instrument.Metrics
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SpleisBehandlingskoblingDao.SpesialistBehandling
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import java.time.LocalDateTime
import java.util.UUID

// Midlertidig: engangsmigrering som gir varsler spleis_behandling_id. Slettes når migreringen er kjørt.
class SpleisBehandlingskoblingRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "spleis_behandlingskobling")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "dryrun")
            it.requireArray("vedtaksperioder") {
                requireKey("vedtaksperiodeId")
                requireArray("behandlinger") {
                    requireKey("behandlingId", "opprettet", "utbetalinger")
                }
            }
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val dao = transaksjon.spleisBehandlingskoblingDao
        val dryrun = packet["dryrun"].asBoolean()
        val resultater =
            packet["vedtaksperioder"].values().flatMap { vedtaksperiode ->
                val spleisBehandlinger =
                    vedtaksperiode["behandlinger"].values().map { behandling ->
                        SpleisBehandling(
                            behandlingId = behandling["behandlingId"].asUUID(),
                            opprettet = behandling["opprettet"].asLocalDateTime(),
                            utbetalinger = behandling["utbetalinger"].values().map { it.asUUID() },
                        )
                    }
                dao.finnBehandlinger(vedtaksperiode["vedtaksperiodeId"].asUUID()).map { rad ->
                    val kobling = kobl(rad, spleisBehandlinger)
                    val antallVarsler =
                        if (kobling.spleisBehandlingId == null || dryrun) {
                            dao.antallVarslerUtenSpleisBehandlingId(rad.id)
                        } else {
                            dao.settSpleisBehandlingIdPåVarsler(rad.id, kobling.spleisBehandlingId)
                        }
                    kobling to antallVarsler
                }
            }

        val dryrunTag = dryrun.toString()
        resultater.groupBy { (kobling) -> kobling.utfall }.forEach { (utfall, liste) ->
            Metrics.counter("spleis_behandlingskobling_rader", "utfall", utfall.name, "dryrun", dryrunTag).increment(liste.size.toDouble())
            Metrics
                .counter("spleis_behandlingskobling_varsler", "utfall", utfall.name, "dryrun", dryrunTag)
                .increment(liste.sumOf { (_, antall) -> antall }.toDouble())
        }
        resultater.mapNotNull { (kobling) -> kobling.kontroll }.groupingBy { it }.eachCount().forEach { (kontroll, antall) ->
            Metrics.counter("spleis_behandlingskobling_kontroll", "resultat", kontroll.name, "dryrun", dryrunTag).increment(antall.toDouble())
        }

        loggInfo(
            "Behandlingskobling (dryrun: $dryrun): " +
                Utfall.entries.joinToString { utfall ->
                    val liste = resultater.filter { (kobling) -> kobling.utfall == utfall }
                    "${utfall.name}=${liste.size} rader/${liste.sumOf { (_, antall) -> antall }} varsler"
                } + ", " +
                Kontroll.entries.joinToString { kontroll -> "${kontroll.name}=${resultater.count { (kobling) -> kobling.kontroll == kontroll }}" },
            "fødselsnummer" to packet["fødselsnummer"].asString(),
        )
    }

    internal data class SpleisBehandling(
        val behandlingId: UUID,
        val opprettet: LocalDateTime,
        val utbetalinger: List<UUID>,
    )

    internal enum class Utfall {
        // Raden har alt spleis-id
        KJENT,

        // Én spleis-behandling har hatt utbetalingen til raden
        EKSAKT,

        // Spleis-behandlingen som var åpen da raden ble opprettet
        TIDSREGEL,
        INGEN_SPLEISBEHANDLINGER,
    }

    // Tidsregelen sammenlignet med fasiten, for rader der fasiten er kjent
    internal enum class Kontroll { ENIG, UENIG }

    internal data class Kobling(
        val utfall: Utfall,
        val spleisBehandlingId: UUID?,
        val kontroll: Kontroll?,
    )

    internal companion object {
        internal fun kobl(
            rad: SpesialistBehandling,
            spleisBehandlinger: List<SpleisBehandling>,
        ): Kobling {
            val etterTid = tidsregel(rad.opprettet, spleisBehandlinger)

            fun kontroll(fasit: UUID) = etterTid?.let { if (it == fasit) Kontroll.ENIG else Kontroll.UENIG }

            rad.spleisBehandlingId?.let { kjent ->
                return Kobling(Utfall.KJENT, kjent, kontroll(kjent))
            }
            val eksakt = rad.utbetalingId?.let { utbetalingId -> spleisBehandlinger.singleOrNull { utbetalingId in it.utbetalinger } }
            if (eksakt != null) {
                return Kobling(Utfall.EKSAKT, eksakt.behandlingId, kontroll(eksakt.behandlingId))
            }
            if (etterTid == null) return Kobling(Utfall.INGEN_SPLEISBEHANDLINGER, null, null)
            return Kobling(Utfall.TIDSREGEL, etterTid, null)
        }

        // Den siste spleis-behandlingen som var opprettet da raden ble opprettet. Rader fra før første behandling får den første.
        private fun tidsregel(
            opprettet: LocalDateTime,
            spleisBehandlinger: List<SpleisBehandling>,
        ): UUID? {
            val sortert = spleisBehandlinger.sortedBy { it.opprettet }
            return (sortert.lastOrNull { !it.opprettet.isAfter(opprettet) } ?: sortert.firstOrNull())?.behandlingId
        }
    }
}
