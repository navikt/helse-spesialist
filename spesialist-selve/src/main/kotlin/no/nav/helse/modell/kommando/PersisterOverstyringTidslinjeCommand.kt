package no.nav.helse.modell.kommando

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.overstyring.OverstyringDagDto

internal class PersisterOverstyringTidslinjeCommand(
    private val oid: UUID,
    private val eventId: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val begrunnelse: String,
    private val overstyrteDager: List<OverstyringDagDto>,
    private val overstyringDao: OverstyringDao,
    private val opprettet: LocalDateTime
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyringTidslinje(
            hendelseId = eventId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            saksbehandlerRef = oid,
            tidspunkt = opprettet
        )
        return true
    }
}
