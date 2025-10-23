package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.withMDC
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import java.util.UUID

internal fun SessionFactory.wrapOgDeleger(
    messageContext: MessageContext,
    meldingnavn: String,
    meldingtype: MeldingDao.Meldingtype,
    vedtaksperiodeId: UUID?,
    packet: JsonMessage,
    inTransaction: SessionContext.(outbox: MutableList<UtgåendeHendelse>) -> Unit,
) {
    withMDC(
        buildMap {
            put("meldingId", packet["@id"].asText())
            put("meldingnavn", meldingnavn)
            put("vedtaksperiodeId", packet["vedtaksperiodeId"].asText())
        },
    ) {
        logg.info("Melding $meldingnavn mottatt")
        val meldingJson = packet.toJson()
        sikkerlogg.info("Melding $meldingnavn mottatt:\n$meldingJson")

        try {
            val meldingPubliserer = MessageContextMeldingPubliserer(messageContext)
            val outbox = mutableListOf<UtgåendeHendelse>()
            this.transactionalSessionScope { sessionContext ->
                sessionContext.meldingDao.lagre(
                    id = packet["@id"].asUUID(),
                    json = meldingJson,
                    meldingtype = meldingtype,
                    vedtaksperiodeId = vedtaksperiodeId,
                )
            }
            this.transactionalSessionScope { sessionContext ->
                sessionContext.inTransaction(outbox)
            }
            outbox.forEach { utgåendeHendelse ->
                meldingPubliserer.publiser(
                    fødselsnummer = packet["fødselsnummer"].asText(),
                    hendelse = utgåendeHendelse,
                    årsak = meldingnavn,
                )
            }
        } finally {
            logg.info("Melding $meldingnavn lest")
            sikkerlogg.info("Melding $meldingnavn lest")
        }
    }
}
