package no.nav.helse.spesialist.api

import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeDto
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.harAktiveVarsler
import no.nav.helse.spesialist.api.vedtaksperiode.ApiGenerasjonRepository
import org.slf4j.LoggerFactory

class SaksbehandlerMediator(
    dataSource: DataSource,
    private val rapidsConnection: RapidsConnection
) {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val generasjonRepository = ApiGenerasjonRepository(dataSource)
    private val varselRepository = ApiVarselRepository(dataSource)

    internal fun håndter(annullering: AnnulleringDto, saksbehandler: Saksbehandler) {
        tellAnnullering()
        saksbehandler.persister(saksbehandlerDao)
        val message = annullering.somJsonMessage(saksbehandler).also {
            sikkerlogg.info(
                "Publiserer annullering fra api: {}, {}, {}\n${it.toJson()}",
                kv("fødselsnummer", annullering.fødselsnummer),
                kv("aktørId", annullering.aktørId),
                kv("organisasjonsnummer", annullering.organisasjonsnummer)
            )
        }
        rapidsConnection.publish(annullering.fødselsnummer, message.toJson())
    }

    internal fun håndter(overstyring: OverstyrTidslinjeDto, saksbehandler: Saksbehandler) {
        tellOverstyrTidslinje()
        val message = overstyring.somJsonMessage(saksbehandler.toDto()).also {
            sikkerlogg.info(
                "Publiserer overstyring av tidslinje fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", overstyring.fødselsnummer),
                kv("aktørId", overstyring.aktørId),
                kv("organisasjonsnummer", overstyring.organisasjonsnummer)
            )
        }
        rapidsConnection.publish(overstyring.fødselsnummer, message.toJson())
    }

    internal fun håndter(overstyring: OverstyrInntektOgRefusjonDto, saksbehandler: Saksbehandler) {
        tellOverstyrInntektOgRefusjon()
        val message = overstyring.somJsonMessage(saksbehandler.toDto()).also {
            sikkerlogg.info(
                "Publiserer overstyring av inntekt og refusjon fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", overstyring.fødselsnummer),
                kv("aktørId", overstyring.aktørId),
            )
        }
        rapidsConnection.publish(overstyring.fødselsnummer, message.toJson())
    }

    internal fun håndter(overstyring: OverstyrArbeidsforholdDto, saksbehandler: Saksbehandler) {
        tellOverstyrArbeidsforhold()
        val message = overstyring.somJsonMessage(saksbehandler.toDto()).also {
            sikkerlogg.info(
                "Publiserer overstyring av arbeidsforhold fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", overstyring.fødselsnummer),
                kv("aktørId", overstyring.aktørId),
            )
        }
        rapidsConnection.publish(overstyring.fødselsnummer, message.toJson())
    }

    fun håndter(godkjenning: GodkjenningDto) {
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(godkjenning.oppgavereferanse)
        if (godkjenning.godkjent) {
            if (perioderTilBehandling.harAktiveVarsler())
                throw ManglerVurderingAvVarsler(godkjenning.oppgavereferanse)

            varselRepository.godkjennVarslerFor(godkjenning.oppgavereferanse)
        }
    }

    fun håndterTotrinnsvurdering(oppgavereferanse: Long) {
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
        if (perioderTilBehandling.harAktiveVarsler())
            throw ManglerVurderingAvVarsler(oppgavereferanse)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}