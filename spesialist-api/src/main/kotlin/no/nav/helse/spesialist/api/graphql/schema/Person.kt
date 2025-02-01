package no.nav.helse.spesialist.api.graphql.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import no.nav.helse.spesialist.api.graphql.mutation.VedtakUtfall
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLNyttInntektsforholdPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Infotrygdutbetaling(
    val fom: String,
    val tom: String,
    val grad: String,
    val dagsats: Double,
    val typetekst: String,
    val organisasjonsnummer: String,
)

data class Saksbehandler(
    val navn: String,
    val ident: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reservasjon(
    val kanVarsles: Boolean,
    val reservert: Boolean,
)

data class UnntattFraAutomatiskGodkjenning(
    val erUnntatt: Boolean,
    val arsaker: List<String>,
    val tidspunkt: LocalDateTime?,
)

data class Enhet(
    val id: String,
    val navn: String,
)

data class Tildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
)

data class PaVent(
    val frist: LocalDate?,
    val oid: UUID,
)

data class Avslag(
    val type: Avslagstype,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)

data class VedtakBegrunnelse(
    val utfall: VedtakUtfall,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
)

data class Annullering(
    val saksbehandlerIdent: String,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val arsaker: List<String>,
    val begrunnelse: String?,
)

data class Person(
    private val snapshot: GraphQLPerson,
    private val personinfo: Personinfo,
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
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) {
    fun versjon(): Int = snapshot.versjon

    fun aktorId(): String = snapshot.aktorId

    fun fodselsnummer(): String = snapshot.fodselsnummer

    fun dodsdato(): LocalDate? = snapshot.dodsdato

    fun personinfo(): Personinfo = personinfo

    fun enhet(): Enhet = personApiDao.finnEnhet(snapshot.fodselsnummer).let { Enhet(it.id, it.navn) }

    fun tildeling(): Tildeling? =
        tildelingApiDao.tildelingForPerson(snapshot.fodselsnummer)?.let {
            Tildeling(
                navn = it.navn,
                epost = it.epost,
                oid = it.oid,
            )
        }

    @Suppress("unused")
    fun tilleggsinfoForInntektskilder(): List<TilleggsinfoForInntektskilde> {
        return snapshot.vilkarsgrunnlag.flatMap { vilkårsgrunnlag ->
            val avviksvurdering = avviksvurderinghenter.hentAvviksvurdering(vilkårsgrunnlag.id)
            (
                avviksvurdering?.sammenligningsgrunnlag?.innrapporterteInntekter?.map { innrapportertInntekt ->
                    innrapportertInntekt.arbeidsgiverreferanse
                } ?: emptyList()
            ) + vilkårsgrunnlag.inntekter.map { inntekt -> inntekt.arbeidsgiver }
        }.toSet().map { orgnr ->
            TilleggsinfoForInntektskilde(
                orgnummer = orgnr,
                navn = arbeidsgiverApiDao.finnNavn(orgnr) ?: "navn er utilgjengelig",
            )
        }
    }

    fun arbeidsgivere(): List<ApiArbeidsgiver> {
        val overstyringer = overstyringApiDao.finnOverstyringer(snapshot.fodselsnummer)

        return snapshot.arbeidsgivere.map { arbeidsgiver ->
            ApiArbeidsgiver(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                navn = arbeidsgiverApiDao.finnNavn(arbeidsgiver.organisasjonsnummer) ?: "navn er utilgjengelig",
                bransjer = arbeidsgiverApiDao.finnBransjer(arbeidsgiver.organisasjonsnummer),
                ghostPerioder = arbeidsgiver.ghostPerioder.tilGhostPerioder(arbeidsgiver.organisasjonsnummer),
                nyeInntektsforholdPerioder = arbeidsgiver.nyeInntektsforholdPerioder.tilNyeInntektsforholdPerioder(),
                fødselsnummer = snapshot.fodselsnummer,
                generasjoner = arbeidsgiver.generasjoner,
                apiOppgaveService = apiOppgaveService,
                saksbehandlerhåndterer = saksbehandlerhåndterer,
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
            )
        }
    }

    @Suppress("unused")
    fun infotrygdutbetalinger(): List<Infotrygdutbetaling>? =
        personApiDao
            .finnInfotrygdutbetalinger(snapshot.fodselsnummer)
            ?.let { objectMapper.readValue(it) }

    fun vilkarsgrunnlag(): List<Vilkarsgrunnlag> = snapshot.vilkarsgrunnlag.map { it.tilVilkarsgrunnlag(avviksvurderinghenter) }

    private fun List<GraphQLGhostPeriode>.tilGhostPerioder(organisasjonsnummer: String): List<ApiGhostPeriode> =
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

    private fun List<GraphQLNyttInntektsforholdPeriode>.tilNyeInntektsforholdPerioder(): List<ApiNyttInntektsforholdPeriode> =
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
            Saksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        dager =
            overstyrteDager.map { dag ->
                ApiDagoverstyring.ApiOverstyrtDag(
                    dato = dag.dato,
                    type = dag.type,
                    fraType = dag.fraType,
                    grad = dag.grad,
                    fraGrad = dag.fraGrad,
                )
            },
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun OverstyringInntektDto.tilInntektoverstyring() =
    ApiInntektoverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            Saksbehandler(
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
            Saksbehandler(
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
            Saksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        skjonnsfastsatt =
            ApiSykepengegrunnlagskjonnsfastsetting.ApiSkjonnsfastsattSykepengegrunnlag(
                arsak = årsak,
                type = type,
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

private fun OverstyringMinimumSykdomsgradDto.tilMinimumSykdomsgradOverstyring() =
    ApiMinimumSykdomsgradOverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            Saksbehandler(
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
