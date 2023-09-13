package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.PersisterOverstyringTidslinjeCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.spesialist.api.overstyring.Dagtype

/**
 * Tar vare på overstyring fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class OverstyringTidslinje(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    orgnummer: String,
    begrunnelse: String,
    overstyrteDager: List<OverstyringDag>,
    opprettet: LocalDateTime,
    private val json: String,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        PersisterOverstyringTidslinjeCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            overstyringDao = overstyringDao,
            opprettet = opprettet,
        ),
        PubliserOverstyringCommand(
            eventName = "overstyr_tidslinje",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal data class OverstyringDag(
        val dato: LocalDate,
        val type: Dagtype,
        val fraType: Dagtype?,
        val grad: Int?,
        val fraGrad: Int?,
    )
}