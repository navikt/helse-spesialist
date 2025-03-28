package no.nav.helse.spesialist.api.graphql.resolvers

import no.nav.helse.FeatureToggles
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiGenerasjon
import no.nav.helse.spesialist.api.graphql.schema.ApiGhostPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiNyttInntektsforholdPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsgiverSchema
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotGenerasjon
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import java.util.UUID

class ApiArbeidsgiverResolver(
    private val organisasjonsnummer: String,
    private val navn: String,
    private val bransjer: List<String>,
    private val ghostPerioder: List<ApiGhostPeriode>,
    private val nyeInntektsforholdPerioder: List<ApiNyttInntektsforholdPeriode>,
    private val fødselsnummer: String,
    private val generasjoner: List<SnapshotGenerasjon>,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val overstyringer: List<ApiOverstyring>,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val sessionFactory: SessionFactory,
    private val featureToggles: FeatureToggles,
) : ArbeidsgiverSchema {
    override fun organisasjonsnummer(): String = organisasjonsnummer

    override fun navn(): String = navn

    override fun bransjer(): List<String> = bransjer

    override fun ghostPerioder(): List<ApiGhostPeriode> = ghostPerioder

    override fun nyeInntektsforholdPerioder(): List<ApiNyttInntektsforholdPeriode> = nyeInntektsforholdPerioder

    override fun generasjoner(): List<ApiGenerasjon> =
        generasjoner.mapIndexed { index, generasjon ->
            val oppgaveId = oppgaveApiDao.finnOppgaveId(fødselsnummer)
            val perioderSomSkalViseAktiveVarsler = varselRepository.perioderSomSkalViseVarsler(oppgaveId)
            ApiGenerasjon(
                id = generasjon.id,
                perioder =
                    generasjon.perioder.map {
                        when (it) {
                            is SnapshotUberegnetPeriode ->
                                ApiUberegnetPeriode(
                                    resolver =
                                        ApiUberegnetPeriodeResolver(
                                            varselRepository = varselRepository,
                                            periode = it,
                                            skalViseAktiveVarsler =
                                                index == 0 &&
                                                    perioderSomSkalViseAktiveVarsler.contains(
                                                        it.vedtaksperiodeId,
                                                    ),
                                            notatDao = notatDao,
                                            index = index,
                                        ),
                                )

                            is SnapshotBeregnetPeriode ->
                                ApiBeregnetPeriode(
                                    resolver =
                                        ApiBeregnetPeriodeResolver(
                                            fødselsnummer = fødselsnummer,
                                            orgnummer = organisasjonsnummer,
                                            periode = it,
                                            apiOppgaveService = apiOppgaveService,
                                            saksbehandlerMediator = saksbehandlerMediator,
                                            risikovurderinger = risikovurderinger,
                                            varselRepository = varselRepository,
                                            oppgaveApiDao = oppgaveApiDao,
                                            periodehistorikkApiDao = periodehistorikkApiDao,
                                            notatDao = notatDao,
                                            påVentApiDao = påVentApiDao,
                                            erSisteGenerasjon = index == 0,
                                            index = index,
                                            vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                                            sessionFactory = sessionFactory,
                                            featureToggles = featureToggles,
                                        ),
                                )

                            else -> throw Exception("Ukjent tidslinjeperiode")
                        }
                    },
            )
        }

    override fun overstyringer(): List<ApiOverstyring> = overstyringer

    override fun arbeidsforhold(): List<ApiArbeidsforhold> =
        arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
            ApiArbeidsforhold(
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent,
                startdato = it.startdato,
                sluttdato = it.sluttdato,
            )
        }

    @Suppress("unused")
    override fun inntekterFraAordningen(): List<ApiArbeidsgiverInntekterFraAOrdningen> =
        arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer,
            organisasjonsnummer,
        ).map { fraAO ->
            ApiArbeidsgiverInntekterFraAOrdningen(
                skjaeringstidspunkt = fraAO.skjaeringstidspunkt,
                inntekter =
                    fraAO.inntekter.map { inntekt ->
                        ApiInntektFraAOrdningen(
                            maned = inntekt.maned,
                            sum = inntekt.sum,
                        )
                    },
            )
        }
}
