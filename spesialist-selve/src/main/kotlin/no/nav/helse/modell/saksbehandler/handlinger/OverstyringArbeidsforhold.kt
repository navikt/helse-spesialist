package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.PersisterOverstyringArbeidsforholdCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi

internal class OverstyringArbeidsforhold(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    overstyrteArbeidsforhold: List<OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdDto>,
    skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
    private val json: String,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        PersisterOverstyringArbeidsforholdCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyringDao = overstyringDao,
            opprettet = opprettet
        ),
        PubliserOverstyringCommand(
            eventName = "overstyr_arbeidsforhold",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        )
    )
    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json

}
