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
import java.time.Duration
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
                val vedtaksperiodeId = vedtaksperiode["vedtaksperiodeId"].asUUID()
                dao.finnBehandlinger(vedtaksperiodeId).map { rad ->
                    val kobling = kobl(rad, spleisBehandlinger)
                    if (kobling.kontroll?.erUenig == true) {
                        loggUenighet(packet["fødselsnummer"].asString(), vedtaksperiodeId, rad, kobling, spleisBehandlinger)
                    }
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

    private fun loggUenighet(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        rad: SpesialistBehandling,
        kobling: Kobling,
        spleisBehandlinger: List<SpleisBehandling>,
    ) {
        val fasit = spleisBehandlinger.find { it.behandlingId == kobling.spleisBehandlingId }
        val etterTid = spleisBehandlinger.find { it.behandlingId == kobling.tidsregel }
        loggInfo(
            "Behandlingskobling: tidsregelen er uenig (${kobling.kontroll?.name})",
            "fødselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId.toString(),
            "radId" to rad.id.toString(),
            "radOpprettet" to rad.opprettet.toString(),
            "utfall" to kobling.utfall.name,
            "fasitBehandlingId" to kobling.spleisBehandlingId.toString(),
            "fasitOpprettet" to fasit?.opprettet.toString(),
            "tidsregelBehandlingId" to kobling.tidsregel.toString(),
            "tidsregelOpprettet" to etterTid?.opprettet.toString(),
            "minutterFraFasitTilRad" to fasit?.let { Duration.between(it.opprettet, rad.opprettet).toMinutes() }.toString(),
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
    internal enum class Kontroll(
        val erUenig: Boolean,
    ) {
        ENIG(false),

        // Tidsregelen valgte en spleis-behandling opprettet før fasiten
        UENIG_FOR_TIDLIG(true),

        // Tidsregelen valgte en spleis-behandling opprettet etter fasiten
        UENIG_FOR_SENT(true),

        // Tidsregelen valgte en annen spleis-behandling med samme tidsstempel som fasiten
        UENIG_UAVGJORT(true),

        // Fasiten finnes ikke blant spleis-behandlingene i meldingen
        UENIG_FASIT_MANGLER(true),
    }

    internal data class Kobling(
        val utfall: Utfall,
        val spleisBehandlingId: UUID?,
        val kontroll: Kontroll?,
        val tidsregel: UUID? = null,
    )

    internal companion object {
        internal fun kobl(
            rad: SpesialistBehandling,
            spleisBehandlinger: List<SpleisBehandling>,
        ): Kobling {
            val etterTid = tidsregel(rad.opprettet, spleisBehandlinger)

            fun kontroll(fasit: UUID): Kontroll? {
                if (etterTid == null) return null
                if (etterTid.behandlingId == fasit) return Kontroll.ENIG
                val fasitOpprettet =
                    spleisBehandlinger.find { it.behandlingId == fasit }?.opprettet ?: return Kontroll.UENIG_FASIT_MANGLER
                return when {
                    etterTid.opprettet == fasitOpprettet -> Kontroll.UENIG_UAVGJORT
                    etterTid.opprettet.isBefore(fasitOpprettet) -> Kontroll.UENIG_FOR_TIDLIG
                    else -> Kontroll.UENIG_FOR_SENT
                }
            }

            rad.spleisBehandlingId?.let { kjent ->
                return Kobling(Utfall.KJENT, kjent, kontroll(kjent), etterTid?.behandlingId)
            }
            val eksakt = rad.utbetalingId?.let { utbetalingId -> spleisBehandlinger.singleOrNull { utbetalingId in it.utbetalinger } }
            if (eksakt != null) {
                return Kobling(Utfall.EKSAKT, eksakt.behandlingId, kontroll(eksakt.behandlingId), etterTid?.behandlingId)
            }
            if (etterTid == null) return Kobling(Utfall.INGEN_SPLEISBEHANDLINGER, null, null)
            return Kobling(Utfall.TIDSREGEL, etterTid.behandlingId, null, etterTid.behandlingId)
        }

        // Den siste spleis-behandlingen som var opprettet da raden ble opprettet. Rader fra før første behandling får den første.
        // Har flere behandlinger samme tidsstempel, velges den første av dem i rekkefølgen fra spleis.
        private fun tidsregel(
            opprettet: LocalDateTime,
            spleisBehandlinger: List<SpleisBehandling>,
        ): SpleisBehandling? {
            val sortert = spleisBehandlinger.sortedBy { it.opprettet }
            val valgtTidspunkt = (sortert.lastOrNull { !it.opprettet.isAfter(opprettet) } ?: sortert.firstOrNull())?.opprettet
            return sortert.firstOrNull { it.opprettet == valgtTidspunkt }
        }
    }
}
