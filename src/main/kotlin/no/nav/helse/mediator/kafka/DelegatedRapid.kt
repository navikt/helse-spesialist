package no.nav.helse.mediator.kafka

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

    override fun onMessage(message: String, context: MessageContext) {
        try {
            beforeRiverAction()
            listeners.forEach { it.onMessage(message, context) }
            afterRiverAction(message, context)
        } catch (err: Exception) {
            errorAction(err, message)
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
