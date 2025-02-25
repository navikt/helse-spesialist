package no.nav.helse.spesialist.api.graphql.resolvers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.mapping.tilVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiDagoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiDagtype
import no.nav.helse.spesialist.api.graphql.schema.ApiEnhet
import no.nav.helse.spesialist.api.graphql.schema.ApiGhostPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiInfotrygdutbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgradOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiNyttInntektsforholdPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiPersoninfo
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettingstype
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengegrunnlagskjonnsfastsetting
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.graphql.schema.ApiTilleggsinfoForInntektskilde
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlag
import no.nav.helse.spesialist.api.graphql.schema.PersonSchema
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.application.snapshot.SnapshotGhostPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotNyttInntektsforholdPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import java.time.LocalDate
import java.util.UUID

data class ApiPersonResolver(
    private val sessionFactory: SessionFactory,
    private val snapshot: SnapshotPerson,
    private val personinfo: ApiPersoninfo,
    private val personApiDao: PersonApiDao,
    private val tildelingApiDao: TildelingApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
) : PersonSchema {
    override fun versjon(): Int = snapshot.versjon

    override fun aktorId(): String = snapshot.aktorId

    override fun fodselsnummer(): String = snapshot.fodselsnummer

    override fun dodsdato(): LocalDate? = snapshot.dodsdato

    override fun personinfo(): ApiPersoninfo = personinfo

    override fun enhet(): ApiEnhet = personApiDao.finnEnhet(snapshot.fodselsnummer).let { ApiEnhet(it.id, it.navn) }

    override fun tildeling(): ApiTildeling? =
        tildelingApiDao.tildelingForPerson(snapshot.fodselsnummer)?.let {
            ApiTildeling(
                navn = it.navn,
                epost = it.epost,
                oid = it.oid,
            )
        }

    @Suppress("unused")
    override fun tilleggsinfoForInntektskilder(): List<ApiTilleggsinfoForInntektskilde> {
        return snapshot.vilkarsgrunnlag.flatMap { vilkårsgrunnlag ->
            val avviksvurdering =
                sessionFactory.transactionalSessionScope {
                    it.avviksvurderingRepository.hentAvviksvurdering(vilkårsgrunnlag.id)
                }
            (
                avviksvurdering?.sammenligningsgrunnlag?.innrapporterteInntekter?.map { innrapportertInntekt ->
                    innrapportertInntekt.arbeidsgiverreferanse
                } ?: emptyList()
            ) + vilkårsgrunnlag.inntekter.map { inntekt -> inntekt.arbeidsgiver }
        }.toSet().map { orgnr ->
            ApiTilleggsinfoForInntektskilde(
                orgnummer = orgnr,
                navn = arbeidsgiverApiDao.finnNavn(orgnr) ?: "navn er utilgjengelig",
            )
        }
    }

    override fun arbeidsgivere(): List<ApiArbeidsgiver> {
        val overstyringer = overstyringApiDao.finnOverstyringer(snapshot.fodselsnummer)

        return snapshot.arbeidsgivere.map { arbeidsgiver ->
            ApiArbeidsgiver(
                resolver =
                    ApiArbeidsgiverResolver(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        navn = arbeidsgiverApiDao.finnNavn(arbeidsgiver.organisasjonsnummer) ?: "navn er utilgjengelig",
                        bransjer = arbeidsgiverApiDao.finnBransjer(arbeidsgiver.organisasjonsnummer),
                        ghostPerioder = arbeidsgiver.ghostPerioder.tilGhostPerioder(arbeidsgiver.organisasjonsnummer),
                        nyeInntektsforholdPerioder = arbeidsgiver.nyeInntektsforholdPerioder.tilNyeInntektsforholdPerioder(),
                        fødselsnummer = snapshot.fodselsnummer,
                        generasjoner = arbeidsgiver.generasjoner,
                        apiOppgaveService = apiOppgaveService,
                        saksbehandlerMediator = saksbehandlerMediator,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        risikovurderinger = risikovurderinger,
                        varselRepository = varselRepository,
                        oppgaveApiDao = oppgaveApiDao,
                        periodehistorikkApiDao = periodehistorikkApiDao,
                        notatDao = notatDao,
                        totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                        påVentApiDao = påVentApiDao,
                        overstyringer =
                            overstyringer
                                .filter { it.relevantFor(arbeidsgiver.organisasjonsnummer) }
                                .map { overstyring ->
                                    when (overstyring) {
                                        is OverstyringTidslinjeDto -> overstyring.tilDagoverstyring()
                                        is OverstyringArbeidsforholdDto -> overstyring.tilArbeidsforholdoverstyring()
                                        is OverstyringInntektDto -> overstyring.tilInntektoverstyring()
                                        is SkjønnsfastsettingSykepengegrunnlagDto -> overstyring.tilSykepengegrunnlagSkjønnsfastsetting()
                                        is OverstyringMinimumSykdomsgradDto -> overstyring.tilMinimumSykdomsgradOverstyring()
                                    }
                                },
                        vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                    ),
            )
        }
    }

    @Suppress("unused")
    override fun infotrygdutbetalinger(): List<ApiInfotrygdutbetaling>? =
        personApiDao
            .finnInfotrygdutbetalinger(snapshot.fodselsnummer)
            ?.let { objectMapper.readValue(it) }

    override fun vilkarsgrunnlag(): List<ApiVilkårsgrunnlag> {
        return sessionFactory.transactionalSessionScope { sessionContext ->
            snapshot.vilkarsgrunnlag.map { it.tilVilkarsgrunnlag(sessionContext.avviksvurderingRepository) }
        }
    }

    private fun List<SnapshotGhostPeriode>.tilGhostPerioder(organisasjonsnummer: String): List<ApiGhostPeriode> =
        map {
            ApiGhostPeriode(
                fom = it.fom,
                tom = it.tom,
                skjaeringstidspunkt = it.skjaeringstidspunkt,
                vilkarsgrunnlagId = it.vilkarsgrunnlagId,
                deaktivert = it.deaktivert,
                organisasjonsnummer = organisasjonsnummer,
            )
        }

    private fun List<SnapshotNyttInntektsforholdPeriode>.tilNyeInntektsforholdPerioder(): List<ApiNyttInntektsforholdPeriode> =
        map {
            ApiNyttInntektsforholdPeriode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                organisasjonsnummer = it.organisasjonsnummer,
                skjaeringstidspunkt = it.skjaeringstidspunkt,
                dagligBelop = it.dagligBelop,
                manedligBelop = it.manedligBelop,
            )
        }
}

