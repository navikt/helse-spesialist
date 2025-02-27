package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.logg.sikkerlogg
import java.time.LocalDate
import java.time.YearMonth

internal class PersisterInntektCommand(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val personDao: PersonDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true

        sikkerlogg.info("Inntekter er ikke tidligere lagret for person med fødselsnummer: $fødselsnummer, sender behov")
        return trengerInntekt(context)
    }

    override fun resume(context: CommandContext): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true
        val løsning = context.get<Inntektløsning>() ?: return trengerInntekt(context)

        sikkerlogg.info("Lagrer inntekter for person med fødselsnummer: $fødselsnummer")
        løsning.lagre(personDao, fødselsnummer, skjæringstidspunkt)
        return true
    }

    private fun trengerInntekt(context: CommandContext): Boolean {
        context.behov(
            Behov.InntekterForSykepengegrunnlag(
                fom = skjæringstidspunkt.minusMonths(12).toYearMonth(),
                tom = skjæringstidspunkt.minusMonths(1).toYearMonth(),
            ),
        )

        return false
    }

    private fun LocalDate.toYearMonth() = YearMonth.of(this.year, this.month)
}
