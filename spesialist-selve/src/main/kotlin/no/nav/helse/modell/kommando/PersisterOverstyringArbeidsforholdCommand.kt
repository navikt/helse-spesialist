package no.nav.helse.modell.kommando

import no.nav.helse.modell.overstyring.OverstyringDao
import java.time.LocalDate
import java.util.*

internal class PersisterOverstyringArbeidsforholdCommand(
    private val oid: UUID,
    private val eventId: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val erAktivt: Boolean,
    private val begrunnelse: String,
    private val forklaring: String,
    private val skjæringstidspunkt: LocalDate,
    private val overstyringDao: OverstyringDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        overstyringDao.persisterOverstyringArbeidsforhold(
            hendelseId = eventId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            erAktivt = erAktivt,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            saksbehandlerRef = oid,
            skjæringstidspunkt = skjæringstidspunkt
        )
        return true
    }
}
