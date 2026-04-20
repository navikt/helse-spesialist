package no.nav.helse.mediator

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.tellAutomatisering
import no.nav.helse.tellAvvistÅrsak

class GodkjenningMediator(
    private val opptegnelseRepository: OpptegnelseRepository,
) {
    internal fun automatiskUtbetaling(
        commandContext: CommandContext,
        behov: GodkjenningsbehovData,
    ) {
        behov.godkjennAutomatisk()
        commandContext.hendelse(behov.medLøsning())
        commandContext.hendelse(behov.lagVedtaksperiodeGodkjentAutomatisk())
        val opptegnelse =
            Opptegnelse.ny(
                identitetsnummer = Identitetsnummer.fraString(behov.fødselsnummer),
                type = Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
            )
        opptegnelseRepository.lagre(opptegnelse)
        tellAutomatisering()
        loggInfo(
            "Automatisk godkjenning av vedtaksperiode ${behov.vedtaksperiodeId}",
            "fødselsnummer" to behov.fødselsnummer,
        )
    }

    internal fun automatiskAvvisning(
        outbox: Outbox,
        begrunnelser: List<String>,
        behov: GodkjenningsbehovData,
    ) {
        behov.avvisAutomatisk(
            begrunnelser = begrunnelser,
        )
        val identitetsnummer = Identitetsnummer.fraString(behov.fødselsnummer)
        outbox.leggTil(identitetsnummer, behov.medLøsning(), "automatisk avvisning")
        outbox.leggTil(identitetsnummer, behov.lagVedtaksperiodeAvvistAutomatisk(), "automatisk avvisning")
        val opptegnelse =
            Opptegnelse.ny(
                identitetsnummer = identitetsnummer,
                type = Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV,
            )
        opptegnelseRepository.lagre(opptegnelse)
        begrunnelser.forEach { tellAvvistÅrsak(it) }
        tellAutomatisering()
        loggInfo(
            "Automatisk avvisning av vedtaksperiode ${behov.vedtaksperiodeId}",
            "fødselsnummer" to behov.fødselsnummer,
            "begrunnelser" to begrunnelser,
        )
    }
}
