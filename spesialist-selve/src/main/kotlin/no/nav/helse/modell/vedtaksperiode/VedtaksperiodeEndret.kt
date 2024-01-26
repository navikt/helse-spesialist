package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.VedtaksperiodeGenerasjonCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class VedtaksperiodeEndret(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    forårsaketAvId: UUID,
    forrigeTilstand: String,
    gjeldendeTilstand: String,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
    personDao: PersonDao,
    gjeldendeGenerasjon: Generasjon,
) : Kommandohendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        VedtaksperiodeGenerasjonCommand(
            vedtaksperiodeEndretHendelseId = forårsaketAvId,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand,
            gjeldendeGenerasjon = gjeldendeGenerasjon
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

}
