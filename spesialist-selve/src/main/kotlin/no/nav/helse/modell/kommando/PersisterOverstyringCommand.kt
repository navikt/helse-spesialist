package no.nav.helse.modell.kommando

import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.overstyring.OverstyringDagDto
import java.util.*

internal class PersisterOverstyringCommand(
    private val oid: UUID,
    private val eventId: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val begrunnelse: String,
    private val overstyrteDager: List<OverstyringDagDto>,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyring(
            hendelseId = eventId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            saksbehandlerRef = oid
        )
        return true
    }
}
