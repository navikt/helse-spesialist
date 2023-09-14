package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.time.YearMonth
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

    override fun execute(context: CommandContext): Boolean {
        personDao.findInntekter(fødselsnummer, skjæringstidspunkt) ?: return trengerInntekt(context).also {
            sikkerLog.info("Inntekter er ikke tidligere lagret for person med fødselsnummer: $fødselsnummer, sender behov")
        }

        return true
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Inntektløsning>() ?: return trengerInntekt(context)

        løsning.lagre(personDao, fødselsnummer, skjæringstidspunkt).also {
            sikkerLog.info("Lagrer inntekter for person med fødselsnummer: $fødselsnummer")
        }

        return true
    }

    private fun trengerInntekt(context: CommandContext): Boolean {
        context.behov(
            "InntekterForSykepengegrunnlag",
            mapOf(
                "beregningStart" to skjæringstidspunkt.minusMonths(12).toYearMonth().toString(),
                "beregningSlutt" to skjæringstidspunkt.minusMonths(1).toYearMonth().toString(),
            )
        )

        return false
    }

    private fun LocalDate.toYearMonth() = YearMonth.of(this.year, this.month)
}