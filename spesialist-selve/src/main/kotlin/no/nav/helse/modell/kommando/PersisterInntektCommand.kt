package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory

internal class PersisterInntektCommand (
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val personDao: PersonDao,
) : Command {
    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun resume(context: CommandContext): Boolean {
        if (!Toggle.Inntekter.enabled) return true

        val løsning = context.get<Inntektløsning>() ?: return trengerInntekt(context)

        løsning.lagre(personDao, fødselsnummer, skjæringstidspunkt).also {
            sikkerLog.info("Lagrer inntekter for person med fødselsnummer: $fødselsnummer")
        }

        return true
    }

    override fun execute(context: CommandContext): Boolean {
        if (!Toggle.Inntekter.enabled) return true

        personDao.findInntekter(fødselsnummer, skjæringstidspunkt) ?: return trengerInntekt(context).also {
            sikkerLog.info("Inntekter er ikke tidligere lagret for person med fødselsnummer: $fødselsnummer, sender behov")
        }

        return true
    }

    private fun trengerInntekt(context: CommandContext): Boolean {
        context.behov(
            "InntekterForSykepengegrunnlag",
            mapOf(
                "beregningStart" to skjæringstidspunkt.minusMonths(3).toString(),
                "beregningSlutt" to skjæringstidspunkt.minusMonths(1).toString()
            )
        )

        return false
    }
}