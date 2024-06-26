package no.nav.helse.mediator

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

internal class DelegatedRapid(
    private val rapidsConnection: RapidsConnection,
    private val beforeRiversAction: () -> Unit,
    private val skalBehandleMelding: (String) -> Boolean,
    private val afterRiversAction: (String) -> Unit,
    private val errorAction: (Exception, String) -> Unit,
) : RapidsConnection(), RapidsConnection.MessageListener {
    init {
        rapidsConnection.register(this)
    }

    override fun rapidName(): String {
        return "Spesialist"
    }

    override fun onMessage(
        message: String,
        context: MessageContext,
    ) {
        try {
            beforeRiversAction()
            if (skalBehandleMelding(message)) notifyMessage(message, context)
            afterRiversAction(message)
        } catch (err: Exception) {
            errorAction(err, message)
            throw err
        }
    }

    override fun publish(message: String) {
        rapidsConnection.publish(message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        rapidsConnection.publish(key, message)
    }

    override fun start() = throw IllegalStateException()

    override fun stop() = throw IllegalStateException()
}
