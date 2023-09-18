package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi

internal class PersisterOverstyringArbeidsforholdCommand(
    private val oid: UUID,
    private val hendelseId: UUID,
    private val fødselsnummer: String,
    private val overstyrteArbeidsforhold: List<OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi>,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyrteArbeidsforhold.forEach {
            overstyringDao.persisterOverstyringArbeidsforhold(
                hendelseId = hendelseId,
                eksternHendelseId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = it.orgnummer,
                deaktivert = it.deaktivert,
                begrunnelse = it.begrunnelse,
                forklaring = it.forklaring,
                saksbehandlerRef = oid,
                skjæringstidspunkt = skjæringstidspunkt,
                tidspunkt = opprettet
            )
        }
        return true
    }
}
