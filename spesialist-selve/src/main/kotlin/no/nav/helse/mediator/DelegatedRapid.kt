package no.nav.helse.mediator

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

internal class DelegatedRapid(
    private val rapidsConnection: RapidsConnection,
    private val beforeRiverAction: () -> Unit,
    private val afterRiverAction: (String, MessageContext) -> Unit,
    private val errorAction: (Exception, String) -> Unit
) : RapidsConnection(), RapidsConnection.MessageListener {

    init {
        rapidsConnection.register(this)
    }

    override fun rapidName(): String {
        return "Spesialist"
    }

    override fun onMessage(message: String, context: MessageContext) {
        try {
            beforeRiverAction()
            notifyMessage(message, context)
            afterRiverAction(message, context)
        } catch (err: Exception) {
            errorAction(err, message)
            throw err
        }
    }

    override fun publish(message: String) {
        rapidsConnection.publish(message)
    }

    override fun publish(key: String, message: String) {
        rapidsConnection.publish(key, message)
    }

    override fun start() = throw IllegalStateException()
    override fun stop() = throw IllegalStateException()
}
