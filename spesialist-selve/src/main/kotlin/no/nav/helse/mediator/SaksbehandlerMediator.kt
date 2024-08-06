package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.AvslagDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.overstyring.Overstyringlagrer
import no.nav.helse.mediator.overstyring.Saksbehandlingsmelder
import no.nav.helse.mediator.påvent.PåVentRepository
import no.nav.helse.mediator.saksbehandler.SaksbehandlerLagrer
import no.nav.helse.mediator.saksbehandler.SaksbehandlerMapper.tilApiversjon
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.saksbehandler.handlinger.PåVent
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.withMDC
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgang
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.schema.AnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.Avslag
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatRepository
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.FJERN_FRA_PA_VENT
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.avvisVarsler
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.godkjennVarsler
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.harAktiveVarsler
import no.nav.helse.spesialist.api.vedtaksperiode.ApiGenerasjonRepository
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

internal class SaksbehandlerMediator(
    dataSource: DataSource,
    private val versjonAvKode: String,
    private val rapidsConnection: RapidsConnection,
    private val oppgaveService: OppgaveService,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val stansAutomatiskBehandlingMediator: StansAutomatiskBehandlingMediator,
) : Saksbehandlerhåndterer {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val generasjonRepository = ApiGenerasjonRepository(dataSource)
    private val varselRepository = ApiVarselRepository(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val abonnementDao = AbonnementDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val påVentDao = PåVentDao(dataSource)
    private val notatRepository: NotatRepository = NotatRepository(notatDao = NotatDao(dataSource))
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val avslagDao = AvslagDao(dataSource)
    private val annulleringDao = AnnulleringDao(dataSource)

    override fun <T : HandlingFraApi> håndter(
        handlingFraApi: T,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handlingFraApi.tilModellversjon()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        tell(modellhandling)
        saksbehandler.register(Saksbehandlingsmelder(rapidsConnection))
        saksbehandler.register(Subsumsjonsmelder(versjonAvKode, rapidsConnection))
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.oid().toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring -> håndter(modellhandling, saksbehandler)
                is Oppgavehandling -> håndter(modellhandling, saksbehandler)
                is PåVent -> håndter(modellhandling, saksbehandler)
                is Personhandling -> håndter(modellhandling, saksbehandler)
                is Annullering -> håndter(modellhandling, saksbehandler)
                else -> modellhandling.utførAv(saksbehandler)
            }
            sikkerlogg.info("Handling ${modellhandling.loggnavn()} utført")
        }
    }

    private fun håndter(
        handling: Annullering,
        saksbehandler: Saksbehandler,
    ) {
        try {
            annulleringDao.lagreAnnullering(handling.toDto(), saksbehandler)
            handling.utførAv(saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Oppgavehandling,
        saksbehandler: Saksbehandler,
    ) {
        try {
            oppgaveService.håndter(handling, saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) = try {
        stansAutomatiskBehandlingMediator.håndter(handling, saksbehandler)
        handling.utførAv(saksbehandler)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun håndter(
        handling: PåVent,
        saksbehandler: Saksbehandler,
    ) {
        try {
            when (handling) {
                is no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent ->
                    notatRepository.lagreForOppgaveId(
                        handling.oppgaveId,
                        handling.notatTekst,
                        saksbehandler.oid(),
                        NotatType.PaaVent,
                    )
                is no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent ->
                    if (påVentDao.erPåVent(handling.oppgaveId)) {
                        periodehistorikkDao.lagre(FJERN_FRA_PA_VENT, saksbehandler.oid(), handling.oppgaveId)
                    }
                is no.nav.helse.modell.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag -> Unit
            }
            oppgaveService.håndter(handling, saksbehandler)
            PåVentRepository(påVentDao).apply {
                this.lagre(
                    påVent = handling,
                    saksbehandlerOid = saksbehandler.oid(),
                )
            }
            sikkerlogg.info(
                "Utfører handling ${handling.loggnavn()} på oppgave ${handling.oppgaveId} på vegne av saksbehandler $saksbehandler",
            )
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Overstyring,
        saksbehandler: Saksbehandler,
    ) {
        val fødselsnummer = handling.gjelderFødselsnummer()
        oppgaveService.håndter(handling)
        reservasjonDao.reserverPerson(saksbehandler.oid(), fødselsnummer)
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        Overstyringlagrer(overstyringDao).apply {
            this.lagre(handling, saksbehandler.oid())
        }
        handling.utførAv(saksbehandler)
    }

    override fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    ) {
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

    override fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Avslag> = avslagDao.finnAlleAvslag(vedtaksperiodeId, utbetalingId)

    override fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    ) {
        if (avslag.handling == Avslagshandling.INVALIDER) {
            avslagDao.invaliderAvslag(oppgaveId)
        } else {
            avslagDao.lagreAvslag(oppgaveId, avslag.data!!, saksbehandlerFraApi.oid)
        }
    }

    override fun hentAnnullering(utbetalingId: UUID): no.nav.helse.spesialist.api.graphql.schema.Annullering? {
        return annulleringDao.finnAnnullering(utbetalingId)
    }

    override fun håndter(
        godkjenning: GodkjenningDto,
        behandlingId: UUID,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val fødselsnummer = oppgaveApiDao.finnFødselsnummer(godkjenning.oppgavereferanse)

        if (godkjenning.godkjent) {
            val perioderTilBehandling = generasjonRepository.perioderTilBehandling(godkjenning.oppgavereferanse)
            if (perioderTilBehandling.harAktiveVarsler()) {
                throw ManglerVurderingAvVarsler(godkjenning.oppgavereferanse)
            }
            perioderTilBehandling.godkjennVarsler(
                fødselsnummer,
                behandlingId,
                saksbehandler.ident(),
                this::vurderVarsel,
            )
        } else {
            val periodeTilGodkjenning = generasjonRepository.periodeTilGodkjenning(godkjenning.oppgavereferanse)
            periodeTilGodkjenning.avvisVarsler(fødselsnummer, behandlingId, saksbehandler.ident(), this::vurderVarsel)
        }

        påVentDao.slettPåVent(godkjenning.oppgavereferanse)
        oppgaveApiDao.lagreBehandlingsreferanse(godkjenning.oppgavereferanse, behandlingId)
        godkjenning.avslag?.let {
            if (it.handling == Avslagshandling.INVALIDER) {
                avslagDao.invaliderAvslag(godkjenning.oppgavereferanse)
            } else {
                avslagDao.lagreAvslag(godkjenning.oppgavereferanse, it.data!!, saksbehandler.oid())
            }
        }
    }

    override fun håndterTotrinnsvurdering(oppgavereferanse: Long) {
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
        if (perioderTilBehandling.harAktiveVarsler()) {
            throw ManglerVurderingAvVarsler(oppgavereferanse)
        }
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
        val message =
            JsonMessage.newMessage(
                "varsel_endret",
                mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "behandling_id" to behandlingId,
                    "varsel_id" to varselId,
                    "varseltittel" to varseltittel,
                    "varselkode" to varselkode,
                    "forrige_status" to forrigeStatus.name,
                    "gjeldende_status" to gjeldendeStatus.name,
                ),
            )
        sikkerlogg.info(
            "Publiserer varsel_endret for varsel med {}, {}, {}",
            kv("varselId", varselId),
            kv("varselkode", varselkode),
            kv("status", gjeldendeStatus),
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun Modellfeil.tilApiversjon(): no.nav.helse.spesialist.api.feilhåndtering.Modellfeil =
            when (this) {
                is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
                is OppgaveTildeltNoenAndre -> {
                    val (oid, navn, epost) = this.saksbehandler.tilApiversjon()
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre(
                        TildelingApiDto(
                            navn,
                            epost,
                            oid,
                        ),
                    )
                }

                is OppgaveAlleredeSendtBeslutter ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter(
                        oppgaveId,
                    )

                is OppgaveAlleredeSendtIRetur ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur(
                        oppgaveId,
                    )

                is OppgaveKreverVurderingAvToSaksbehandlere ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverVurderingAvToSaksbehandlere(
                        oppgaveId,
                    )

                is ManglerTilgang -> IkkeTilgang(oid, oppgaveId)
            }
    }

    private fun SaksbehandlerFraApi.tilSaksbehandler() =
        Saksbehandler(epost, oid, navn, ident, TilgangskontrollørForApi(this.grupper, tilgangsgrupper))

    private fun HandlingFraApi.tilModellversjon(): Handling =
        when (this) {
            is ArbeidsforholdOverstyringHandling -> this.tilModellversjon()
            is InntektOgRefusjonOverstyring -> this.tilModellversjon()
            is TidslinjeOverstyring -> this.tilModellversjon()
            is Skjonnsfastsettelse -> this.tilModellversjon()
            is AnnulleringData -> this.tilModellversjon()
            is TildelOppgave -> this.tilModellversjon()
            is AvmeldOppgave -> this.tilModellversjon()
            is LeggPåVent -> this.tilModellversjon()
            is FjernPåVent -> this.tilModellversjon()
            is FjernPåVentUtenHistorikkinnslag -> this.tilModellversjon()
            is OpphevStans -> this.tilModellversjon()
            else -> throw IllegalStateException("Støtter ikke handling ${this::class.simpleName}")
        }

    private fun ArbeidsforholdOverstyringHandling.tilModellversjon(): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold(
            fødselsnummer = fodselsnummer,
            aktørId = aktorId,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            overstyrteArbeidsforhold =
                overstyrteArbeidsforhold.map { overstyrtArbeidsforhold ->
                    Arbeidsforhold(
                        organisasjonsnummer = overstyrtArbeidsforhold.orgnummer,
                        deaktivert = overstyrtArbeidsforhold.deaktivert,
                        begrunnelse = overstyrtArbeidsforhold.begrunnelse,
                        forklaring = overstyrtArbeidsforhold.forklaring,
                        lovhjemmel =
                            overstyrtArbeidsforhold.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                    )
                },
        )

    private fun Skjonnsfastsettelse.tilModellversjon(): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsgivere =
                arbeidsgivere.map { ag ->
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = ag.organisasjonsnummer,
                        årlig = ag.arlig,
                        fraÅrlig = ag.fraArlig,
                        årsak = ag.arsak,
                        type =
                            when (ag.type) {
                                OMREGNET_ARSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                                RAPPORTERT_ARSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                                ANNET -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
                            },
                        begrunnelseMal = ag.begrunnelseMal,
                        begrunnelseFritekst = ag.begrunnelseFritekst,
                        begrunnelseKonklusjon = ag.begrunnelseKonklusjon,
                        lovhjemmel =
                            ag.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                        initierendeVedtaksperiodeId = ag.initierendeVedtaksperiodeId,
                    )
                },
        )

    private fun InntektOgRefusjonOverstyring.tilModellversjon(): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsgivere =
                arbeidsgivere.map { overstyrArbeidsgiver ->
                    OverstyrtArbeidsgiver(
                        overstyrArbeidsgiver.organisasjonsnummer,
                        overstyrArbeidsgiver.manedligInntekt,
                        overstyrArbeidsgiver.fraManedligInntekt,
                        overstyrArbeidsgiver.refusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        overstyrArbeidsgiver.fraRefusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        begrunnelse = overstyrArbeidsgiver.begrunnelse,
                        forklaring = overstyrArbeidsgiver.forklaring,
                        lovhjemmel =
                            overstyrArbeidsgiver.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                    )
                },
        )

    private fun TidslinjeOverstyring.tilModellversjon(): OverstyrtTidslinje =
        OverstyrtTidslinje(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager =
                dager.map {
                    OverstyrtTidslinjedag(
                        dato = it.dato,
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        lovhjemmel =
                            it.lovhjemmel?.let { lovhjemmel ->
                                Lovhjemmel(
                                    paragraf = lovhjemmel.paragraf,
                                    ledd = lovhjemmel.ledd,
                                    bokstav = lovhjemmel.bokstav,
                                    lovverk = lovhjemmel.lovverk,
                                    lovverksversjon = lovhjemmel.lovverksversjon,
                                )
                            },
                    )
                },
            begrunnelse = begrunnelse,
        )

    private fun AnnulleringData.tilModellversjon(): Annullering =
        Annullering(
            aktørId = this.aktorId,
            fødselsnummer = this.fodselsnummer,
            organisasjonsnummer = this.organisasjonsnummer,
            vedtaksperiodeId = this.vedtaksperiodeId,
            utbetalingId = this.utbetalingId,
            begrunnelser = this.begrunnelser,
            arsaker = this.arsaker?.map { arsak -> AnnulleringArsak(key = arsak._key, arsak = arsak.arsak) },
            kommentar = this.kommentar,
        )

    private fun LeggPåVent.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent =
        no.nav.helse.modell.saksbehandler.handlinger
            .LeggPåVent(oppgaveId, frist, skalTildeles, begrunnelse, notatTekst)

    private fun FjernPåVent.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent =
        no.nav.helse.modell.saksbehandler.handlinger
            .FjernPåVent(oppgaveId)

    private fun FjernPåVentUtenHistorikkinnslag.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag =
        no.nav.helse.modell.saksbehandler.handlinger
            .FjernPåVentUtenHistorikkinnslag(oppgaveId)

    private fun TildelOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .TildelOppgave(this.oppgaveId)

    private fun AvmeldOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .AvmeldOppgave(this.oppgaveId)

    private fun OpphevStans.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.OpphevStans =
        no.nav.helse.modell.saksbehandler.handlinger
            .OpphevStans(this.fødselsnummer, this.begrunnelse)
}
