package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.schema.GraphQLSchema
import no.nav.helse.mediator.IBehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.mutation.AnnulleringMutation
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutation
import no.nav.helse.spesialist.api.graphql.mutation.OpphevStansMutation
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutation
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutation
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutation
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutation
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutation
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutation
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQuery
import no.nav.helse.spesialist.api.graphql.query.DokumentQuery
import no.nav.helse.spesialist.api.graphql.query.NotatQuery
import no.nav.helse.spesialist.api.graphql.query.OppgaverQuery
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository

internal class SchemaBuilder(
    private val personApiDao: PersonApiDao,
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val påVentApiDao: PåVentApiDao,
    private val snapshotMediator: SnapshotMediator,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val reservasjonClient: ReservasjonClient,
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val behandlingsstatistikkMediator: IBehandlingsstatistikkMediator,
    private val notatMediator: NotatMediator,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
    private val godkjenninghåndterer: Godkjenninghåndterer,
    private val personhåndterer: Personhåndterer,
    private val dokumenthåndterer: Dokumenthåndterer,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
) {
    fun build(): GraphQLSchema {
        val schemaConfig =
            SchemaGeneratorConfig(
                supportedPackages =
                    listOf(
                        "no.nav.helse.spesialist.api.graphql",
                        "no.nav.helse.spleis.graphql",
                    ),
                hooks = schemaGeneratorHooks,
            )
        return toSchema(
            config = schemaConfig,
            queries =
                listOf(
                    TopLevelObject(
                        PersonQuery(
                            personApiDao = personApiDao,
                            egenAnsattApiDao = egenAnsattApiDao,
                            tildelingDao = tildelingDao,
                            arbeidsgiverApiDao = arbeidsgiverApiDao,
                            overstyringApiDao = overstyringApiDao,
                            risikovurderingApiDao = risikovurderingApiDao,
                            varselRepository = varselRepository,
                            oppgaveApiDao = oppgaveApiDao,
                            periodehistorikkDao = periodehistorikkDao,
                            notatDao = notatDao,
                            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                            påVentApiDao = påVentApiDao,
                            snapshotMediator = snapshotMediator,
                            reservasjonClient = reservasjonClient,
                            oppgavehåndterer = oppgavehåndterer,
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            avviksvurderinghenter = avviksvurderinghenter,
                            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                        ),
                    ),
                    TopLevelObject(
                        OppgaverQuery(
                            oppgavehåndterer = oppgavehåndterer,
                        ),
                    ),
                    TopLevelObject(
                        BehandlingsstatistikkQuery(
                            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                        ),
                    ),
                    TopLevelObject(
                        NotatQuery(notatDao = notatDao),
                    ),
                    TopLevelObject(
                        OpptegnelseQuery(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                        ),
                    ),
                    TopLevelObject(
                        DokumentQuery(
                            personApiDao = personApiDao,
                            egenAnsattApiDao = egenAnsattApiDao,
                            dokumenthåndterer = dokumenthåndterer,
                        ),
                    ),
                ),
            mutations =
                listOf(
                    TopLevelObject(
                        NotatMutation(notatDao = notatDao),
                    ),
                    TopLevelObject(
                        VarselMutation(varselRepository = varselRepository),
                    ),
                    TopLevelObject(
                        TildelingMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    ),
                    TopLevelObject(
                        OpptegnelseMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    ),
                    TopLevelObject(
                        OverstyringMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    ),
                    TopLevelObject(
                        SkjonnsfastsettelseMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    ),
                    TopLevelObject(
                        TotrinnsvurderingMutation(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            oppgavehåndterer = oppgavehåndterer,
                            totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
                        ),
                    ),
                    TopLevelObject(
                        VedtakMutation(
                            oppgavehåndterer = oppgavehåndterer,
                            totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            godkjenninghåndterer = godkjenninghåndterer,
                        ),
                    ),
                    TopLevelObject(
                        PersonMutation(
                            personhåndterer = personhåndterer,
                        ),
                    ),
                    TopLevelObject(
                        AnnulleringMutation(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                        ),
                    ),
                    TopLevelObject(
                        PaVentMutation(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            notatMediator = notatMediator,
                            periodehistorikkDao = periodehistorikkDao,
                        ),
                    ),
                    TopLevelObject(
                        OpphevStansMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    ),
                ),
        )
    }
}
