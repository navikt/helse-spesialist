package no.nav.helse.modell.kommando

import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao
import java.util.*

internal class CommandContext(private val id: UUID, sti: List<Int> = emptyList()) {
    private val data = mutableListOf<Any>()
    private val behovsgrupper = mutableListOf<Behovgruppe>()
    private val sti: MutableList<Int> = sti.toMutableList()
    private val meldinger = mutableListOf<String>()

    internal class Behovgruppe {
        private val behov = mutableMapOf<String, Map<String, Any>>()

        internal operator fun contains(behovtype: String) = behovtype in behov
        internal operator fun get(behovtype: String) = behov.getValue(behovtype)
        internal val size get() = behov.size

        internal fun behov(behovtype: String, params: Map<String, Any> = emptyMap()) {
            this.behov[behovtype] = params
        }

        internal fun behov() = behov.toMap()
    }

    internal fun nyBehovgruppe() {
        behovsgrupper.add(Behovgruppe())
    }

    internal fun behov(behovtype: String, params: Map<String, Any> = emptyMap()) {
        if (behovsgrupper.isEmpty()) behovsgrupper.add(Behovgruppe())
        this.behovsgrupper.last().behov(behovtype, params)
    }

    internal fun behovsgrupper() = behovsgrupper.filter { it.size > 0 }.toList()
    internal fun meldinger() = meldinger.toList()

    internal fun publiser(melding: String) {
        meldinger.add(melding)
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

    internal fun harBehov() = behovsgrupper().isNotEmpty()

    internal fun sti() = sti.toList()

    internal fun opprett(commandContextDao: CommandContextDao, hendelse: Hendelse) {
        commandContextDao.opprett(hendelse, id)
    }

    internal fun avbryt(commandContextDao: CommandContextDao, vedtaksperiodeId: UUID) {
        commandContextDao.avbryt(vedtaksperiodeId, id)
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()

    internal fun utfør(commandContextDao: CommandContextDao, hendelse: Hendelse) = try {
        utfør(hendelse).also {
            if (it) commandContextDao.ferdig(hendelse, id)
            else commandContextDao.suspendert(hendelse, id, sti)
        }
    } catch (rootErr: Exception) {
        try {
            commandContextDao.feil(hendelse, id)
        } catch (nestedErr: Exception) {
            throw RuntimeException("Feil ved lagring av FEIL-tilstand: $nestedErr", rootErr)
        }
        throw rootErr
    }

    private fun utfør(command: Command) = when {
        sti.isEmpty() -> command.execute(this)
        else -> command.resume(this)
    }
}
