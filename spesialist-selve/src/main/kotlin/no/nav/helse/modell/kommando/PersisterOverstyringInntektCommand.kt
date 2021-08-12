package no.nav.helse.modell.kommando

import no.nav.helse.modell.overstyring.OverstyringDao
import java.time.LocalDate
import java.util.*

internal class PersisterOverstyringInntektCommand(
    private val oid: UUID,
    private val eventId: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val begrunnelse: String,
    private val månedligInntekt: Double,
    private val skjæringstidspunkt: LocalDate,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyringInntekt(
            hendelseId = eventId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            saksbehandlerRef = oid,
            månedligInntekt = månedligInntekt,
            skjæringstidspunkt = skjæringstidspunkt
        )
        return true
    }
}
