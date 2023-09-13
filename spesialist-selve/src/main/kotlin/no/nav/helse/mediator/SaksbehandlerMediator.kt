package no.nav.helse.mediator

import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.modell.AnnullertUtbetalingEvent
import no.nav.helse.spesialist.api.modell.OverstyrtArbeidsforholdEvent
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.modell.OverstyrtTidslinjeEvent
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.modell.SaksbehandlerObserver
import no.nav.helse.spesialist.api.modell.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Annullering
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Handling
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Overstyring
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtArbeidsgiver
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinje
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinjedag
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Refusjonselement
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Subsumsjon
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SaksbehandlerHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandling
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
    private val rapidsConnection: RapidsConnection
): SaksbehandlerObserver, Saksbehandlerhåndterer {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val generasjonRepository = ApiGenerasjonRepository(dataSource)
    private val varselRepository = ApiVarselRepository(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val abonnementDao = AbonnementDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)

    override fun <T: SaksbehandlerHandling> håndter(handling: T, saksbehandlerFraApi: SaksbehandlerFraApi) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val handlingId = UUID.randomUUID()
        tell(handling)
        saksbehandler.register(this)
        saksbehandler.persister(saksbehandlerDao)
        val modellhandling = handling.toModellobjekt()
        no.nav.helse.rapids_rivers.withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.oid().toString(),
                "handlingId" to handlingId.toString()
            )
        ) {
            sikkerlogg.info("Utfører handling ${handling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring -> håndter(modellhandling, saksbehandler)
                else -> modellhandling.utførAv(saksbehandler)
            }
        }
        sikkerlogg.info("Handling ${handling.loggnavn()} utført")
    }

    private fun <T: Overstyring> håndter(handling: T, saksbehandler: Saksbehandler) {
        val fødselsnummer = handling.gjelderFødselsnummer()
        val antall = oppgaveApiDao.invaliderOppgaveFor(fødselsnummer)
        sikkerlogg.info("Invaliderer $antall {} for $fødselsnummer", if (antall == 1) "oppgave" else "oppgaver")
        reservasjonDao.reserverPerson(saksbehandler.oid(), fødselsnummer, false)
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        handling.utførAv(saksbehandler)
    }

    override fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {
        val message = event.somJsonMessage()
        rapidsConnection.publish(fødselsnummer, message.toJson())
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
        saksbehandler.persister(saksbehandlerDao)
        abonnementDao.opprettAbonnement(saksbehandler.oid(), personidentifikator.toLong())
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi, sisteSekvensId: Int): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        saksbehandler.persister(saksbehandlerDao)
        abonnementDao.registrerSistekvensnummer(saksbehandler.oid(), sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandler.oid())
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        saksbehandler.persister(saksbehandlerDao)
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

        perioderTilBehandling.vurderVarsler(godkjenning.godkjent, fødselsnummer, behandlingId, saksbehandler.ident(), this::vurderVarsel)
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
        saksbehandlerIdent: String
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
            StructuredArguments.kv("varselId", varselId),
            StructuredArguments.kv("varselkode", varselkode),
            StructuredArguments.kv("status", gjeldendeStatus)
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(epost, oid, navn, ident)

    private fun SaksbehandlerHandling.toModellobjekt(): Handling {
        return when (this) {
            is OverstyrArbeidsforholdHandling -> this.toModellobjekt()
            is OverstyrInntektOgRefusjonHandling -> this.toModellobjekt()
            is OverstyrTidslinjeHandling -> this.toModellobjekt()
            is SkjønnsfastsettSykepengegrunnlagHandling -> this.toModellobjekt()
            is AnnulleringHandling -> this.toModellobjekt()
        }
    }

    private fun OverstyrArbeidsforholdHandling.toModellobjekt(): OverstyrtArbeidsforhold {
        return OverstyrtArbeidsforhold(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map {
                OverstyrtArbeidsforhold.Arbeidsforhold(it.orgnummer, it.deaktivert, it.begrunnelse, it.forklaring)
            }
        )
    }

    private fun OverstyrInntektOgRefusjonHandling.toModellobjekt(): OverstyrtInntektOgRefusjon {
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
                    subsumsjon = overstyrArbeidsgiver.subsumsjon?.let {
                        Subsumsjon(it.paragraf, it.ledd, it.bokstav)
                    }
                )
            },
        )
    }

    private fun OverstyrTidslinjeHandling.toModellobjekt(): OverstyrtTidslinje {
        return OverstyrtTidslinje(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager.map {
                OverstyrtTidslinjedag(
                    it.dato,
                    it.type,
                    it.fraType,
                    it.grad,
                    it.fraGrad,
                    it.subsumsjon?.let { subsumsjon ->
                        Subsumsjon(
                            paragraf = subsumsjon.paragraf,
                            ledd = subsumsjon.ledd,
                            bokstav = subsumsjon.bokstav
                        )
                    })
            },
            begrunnelse = begrunnelse
        )
    }

    private fun SkjønnsfastsettSykepengegrunnlagHandling.toModellobjekt(): SkjønnsfastsattSykepengegrunnlag {
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
                        SkjønnsfastsettSykepengegrunnlagHandling.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                        SkjønnsfastsettSykepengegrunnlagHandling.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                        SkjønnsfastsettSykepengegrunnlagHandling.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto.ANNET -> SkjønnsfastsattSykepengegrunnlag.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
                    },
                    begrunnelseMal = arbeidsgiverDto.begrunnelseMal,
                    begrunnelseFritekst = arbeidsgiverDto.begrunnelseFritekst,
                    begrunnelseKonklusjon = arbeidsgiverDto.begrunnelseKonklusjon,
                    subsumsjon = arbeidsgiverDto.subsumsjon?.let {
                        Subsumsjon(it.paragraf, it.ledd, it.bokstav)
                    },
                    initierendeVedtaksperiodeId = arbeidsgiverDto.initierendeVedtaksperiodeId
                )
            }
        )
    }

    private fun AnnulleringHandling.toModellobjekt(): Annullering {
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