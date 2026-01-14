package no.nav.helse.spesialist.api.graphql.resolvers

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.schema.ApiBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiGenerasjon
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.SelvstendigNaeringSchema
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotGenerasjon
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import java.util.UUID

class ApiSelvstendigNaeringResolver(
    private val fødselsnummer: String,
    private val generasjoner: List<SnapshotGenerasjon>,
    private val overstyringer: List<ApiOverstyring>,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val sessionFactory: SessionFactory,
    private val annulleringRepository: AnnulleringRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
) : SelvstendigNaeringSchema {
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
                                            orgnummer = "SELVSTENDIG",
                                            periode = it,
                                            apiOppgaveService = apiOppgaveService,
                                            saksbehandlerMediator = saksbehandlerMediator,
                                            risikovurderinger = risikovurderinger,
                                            varselRepository = varselRepository,
                                            oppgaveApiDao = oppgaveApiDao,
                                            periodehistorikkApiDao = periodehistorikkApiDao,
                                            notatDao = notatDao,
                                            påVentApiDao = påVentApiDao,
                                            erSisteBehandling = index == 0,
                                            index = index,
                                            vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                                            sessionFactory = sessionFactory,
                                            annulleringRepository = annulleringRepository,
                                            saksbehandlerRepository = saksbehandlerRepository,
                                        ),
                                )

                            else -> throw Exception("Ukjent tidslinjeperiode")
                        }
                    },
            )
        }

    override fun overstyringer(): List<ApiOverstyring> = overstyringer
}
