package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.AvbrytOppgaveCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksperiodeReberegnet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: SpleisBehandlingId,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        spleisBehandlingId = SpleisBehandlingId(UUID.fromString(jsonNode["behandlingId"].asText())),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiodeOrNull(vedtaksperiodeId)) { "Fant ikke vedtaksperiode med id: $vedtaksperiodeId" }
        kommandostarter { vedtaksperiodeReberegnet(this@VedtaksperiodeReberegnet, vedtaksperiode, spleisBehandlingId, sessionContext) }
    }
}

internal class VedtaksperiodeReberegnetCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    spleisBehandlingId: SpleisBehandlingId,
    periodehistorikkDao: PeriodehistorikkDao,
    spesialistBehandlingId: UUID,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VedtaksperiodeReberegnetPeriodehistorikk(
                spesialistBehandlingId = spesialistBehandlingId,
                periodehistorikkDao = periodehistorikkDao,
            ),
            ReserverPersonHvisTildeltCommand(fødselsnummer = fødselsnummer),
            AvbrytOppgaveCommand(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                vedtaksperiodeId = vedtaksperiodeId,
            ),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId),
            ikkesuspenderendeCommand("fjernVedtak") { sessionContext: SessionContext, _: Outbox ->
                val vedtak = sessionContext.vedtakRepository.finn(spleisBehandlingId) ?: return@ikkesuspenderendeCommand
                if (vedtak.behandletAvSpleis) {
                    log.warn("Spleis har behandlet svar på godkjenningsbehov for perioden, det er merkelig at spesialist behandler godkjenningsbehov etterpå")
                    return@ikkesuspenderendeCommand
                }
                log.info("Sletter vedtak for $spleisBehandlingId")
                sessionContext.vedtakRepository.slett(spleisBehandlingId)
            },
        )

    private val log = LoggerFactory.getLogger(this::class.java)
}
