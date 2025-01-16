package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import org.slf4j.LoggerFactory
import java.util.UUID

class CommandContext(
    private val id: UUID,
    sti: List<Int> = emptyList(),
    private val hash: UUID? = null,
) {
    private val data = mutableListOf<Any>()
    private val sti: MutableList<Int> = sti.toMutableList()
    private var tidligFerdigstilt = false
    private val observers = mutableSetOf<CommandContextObserver>()

    fun nyObserver(observer: CommandContextObserver) {
        observers.add(observer)
    }

    internal fun avregistrerObserver(observer: UtgåendeMeldingerObserver) {
        observers.remove(observer)
    }

    fun behov(behov: Behov) {
        observers.forEach {
            it.behov(behov = behov, commandContextId = id)
        }
    }

    internal fun id() = id

    fun hendelse(hendelse: UtgåendeHendelse) {
        observers.forEach { it.hendelse(hendelse) }
    }

    private fun kommandokjedetilstandEndret(kommandokjedeEndretEvent: KommandokjedeEndretEvent) {
        observers.forEach { it.tilstandEndret(kommandokjedeEndretEvent) }
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

    internal fun opprett(
        commandContextDao: CommandContextRepository,
        hendelseId: UUID,
    ) {
        commandContextDao.opprett(hendelseId, id)
    }

    internal fun avbrytAlleForPeriode(
        commandContextRepository: CommandContextRepository,
        vedtaksperiodeId: UUID,
    ) {
        val avbrutteKommandokjeder = commandContextRepository.avbryt(vedtaksperiodeId, id)
        avbrutteKommandokjeder.forEach { (contextId, hendelseId) ->
            kommandokjedetilstandEndret(KommandokjedeEndretEvent.Avbrutt(contextId, hendelseId))
        }
    }

    private fun ferdigstill() {
        tidligFerdigstilt = true
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()

    internal fun utfør(
        commandContextDao: CommandContextRepository,
        hendelseId: UUID,
        command: Command,
    ): Boolean {
        val newHash = command.hash().convertToUUID()
        if (hash != null && newHash != hash) {
            logger.info(
                "Restarter kommandokjede ${command.name} fordi rekkefølgen, antallet kommandoer eller navn på en eller flere kommandoer i kjeden har endret seg.",
            )
            sti.clear()
        }
        return utfør(command).also { ferdig ->
            if (tidligFerdigstilt || ferdig) {
                commandContextDao.ferdig(hendelseId, id).also {
                    kommandokjedetilstandEndret(KommandokjedeEndretEvent.Ferdig(command.name, id, hendelseId))
                }
            } else {
                commandContextDao.suspendert(hendelseId, id, newHash, sti).also {
                    kommandokjedetilstandEndret(KommandokjedeEndretEvent.Suspendert(command.name, sti, id, hendelseId))
                }
            }
        }
    }

    private fun utfør(command: Command) =
        when {
            sti.isEmpty() -> command.execute(this)
            else -> command.resume(this)
        }

    internal companion object {
        private val logger = LoggerFactory.getLogger(CommandContext::class.java)

        internal fun Command.ferdigstill(context: CommandContext): Boolean {
            logger.info(
                "Kommando ${this.javaClass.simpleName} ferdigstilte {}",
                keyValue("context_id", "${context.id}"),
            )
            context.ferdigstill()
            return true
        }

        internal fun run(
            context: CommandContext,
            commands: List<Command>,
            runner: (command: Command) -> Boolean,
        ) = commands.all {
            if (context.tidligFerdigstilt) {
                true
            } else {
                runner(it)
            }
        }

        internal fun String.convertToUUID() = UUID.nameUUIDFromBytes(this.toByteArray())
    }
}
