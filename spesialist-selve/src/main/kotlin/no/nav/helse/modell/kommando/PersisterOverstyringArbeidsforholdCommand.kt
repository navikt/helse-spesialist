package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.modell.overstyring.OverstyringDao
import java.time.LocalDate
import java.util.*

internal class PersisterOverstyringArbeidsforholdCommand(
    private val oid: UUID,
    private val eventId: UUID,
    private val fødselsnummer: String,
    private val overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
    private val skjæringstidspunkt: LocalDate,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyrteArbeidsforhold.forEach {
            overstyringDao.persisterOverstyringArbeidsforhold(
                hendelseId = eventId,
                fødselsnummer = fødselsnummer,
                orgnummer = it.orgnummer,
                deaktivert = it.deaktivert,
                begrunnelse = it.begrunnelse,
                forklaring = it.forklaring,
                saksbehandlerRef = oid,
                skjæringstidspunkt = skjæringstidspunkt
            )
        }
        return true
    }
}