private fun OverstyringTidslinjeDto.tilDagoverstyring() =
    ApiDagoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        dager =
            overstyrteDager.map { dag ->
                ApiDagoverstyring.ApiOverstyrtDag(
                    dato = dag.dato,
                    type = dag.type.tilApiDagtype(),
                    fraType = dag.fraType?.tilApiDagtype(),
                    grad = dag.grad,
                    fraGrad = dag.fraGrad,
                )
            },
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun Dagtype.tilApiDagtype() =
    when (this) {
        Dagtype.Sykedag -> ApiDagtype.Sykedag
        Dagtype.SykedagNav -> ApiDagtype.SykedagNav
        Dagtype.Feriedag -> ApiDagtype.Feriedag
        Dagtype.Egenmeldingsdag -> ApiDagtype.Egenmeldingsdag
        Dagtype.Permisjonsdag -> ApiDagtype.Permisjonsdag
        Dagtype.Arbeidsdag -> ApiDagtype.Arbeidsdag
        Dagtype.ArbeidIkkeGjenopptattDag -> ApiDagtype.ArbeidIkkeGjenopptattDag
        Dagtype.Foreldrepengerdag -> ApiDagtype.Foreldrepengerdag
        Dagtype.AAPdag -> ApiDagtype.AAPdag
        Dagtype.Omsorgspengerdag -> ApiDagtype.Omsorgspengerdag
        Dagtype.Pleiepengerdag -> ApiDagtype.Pleiepengerdag
        Dagtype.Svangerskapspengerdag -> ApiDagtype.Svangerskapspengerdag
        Dagtype.Opplaringspengerdag -> ApiDagtype.Opplaringspengerdag
        Dagtype.Dagpengerdag -> ApiDagtype.Dagpengerdag
        Dagtype.Avvistdag -> ApiDagtype.Avvistdag
        Dagtype.Helg -> ApiDagtype.Helg
    }

private fun OverstyringInntektDto.tilInntektoverstyring() =
    ApiInntektoverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        inntekt =
            ApiInntektoverstyring.ApiOverstyrtInntekt(
                forklaring = forklaring,
                begrunnelse = begrunnelse,
                manedligInntekt = månedligInntekt,
                fraManedligInntekt = fraMånedligInntekt,
                skjaeringstidspunkt = skjæringstidspunkt,
                refusjonsopplysninger =
                    refusjonsopplysninger?.map {
                        ApiInntektoverstyring.ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
                fraRefusjonsopplysninger =
                    fraRefusjonsopplysninger?.map {
                        ApiInntektoverstyring.ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun OverstyringArbeidsforholdDto.tilArbeidsforholdoverstyring() =
    ApiArbeidsforholdoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        deaktivert = deaktivert,
        skjaeringstidspunkt = skjæringstidspunkt,
        forklaring = forklaring,
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun SkjønnsfastsettingSykepengegrunnlagDto.tilSykepengegrunnlagSkjønnsfastsetting() =
    ApiSykepengegrunnlagskjonnsfastsetting(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        skjonnsfastsatt =
            ApiSykepengegrunnlagskjonnsfastsetting.ApiSkjonnsfastsattSykepengegrunnlag(
                arsak = årsak,
                type = type.tilApiSkjonnsfastsettingstype(),
                begrunnelse = begrunnelse,
                begrunnelseMal = begrunnelseMal,
                begrunnelseFritekst = begrunnelseFritekst,
                begrunnelseKonklusjon = begrunnelseKonklusjon,
                arlig = årlig,
                fraArlig = fraÅrlig,
                skjaeringstidspunkt = skjæringstidspunkt,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun Skjonnsfastsettingstype.tilApiSkjonnsfastsettingstype() =
    when (this) {
        Skjonnsfastsettingstype.OMREGNET_ARSINNTEKT -> ApiSkjonnsfastsettingstype.OMREGNET_ARSINNTEKT
        Skjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT -> ApiSkjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT
        Skjonnsfastsettingstype.ANNET -> ApiSkjonnsfastsettingstype.ANNET
    }

private fun OverstyringMinimumSykdomsgradDto.tilMinimumSykdomsgradOverstyring() =
    ApiMinimumSykdomsgradOverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        minimumSykdomsgrad =
            ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgrad(
                perioderVurdertOk =
                    perioderVurdertOk.map {
                        ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                perioderVurdertIkkeOk =
                    perioderVurdertIkkeOk.map {
                        ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                begrunnelse = begrunnelse,
                initierendeVedtaksperiodeId = vedtaksperiodeId,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )
