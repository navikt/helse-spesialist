package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class LagreOppdragCommandTest : ApplicationTest() {
    @Test
    fun `lagre arbeidsgiverbeløp og personbeløp`() {
        // given
        val arbeidsgiverbeløp = 40000
        val personbeløp = 30000
        val utbetalingId = UUID.randomUUID()
        val command = command(utbetalingId = utbetalingId, arbeidsgiverbeløp = arbeidsgiverbeløp, personbeløp = personbeløp)

        // when
        command.execute(CommandContext(UUID.randomUUID()), sessionContext, outbox)

        // then
        val forventetUtbetaling =
            Utbetaling(
                utbetalingId = utbetalingId,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                type = Utbetalingtype.UTBETALING,
            )
        assertEquals(forventetUtbetaling, sessionContext.utbetalingDao.hentUtbetaling(utbetalingId))
    }

    private fun command(
        utbetalingId: UUID = UUID.randomUUID(),
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
    ): LagreOppdragCommand {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        return LagreOppdragCommand(
            fødselsnummer,
            organisasjonsnummer,
            utbetalingId,
            type = Utbetalingtype.UTBETALING,
            status = Utbetalingsstatus.IKKE_UTBETALT,
            LocalDateTime.now(),
            arbeidsgiverOppdrag = LagreOppdragCommand.Oppdrag(UUID.randomUUID().toString(), organisasjonsnummer),
            personOppdrag = LagreOppdragCommand.Oppdrag(UUID.randomUUID().toString(), fødselsnummer),
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            json = "{}",
        )
    }
}
