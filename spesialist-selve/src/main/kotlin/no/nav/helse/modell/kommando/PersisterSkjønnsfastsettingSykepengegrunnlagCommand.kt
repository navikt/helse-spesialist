package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver

internal class PersisterSkjønnsfastsettingSykepengegrunnlagCommand(
    private val oid: UUID,
    private val hendelseId: UUID,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
    private val opprettet: LocalDateTime,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
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
