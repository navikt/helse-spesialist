package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.schema.GraphQLSchema
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.notat.NotatDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao

internal class SchemaBuilder(
    val personApiDao: PersonApiDao,
    val egenAnsattDao: EgenAnsattDao,
    val tildelingDao: TildelingDao,
    val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    val overstyringApiDao: OverstyringApiDao,
    val risikovurderingApiDao: RisikovurderingApiDao,
    val varselDao: VarselDao,
    val utbetalingDao: UtbetalingDao,
    val oppgaveDao: OppgaveDao,
    val periodehistorikkDao: PeriodehistorikkDao,
    val snapshotMediator: SnapshotMediator,
    val notatDao: NotatDao,
) {
    fun build(): GraphQLSchema {
        val schemaConfig = SchemaGeneratorConfig(
            supportedPackages = listOf(
                "no.nav.helse.mediator.api.graphql",
                "no.nav.helse.mediator.graphql"
            )
        )
        return toSchema(
            config = schemaConfig,
            queries = listOf(
                TopLevelObject(
                    PersonQuery(
                        personApiDao = personApiDao,
                        egenAnsattDao = egenAnsattDao,
                        tildelingDao = tildelingDao,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        overstyringApiDao = overstyringApiDao,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselDao = varselDao,
                        oppgaveDao = oppgaveDao,
                        periodehistorikkDao = periodehistorikkDao,
                        snapshotMediator = snapshotMediator,
                        notatDao = notatDao,
                    )
                ),
                TopLevelObject(
                    OppdragQuery(
                        personApiDao = personApiDao,
                        egenAnsattDao = egenAnsattDao,
                        utbetalingDao = utbetalingDao,
                    )
                )
            )
        )
    }
}
