package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.application.logg.logg
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

    fun add(data: Any) {
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

    fun opprett(
        commandContextDao: CommandContextDao,
        hendelseId: UUID,
    ) {
        commandContextDao.opprett(hendelseId, id)
    }

    internal fun avbrytAlleForPeriode(
        commandContextDao: CommandContextDao,
        vedtaksperiodeId: UUID,
    ) {
        val avbrutteKommandokjeder = commandContextDao.avbryt(vedtaksperiodeId, id)
        avbrutteKommandokjeder.forEach { (contextId, hendelseId) ->
            kommandokjedetilstandEndret(KommandokjedeEndretEvent.Avbrutt(contextId, hendelseId))
        }
    }

    private fun ferdigstill() {
        tidligFerdigstilt = true
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()

    internal fun utfør(
        commandContextDao: CommandContextDao,
        hendelseId: UUID,
        command: Command,
        sessionContext: SessionContext,
    ): Boolean {
        val newHash = command.hash().convertToUUID()
        if (hash != null && newHash != hash) {
            logg.info(
                "Restarter kommandokjede ${command.name} fordi rekkefølgen, antallet kommandoer eller navn på en eller flere kommandoer i kjeden har endret seg.",
            )
            sti.clear()
        }
        return utfør(command, sessionContext).also { ferdig ->
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

    private fun utfør(
        command: Command,
        sessionContext: SessionContext,
    ) = when {
        sti.isEmpty() -> command.execute(this, sessionContext)
        else -> command.resume(this, sessionContext)
    }

    internal companion object {
        internal fun Command.ferdigstill(context: CommandContext): Boolean {
            logg.info(
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
