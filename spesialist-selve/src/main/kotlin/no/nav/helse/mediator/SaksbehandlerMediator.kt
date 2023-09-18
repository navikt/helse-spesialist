package no.nav.helse.mediator

import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.overstyring.Overstyringlagrer
import no.nav.helse.mediator.saksbehandler.SaksbehandlerLagrer
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.saksbehandler.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.saksbehandler.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.saksbehandler.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.Lovhjemmel
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.withMDC
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.tell
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.harAktiveVarsler
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.vurderVarsler
import no.nav.helse.spesialist.api.vedtaksperiode.ApiGenerasjonRepository
import org.slf4j.LoggerFactory

class SaksbehandlerMediator(
    dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
) : SaksbehandlerObserver, Saksbehandlerhåndterer {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val generasjonRepository = ApiGenerasjonRepository(dataSource)
    private val varselRepository = ApiVarselRepository(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val abonnementDao = AbonnementDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)

    override fun <T : HandlingFraApi> håndter(handlingFraApi: T, saksbehandlerFraApi: SaksbehandlerFraApi) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        val handlingId = UUID.randomUUID()
        tell(handlingFraApi)
        saksbehandler.register(this)
        val modellhandling = handlingFraApi.tilHandling()
        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.oid().toString(),
                "handlingId" to handlingId.toString()
            )
        ) {
            sikkerlogg.info("Utfører handling ${handlingFraApi.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring -> håndter(modellhandling, saksbehandler)
                else -> modellhandling.utførAv(saksbehandler)
            }
            sikkerlogg.info("Handling ${handlingFraApi.loggnavn()} utført")
        }
    }

    private fun <T : Overstyring> håndter(handling: T, saksbehandler: Saksbehandler) {
        val fødselsnummer = handling.gjelderFødselsnummer()
        val antall = oppgaveApiDao.invaliderOppgaveFor(fødselsnummer)
        sikkerlogg.info("Invaliderer $antall {} for $fødselsnummer", if (antall == 1) "oppgave" else "oppgaver")
        reservasjonDao.reserverPerson(saksbehandler.oid(), fødselsnummer, false)
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        Overstyringlagrer(overstyringDao).apply {
            this.lagre(handling, saksbehandler.oid())
        }
        handling.utførAv(saksbehandler)
    }

    override fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {
        val json = event.somJsonMessage().toJson()
        logg.info("Publiserer ${event.eventName()}")
        sikkerlogg.info(
            "Publiserer ${event.eventName()} for {}:\n{}",
            kv("fødselsnummer", fødselsnummer),
            kv("json", json)
        )
        rapidsConnection.publish(fødselsnummer, json)
    }

    override fun inntektOgRefusjonOverstyrt(fødselsnummer: String, event: OverstyrtInntektOgRefusjonEvent) {
        val message = event.somJsonMessage()
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    override fun arbeidsforholdOverstyrt(fødselsnummer: String, event: OverstyrtArbeidsforholdEvent) {
        val message = event.somJsonMessage()
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    override fun sykepengegrunnlagSkjønnsfastsatt(fødselsnummer: String, event: SkjønnsfastsattSykepengegrunnlagEvent) {
        val message = event.somJsonMessage()
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    override fun utbetalingAnnullert(fødselsnummer: String, event: AnnullertUtbetalingEvent) {
        val message = event.somJsonMessage()
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    override fun opprettAbonnement(saksbehandlerFraApi: SaksbehandlerFraApi, personidentifikator: String) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        abonnementDao.opprettAbonnement(saksbehandler.oid(), personidentifikator.toLong())
    }

    override fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        abonnementDao.registrerSistekvensnummer(saksbehandler.oid(), sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandler.oid())
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        return opptegnelseDao.finnOpptegnelser(saksbehandler.oid())
    }

    override fun håndter(godkjenning: GodkjenningDto, behandlingId: UUID, saksbehandlerFraApi: SaksbehandlerFraApi) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(godkjenning.oppgavereferanse)
        if (godkjenning.godkjent) {
            if (perioderTilBehandling.harAktiveVarsler())
                throw ManglerVurderingAvVarsler(godkjenning.oppgavereferanse)
        }

        oppgaveApiDao.lagreBehandlingsreferanse(godkjenning.oppgavereferanse, behandlingId)

        val fødselsnummer = oppgaveApiDao.finnFødselsnummer(godkjenning.oppgavereferanse)

        perioderTilBehandling.vurderVarsler(
            godkjenning.godkjent,
            fødselsnummer,
            behandlingId,
            saksbehandler.ident(),
            this::vurderVarsel
        )
    }

    override fun håndterTotrinnsvurdering(oppgavereferanse: Long) {
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
        if (perioderTilBehandling.harAktiveVarsler())
            throw ManglerVurderingAvVarsler(oppgavereferanse)
    }

    private fun vurderVarsel(
        fødselsnummer: String,
        behandlingId: UUID,
        vedtaksperiodeId: UUID,
        varselId: UUID,
        varseltittel: String,
        varselkode: String,
        forrigeStatus: Varsel.Varselstatus,
        gjeldendeStatus: Varsel.Varselstatus,
        saksbehandlerIdent: String,
    ) {
        varselRepository.vurderVarselFor(varselId, gjeldendeStatus, saksbehandlerIdent)
        val message = JsonMessage.newMessage(
            "varsel_endret", mapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiode_id" to vedtaksperiodeId,
                "behandling_id" to behandlingId,
                "varsel_id" to varselId,
                "varseltittel" to varseltittel,
                "varselkode" to varselkode,
                "forrige_status" to forrigeStatus.name,
                "gjeldende_status" to gjeldendeStatus.name
            )
        )
        sikkerlogg.info(
            "Publiserer varsel_endret for varsel med {}, {}, {}",
            kv("varselId", varselId),
            kv("varselkode", varselkode),
            kv("status", gjeldendeStatus)
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(epost, oid, navn, ident)

    private fun HandlingFraApi.tilHandling(): Handling {
        return when (this) {
            is OverstyrArbeidsforholdHandlingFraApi -> this.tilHandling()
            is OverstyrInntektOgRefusjonHandlingFraApi -> this.tilHandling()
            is OverstyrTidslinjeHandlingFraApi -> this.tilHandling()
            is SkjønnsfastsettSykepengegrunnlagHandlingFraApi -> this.tilHandling()
            is AnnulleringHandlingFraApi -> this.tilHandling()
        }
    }

    private fun OverstyrArbeidsforholdHandlingFraApi.tilHandling(): OverstyrtArbeidsforhold {
        return OverstyrtArbeidsforhold(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map {
                OverstyrtArbeidsforhold.Arbeidsforhold(it.orgnummer, it.deaktivert, it.begrunnelse, it.forklaring)
            }
        )
    }

    private fun OverstyrInntektOgRefusjonHandlingFraApi.tilHandling(): OverstyrtInntektOgRefusjon {
        return OverstyrtInntektOgRefusjon(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { overstyrArbeidsgiver ->
                OverstyrtArbeidsgiver(
                    overstyrArbeidsgiver.organisasjonsnummer,
                    overstyrArbeidsgiver.månedligInntekt,
                    overstyrArbeidsgiver.fraMånedligInntekt,
                    overstyrArbeidsgiver.refusjonsopplysninger?.map {
                        Refusjonselement(it.fom, it.tom, it.beløp)
                    },
                    overstyrArbeidsgiver.fraRefusjonsopplysninger?.map {
                        Refusjonselement(it.fom, it.tom, it.beløp)
                    },
                    begrunnelse = overstyrArbeidsgiver.begrunnelse,
                    forklaring = overstyrArbeidsgiver.forklaring,
                    lovhjemmel = overstyrArbeidsgiver.lovhjemmel?.let {
                        Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                    }
                )
            },
        )
    }

    private fun OverstyrTidslinjeHandlingFraApi.tilHandling(): OverstyrtTidslinje {
        return OverstyrtTidslinje(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager.map {
                OverstyrtTidslinjedag(
                    dato = it.dato,
                    type = it.type,
                    fraType = it.fraType,
                    grad = it.grad,
                    fraGrad = it.fraGrad,
                    lovhjemmel = it.lovhjemmel?.let { lovhjemmel ->
                        Lovhjemmel(
                            paragraf = lovhjemmel.paragraf,
                            ledd = lovhjemmel.ledd,
                            bokstav = lovhjemmel.bokstav,
                            lovverk = lovhjemmel.lovverk,
                            lovverksversjon = lovhjemmel.lovverksversjon,
                        )
                    })
            },
            begrunnelse = begrunnelse
        )
    }

    private fun SkjønnsfastsettSykepengegrunnlagHandlingFraApi.tilHandling(): SkjønnsfastsattSykepengegrunnlag {
        return SkjønnsfastsattSykepengegrunnlag(
            aktørId,
            fødselsnummer,
            skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { arbeidsgiverDto ->
                SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver(
                    arbeidsgiverDto.organisasjonsnummer,
                    arbeidsgiverDto.årlig,
                    arbeidsgiverDto.fraÅrlig,
                    arbeidsgiverDto.årsak,
                    type = when (arbeidsgiverDto.type) {
                        SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                        SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                        SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto.ANNET -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
                    },
                    begrunnelseMal = arbeidsgiverDto.begrunnelseMal,
                    begrunnelseFritekst = arbeidsgiverDto.begrunnelseFritekst,
                    begrunnelseKonklusjon = arbeidsgiverDto.begrunnelseKonklusjon,
                    lovhjemmel = arbeidsgiverDto.lovhjemmel?.let {
                        Lovhjemmel(
                            paragraf = it.paragraf,
                            ledd = it.ledd,
                            bokstav = it.bokstav,
                            lovverk = it.lovverk,
                            lovverksversjon = it.lovverksversjon
                        )
                    },
                    initierendeVedtaksperiodeId = arbeidsgiverDto.initierendeVedtaksperiodeId
                )
            }
        )
    }

    private fun AnnulleringHandlingFraApi.tilHandling(): Annullering {
        return Annullering(
            aktørId = this.aktørId,
            fødselsnummer = this.fødselsnummer,
            organisasjonsnummer = this.organisasjonsnummer,
            fagsystemId = this.fagsystemId,
            begrunnelser = this.begrunnelser,
            kommentar = this.kommentar
        )
    }
}