package no.nav.helse.sidegig

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

class MinuttRiver(
    rapidsConnection: RapidsConnection,
    private val myDao: MyDao,
) : River.PacketListener {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAny("@event_name", listOf("minutt_spout"))
                }
                validate {
                    it.requireKey("@id")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        oppdaterAnnulleringerMedManglendeVedtaksperiodeId()
    }

    private fun oppdaterAnnulleringerMedManglendeVedtaksperiodeId() {
        logg.info("Setter i gang med oppdatering av annulleringer som mangler vedtaksperiodeId")

        val annulleringer =
            myDao
                .find10Annulleringer()
                .also { logg.info("Hentet ${it.size} annulleringer ${it.map { annullering -> annullering.id }}") }
        val annulleringerOppdatert =
            annulleringer.mapNotNull { annullering ->
                myDao
                    .findUtbetalingId(
                        arbeidsgiverFagsystemId = annullering.arbeidsgiver_fagsystem_id,
                        personFagsystemId = annullering.person_fagsystem_id,
                    ).also { utbetalingId ->
                        if (utbetalingId == null) {
                            logg.info("Fant ingen utbetalingid for annullering ${annullering.id}")
                        } else {
                            logg.info("Fant utbetalingid $utbetalingId for annullering ${annullering.id}")
                        }
                    }?.let { utbetalingId ->
                        myDao
                            .finnBehandlingISykefraværstilfelle(utbetalingId)
                            .also { behandling ->
                                if (behandling != null) {
                                    logg.info("Fant behandling ${behandling.behandlingId} for utbetalingid $utbetalingId")
                                } else {
                                    logg.info("Fant ingen behandling for utbetalingid $utbetalingId")
                                }
                            }
                    }?.let { behandling ->
                        myDao
                            .finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
                            .also { vedtaksperiodeId ->
                                if (vedtaksperiodeId != null) {
                                    logg.info("Fant vedtaksperiodeid $vedtaksperiodeId for behandling ${behandling.behandlingId}")
                                } else {
                                    logg.info("Fant ingen vedtaksperiodeid for behandling ${behandling.behandlingId}")
                                }
                            }
                    }?.let { førsteVedtaksperiodeId ->
                        logg.info("Dry run - Ville oppdatert annullering ${annullering.id} med vedtaksperiodeid $førsteVedtaksperiodeId")
//                    myDao.oppdaterAnnulleringMedVedtaksperiodeId(
//                        annullering.id,
//                        førsteVedtaksperiodeId
//                    )
                    }
            }
        logg.info("Hentet ${annulleringer.size} annulleringer og oppdaterte ${annulleringerOppdatert.size} annulleringer med vedtaksperiodeId")
    }
}
