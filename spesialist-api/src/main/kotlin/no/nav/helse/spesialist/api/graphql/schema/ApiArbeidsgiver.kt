package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLDeprecated
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Arbeidsforhold")
data class ApiArbeidsforhold(
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
)

@GraphQLName("Generasjon")
data class ApiGenerasjon(
    val id: UUID,
    val perioder: List<Periode>,
)

@GraphQLName("ArbeidsgiverInntekterFraAOrdningen")
data class ApiArbeidsgiverInntekterFraAOrdningen(
    val skjaeringstidspunkt: String,
    val inntekter: List<InntektFraAOrdningen>,
)

@GraphQLName("Overstyring")
interface ApiOverstyring {
    val hendelseId: UUID
    val timestamp: LocalDateTime
    val saksbehandler: Saksbehandler
    val ferdigstilt: Boolean
    val vedtaksperiodeId: UUID
}

@GraphQLName("Dagoverstyring")
data class ApiDagoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val dager: List<ApiOverstyrtDag>,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtDag")
    data class ApiOverstyrtDag(
        val dato: LocalDate,
        val type: Dagtype,
        val fraType: Dagtype?,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

@GraphQLName("Inntektoverstyring")
data class ApiInntektoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val inntekt: ApiOverstyrtInntekt,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtInntekt")
    data class ApiOverstyrtInntekt(
        val forklaring: String,
        val begrunnelse: String,
        val manedligInntekt: Double,
        val fraManedligInntekt: Double?,
        val skjaeringstidspunkt: LocalDate,
        val refusjonsopplysninger: List<ApiRefusjonsopplysning>?,
        val fraRefusjonsopplysninger: List<ApiRefusjonsopplysning>?,
    )

    @GraphQLName("Refusjonsopplysning")
    data class ApiRefusjonsopplysning(
        val fom: LocalDate,
        val tom: LocalDate?,
        val belop: Double,
    )
}

@GraphQLName("MinimumSykdomsgradOverstyring")
data class ApiMinimumSykdomsgradOverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val minimumSykdomsgrad: ApiOverstyrtMinimumSykdomsgrad,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtMinimumSykdomsgrad")
    data class ApiOverstyrtMinimumSykdomsgrad(
        val perioderVurdertOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
        val perioderVurdertIkkeOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
        val begrunnelse: String,
        @GraphQLDeprecated("Bruk vedtaksperiodeId i stedet")
        val initierendeVedtaksperiodeId: UUID,
    )

    @GraphQLName("OverstyrtMinimumSykdomsgradPeriode")
    data class ApiOverstyrtMinimumSykdomsgradPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}

@GraphQLName("Sykepengegrunnlagskjonnsfastsetting")
data class ApiSykepengegrunnlagskjonnsfastsetting(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val skjonnsfastsatt: ApiSkjonnsfastsattSykepengegrunnlag,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("SkjonnsfastsattSykepengegrunnlag")
    data class ApiSkjonnsfastsattSykepengegrunnlag(
        val arsak: String,
        val type: Skjonnsfastsettingstype?,
        val begrunnelse: String?,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val arlig: Double,
        val fraArlig: Double?,
        val skjaeringstidspunkt: LocalDate,
    )
}

@GraphQLName("Arbeidsforholdoverstyring")
data class ApiArbeidsforholdoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: LocalDate,
    val forklaring: String,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@GraphQLName("GhostPeriode")
data class ApiGhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String,
) {
    val id = UUID.nameUUIDFromBytes(fom.toString().toByteArray() + organisasjonsnummer.toByteArray()).toString()
}

@GraphQLName("NyttInntektsforholdPeriode")
data class ApiNyttInntektsforholdPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val organisasjonsnummer: String,
    val skjaeringstidspunkt: LocalDate,
    val dagligBelop: Double,
    val manedligBelop: Double,
)

@GraphQLName("Arbeidsgiver")
data class ApiArbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>,
    val ghostPerioder: List<ApiGhostPeriode>,
    val nyeInntektsforholdPerioder: List<ApiNyttInntektsforholdPeriode>,
    private val fødselsnummer: String,
    private val generasjoner: List<GraphQLGenerasjon>,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val overstyringer: List<ApiOverstyring>,
) {
    fun generasjoner(): List<ApiGenerasjon> =
        generasjoner.mapIndexed { index, generasjon ->
            val oppgaveId = oppgaveApiDao.finnOppgaveId(fødselsnummer)
            val perioderSomSkalViseAktiveVarsler = varselRepository.perioderSomSkalViseVarsler(oppgaveId)
            ApiGenerasjon(
                id = generasjon.id,
                perioder =
                    generasjon.perioder.map {
                        when (it) {
                            is GraphQLUberegnetPeriode ->
                                UberegnetPeriode(
                                    varselRepository = varselRepository,
                                    periode = it,
                                    skalViseAktiveVarsler = index == 0 && perioderSomSkalViseAktiveVarsler.contains(it.vedtaksperiodeId),
                                    notatDao = notatDao,
                                    index = index,
                                )

                            is GraphQLBeregnetPeriode ->
                                BeregnetPeriode(
                                    orgnummer = organisasjonsnummer,
                                    periode = it,
                                    apiOppgaveService = apiOppgaveService,
                                    saksbehandlerhåndterer = saksbehandlerhåndterer,
                                    risikovurderinger = risikovurderinger,
                                    varselRepository = varselRepository,
                                    oppgaveApiDao = oppgaveApiDao,
                                    periodehistorikkApiDao = periodehistorikkApiDao,
                                    notatDao = notatDao,
                                    totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                                    påVentApiDao = påVentApiDao,
                                    erSisteGenerasjon = index == 0,
                                    index = index,
                                )
                            else -> throw Exception("Ukjent tidslinjeperiode")
                        }
                    },
            )
        }

    fun overstyringer(): List<ApiOverstyring> = overstyringer

    fun arbeidsforhold(): List<ApiArbeidsforhold> =
        arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
            ApiArbeidsforhold(
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent,
                startdato = it.startdato,
                sluttdato = it.sluttdato,
            )
        }

    @Suppress("unused")
    fun inntekterFraAordningen(): List<ApiArbeidsgiverInntekterFraAOrdningen> =
        arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer,
            organisasjonsnummer,
        ).map { fraAO ->
            ApiArbeidsgiverInntekterFraAOrdningen(
                skjaeringstidspunkt = fraAO.skjaeringstidspunkt,
                inntekter =
                    fraAO.inntekter.map { inntekt ->
                        InntektFraAOrdningen(
                            maned = inntekt.maned,
                            sum = inntekt.sum,
                        )
                    },
            )
        }
}
