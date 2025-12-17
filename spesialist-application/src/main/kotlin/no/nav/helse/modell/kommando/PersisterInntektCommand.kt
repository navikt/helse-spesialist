package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.logg.loggInfo
import java.time.LocalDate
import java.time.YearMonth

internal class PersisterInntektCommand(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val personDao: PersonDao,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true

        loggInfo(
            "Inntekter er ikke tidligere lagret for person, sender behov",
            "fødselsnummer: $fødselsnummer",
        )
        return trengerInntekt(context)
    }

    override fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true
        val løsning = context.get<Inntektløsning>() ?: return trengerInntekt(context)

        loggInfo(
            "Lagrer inntekter for person",
            "fødselsnummer: $fødselsnummer",
        )
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
