package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.api.Refusjonselement
import no.nav.helse.modell.overstyring.OverstyringDao

internal class PersisterOverstyringInntektCommand(
    private val oid: UUID,
    private val hendelseId: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val begrunnelse: String,
    private val forklaring: String,
    private val månedligInntekt: Double,
    private val fraMånedligInntekt: Double,
    private val skjæringstidspunkt: LocalDate,
    private val refusjonsopplysninger: List<Refusjonselement>?,
    private val fraRefusjonsopplysninger: List<Refusjonselement>?,
    private val opprettet: LocalDateTime,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyringInntekt(
            hendelseId = hendelseId,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            saksbehandlerRef = oid,
            månedligInntekt = månedligInntekt,
            fraMånedligInntekt = fraMånedligInntekt,
            skjæringstidspunkt = skjæringstidspunkt,
            tidspunkt = opprettet,
            refusjonsopplysninger = refusjonsopplysninger,
            fraRefusjonsopplysninger = fraRefusjonsopplysninger,
        )
        return true
    }
}
