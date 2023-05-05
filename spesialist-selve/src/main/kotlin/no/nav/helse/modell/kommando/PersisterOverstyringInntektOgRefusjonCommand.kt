package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto

internal class PersisterOverstyringInntektOgRefusjonCommand(
    private val oid: UUID,
    private val hendelseId: UUID,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<OverstyrArbeidsgiverDto>,
    private val opprettet: LocalDateTime,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            hendelseId = hendelseId,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            saksbehandlerRef = oid,
            skjæringstidspunkt = skjæringstidspunkt,
            tidspunkt = opprettet,
        )
        return true
    }
}
