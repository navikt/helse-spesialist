package no.nav.helse.spesialist.api.graphql.resolvers

import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
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
                                            skalViseAktiveVarsler = index == 0 && perioderSomSkalViseAktiveVarsler.contains(it.vedtaksperiodeId),
                                            notatDao = notatDao,
                                            index = index,
                                        ),
                                )

                            is SnapshotBeregnetPeriode ->
                                ApiBeregnetPeriode(
                                    resolver =
                                        ApiBeregnetPeriodeResolver(
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
