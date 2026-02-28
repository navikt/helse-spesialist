package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.Test

class LagreOppdragCommandTest {

    @Test
    fun `lagre arbeidsgiverbeløp og personbeløp`() {
        // given
        every { utbetalingDao.finnUtbetalingIdRef(any()) } returns null
        every { utbetalingDao.nyttOppdrag(any(), any()) } returns nextLong()
        val arbeidsgiverbeløp = 40000
        val personbeløp = 30000
        val command = command(arbeidsgiverbeløp = arbeidsgiverbeløp, personbeløp = personbeløp)

        // when
        command.execute(CommandContext(UUID.randomUUID()))

        // then
        verify(exactly = 1) {
            utbetalingDao.opprettUtbetalingId(
                utbetalingId = any(),
                fødselsnummer = any(),
                arbeidsgiverIdentifikator = any(),
                type = any(),
                opprettet = any(),
                arbeidsgiverFagsystemIdRef = any(),
                personFagsystemIdRef = any(),
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp
            )
        }
    }

    private fun command(arbeidsgiverbeløp: Int, personbeløp: Int): LagreOppdragCommand {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        return LagreOppdragCommand(
            fødselsnummer,
            organisasjonsnummer,
            UUID.randomUUID(),
            type = Utbetalingtype.UTBETALING,
            status = Utbetalingsstatus.IKKE_UTBETALT,
            LocalDateTime.now(),
            arbeidsgiverOppdrag = LagreOppdragCommand.Oppdrag(UUID.randomUUID().toString(), organisasjonsnummer),
            personOppdrag = LagreOppdragCommand.Oppdrag(UUID.randomUUID().toString(), fødselsnummer),
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            json = "{}",
            utbetalingDao = utbetalingDao,
            opptegnelseRepository = opptegnelseRepository,
        )
    }

    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val opptegnelseRepository = mockk<OpptegnelseRepository>(relaxed = true)
}
