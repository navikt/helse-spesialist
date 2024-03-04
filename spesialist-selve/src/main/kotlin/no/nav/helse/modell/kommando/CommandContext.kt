package no.nav.helse.modell.kommando

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.modell.CommandContextDao
import org.slf4j.LoggerFactory

internal class CommandContext(private val id: UUID, sti: List<Int> = emptyList(), private val hash: UUID? = null) {
    private val data = mutableListOf<Any>()
    private val sti: MutableList<Int> = sti.toMutableList()
    private var ferdigstilt = false
    private val observers = mutableSetOf<UtgåendeMeldingerObserver>()

    internal fun nyObserver(observer: UtgåendeMeldingerObserver) {
        observers.add(observer)
    }

    internal fun behov(behovtype: String, params: Map<String, Any> = emptyMap()) {
        observers.forEach {
            it.behov(behovtype, mapOf("contextId" to id), params)
        }
    }

    internal fun id() = id

    /**
     * Publisere svar tilbake på rapid, typisk svar på godkjenningsbehov
     */
    internal fun publiser(melding: String) {
        observers.forEach { it.hendelse(melding) }
    }

    internal fun add(data: Any) {
        this.data.add(data)
    }

    internal fun suspendert(index: Int) {
        sti.add(0, index)
    }

    internal fun clear() {
        sti.clear()
    }

    internal fun register(command: MacroCommand) {
        if (sti.isEmpty()) return
        command.restore(sti.removeAt(0))
    }

    internal fun sti() = sti.toList()

    internal fun opprett(commandContextDao: CommandContextDao, hendelseId: UUID) {
        commandContextDao.opprett(hendelseId, id)
    }

    internal fun avbryt(commandContextDao: CommandContextDao, vedtaksperiodeId: UUID) {
        commandContextDao.avbryt(vedtaksperiodeId, id)
    }

    private fun ferdigstill() {
        ferdigstilt = true
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()

    internal fun utfør(commandContextDao: CommandContextDao, hendelseId: UUID, command: Command) = try {
        val newHash = command.hash().convertToUUID()
        if (hash != null && newHash != hash) {
            logger.info(
                "Restarter kommandokjede ${command.name} fordi rekkefølgen, antallet kommandoer eller navn på en eller flere kommandoer i kjeden har endret seg."
            )
            sti.clear()
        }
        utfør(command).also {
            if (ferdigstilt || it) commandContextDao.ferdig(hendelseId, id)
            else commandContextDao.suspendert(hendelseId, id, newHash, sti)
        }
    } catch (rootErr: Exception) {
        try {
            commandContextDao.feil(hendelseId, id)
        } catch (nestedErr: Exception) {
            throw RuntimeException("Feil ved lagring av FEIL-tilstand: $nestedErr", rootErr)
        }
        throw rootErr
    }

    private fun utfør(command: Command) = when {
        sti.isEmpty() -> command.execute(this)
        else -> command.resume(this)
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(CommandContext::class.java)
        internal fun Command.ferdigstill(context: CommandContext) : Boolean {
            logger.info("Kommando ${this.javaClass.simpleName} ferdigstilte {}", keyValue("context_id", "${context.id}"))
            context.ferdigstill()
            return true
        }
        internal fun run(context: CommandContext, commands: List<Command>, runner:(command: Command) -> Boolean) =
            commands.all {
                if (context.ferdigstilt) true
                else runner(it)
            }

        internal fun String.convertToUUID() = UUID.nameUUIDFromBytes(this.toByteArray())
    }
}
