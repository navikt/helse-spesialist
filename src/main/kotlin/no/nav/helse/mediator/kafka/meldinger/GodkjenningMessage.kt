package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate
import java.util.*

internal class GodkjenningMessage(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate
) {
    fun asSpleisBehov(
        personDao: PersonDao,
        arbeidsgiverDao: ArbeidsgiverDao,
        vedtakDao: VedtakDao,
        snapshotDao: SnapshotDao,
        speilSnapshotRestDao: SpeilSnapshotRestDao,
        oppgaveDao: OppgaveDao
    ) = SpleisBehov(
        id = id,
        fødselsnummer = fødselsnummer,
        periodeFom = periodeFom,
        periodeTom = periodeTom,
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        snapshotDao = snapshotDao,
        speilSnapshotRestDao = speilSnapshotRestDao,
        oppgaveDao = oppgaveDao
    )

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val personDao: PersonDao,
        private val arbeidsgiverDao: ArbeidsgiverDao,
        private val vedtakDao: VedtakDao,
        private val snapshotDao: SnapshotDao,
        private val spleisBehovMediator: SpleisBehovMediator,
        private val speilSnapshotRestDao: SpeilSnapshotRestDao,
        private val oppgaveDao: OppgaveDao
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate { it ->
                    it.requireAll("@behov", listOf("Godkjenning"))
                    it.requireKey("fødselsnummer")
                    it.requireKey("aktørId")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = GodkjenningMessage(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            )
            spleisBehovMediator.håndter(context, behov.asSpleisBehov(
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao,
                oppgaveDao = oppgaveDao
            ))
        }
    }
}
