package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID

internal class CommandContext(private val id: UUID, sti: List<Int> = emptyList(), private val hash: UUID? = null) {
    private val data = mutableListOf<Any>()
    private val sti: MutableList<Int> = sti.toMutableList()
    private var ferdigstilt = false
    private val observers = mutableSetOf<CommandContextObserver>()

    internal fun nyObserver(observer: CommandContextObserver) {
        observers.add(observer)
    }

    internal fun avregistrerObserver(observer: UtgåendeMeldingerObserver) {
        observers.remove(observer)
    }

    internal fun behov(
        behovtype: String,
        params: Map<String, Any> = emptyMap(),
    ) {
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

    private fun publiserTilstandEndring(melding: String) {
        observers.forEach { it.tilstandEndring(melding) }
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
        commandContextDao: CommandContextDao,
        hendelseId: UUID,
    ) {
        commandContextDao.opprett(hendelseId, id)
    }

    internal fun avbryt(
        commandContextDao: CommandContextDao,
        meldingId: UUID,
    ) {
        commandContextDao.avbrutt(meldingId, id)
        publiserAvbrutt(id, meldingId)
    }

    internal fun avbrytAlleForPeriode(
        commandContextDao: CommandContextDao,
        vedtaksperiodeId: UUID,
    ) {
        val avbrutteKommandokjeder = commandContextDao.avbryt(vedtaksperiodeId, id)
        avbrutteKommandokjeder.forEach {
            publiserAvbrutt(it.first, it.second)
        }
    }

    private fun publiserAvbrutt(
        contextId: UUID,
        meldingId: UUID,
    ) {
        publiserTilstandEndring(
            JsonMessage.newMessage(
                "kommandokjede_avbrutt",
                mutableMapOf(
                    "commandContextId" to contextId,
                    "meldingId" to meldingId,
                ),
            ).toJson(),
        )
    }

    private fun ferdigstill() {
        ferdigstilt = true
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()

    internal fun utfør(
        commandContextDao: CommandContextDao,
        hendelseId: UUID,
        command: Command,
    ) = try {
        val newHash = command.hash().convertToUUID()
        if (hash != null && newHash != hash) {
            logger.info(
                "Restarter kommandokjede ${command.name} fordi rekkefølgen, antallet kommandoer eller navn på en eller flere kommandoer i kjeden har endret seg.",
            )
            sti.clear()
        }
        utfør(command).also {
            if (ferdigstilt || it) {
                commandContextDao.ferdig(hendelseId, id).also {
                    publiserTilstandEndring(
                        JsonMessage.newMessage(
                            "kommandokjede_ferdigstilt",
                            mutableMapOf(
                                "commandContextId" to id,
                                "meldingId" to hendelseId,
                                "command" to command.name,
                            ),
                        ).toJson(),
                    )
                }
            } else {
                commandContextDao.suspendert(hendelseId, id, newHash, sti).also {
                    publiserTilstandEndring(
                        JsonMessage.newMessage(
                            "kommandokjede_suspendert",
                            mutableMapOf(
                                "commandContextId" to id,
                                "meldingId" to hendelseId,
                                "command" to command.name,
                                "sti" to sti,
                            ),
                        ).toJson(),
                    )
                }
            }
        }
    } catch (rootErr: Exception) {
        try {
            commandContextDao.feil(hendelseId, id).also {
                publiserTilstandEndring(
                    JsonMessage.newMessage(
                        "kommandokjede_feilet",
                        mutableMapOf(
                            "commandContextId" to id,
                            "meldingId" to hendelseId,
                            "command" to command.name,
                            "sti" to sti,
                        ),
                    ).toJson(),
                )
            }
        } catch (nestedErr: Exception) {
            throw RuntimeException("Feil ved lagring av FEIL-tilstand: $nestedErr", rootErr)
        }
        throw rootErr
    }

    private fun utfør(command: Command) =
        when {
            sti.isEmpty() -> command.execute(this)
            else -> command.resume(this)
        }

    internal companion object {
        private val logger = LoggerFactory.getLogger(CommandContext::class.java)

        internal fun Command.ferdigstill(context: CommandContext): Boolean {
            logger.info("Kommando ${this.javaClass.simpleName} ferdigstilte {}", keyValue("context_id", "${context.id}"))
            context.ferdigstill()
            return true
        }

        internal fun run(
            context: CommandContext,
            commands: List<Command>,
            runner: (command: Command) -> Boolean,
        ) = commands.all {
            if (context.ferdigstilt) {
                true
            } else {
                runner(it)
            }
        }

        internal fun String.convertToUUID() = UUID.nameUUIDFromBytes(this.toByteArray())
    }
}
