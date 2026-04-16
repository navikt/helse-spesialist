package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import java.time.LocalDate
import java.time.YearMonth

internal class PersisterInntektCommand(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val personDao: PersonDao,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true

        loggInfo(
            "Inntekter er ikke tidligere lagret for person, sender behov",
            "fødselsnummer" to fødselsnummer,
        )
        return trengerInntekt(commandContext)
    }

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        if (personDao.finnInntekter(fødselsnummer, skjæringstidspunkt) != null) return true
        val løsning = commandContext.get<Inntektløsning>() ?: return trengerInntekt(commandContext)

        loggInfo(
            "Lagrer inntekter for person",
            "fødselsnummer" to fødselsnummer,
        )
        løsning.lagre(personDao, fødselsnummer, skjæringstidspunkt)
        return true
    }

    private fun trengerInntekt(commandContext: CommandContext): Boolean {
        commandContext.behov(
            Behov.InntekterForSykepengegrunnlag(
                fom = skjæringstidspunkt.minusMonths(12).toYearMonth(),
                tom = skjæringstidspunkt.minusMonths(1).toYearMonth(),
            ),
        )

        return false
    }

    private fun LocalDate.toYearMonth() = YearMonth.of(this.year, this.month)
}
