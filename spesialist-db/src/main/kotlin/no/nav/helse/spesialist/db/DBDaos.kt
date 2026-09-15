package no.nav.helse.spesialist.db

import no.nav.helse.db.Daos
import no.nav.helse.spesialist.db.dao.*
import no.nav.helse.spesialist.db.dao.api.*
import no.nav.helse.spesialist.db.repository.PgOppgaveRepository
import no.nav.helse.spesialist.db.repository.PgSaksbehandlerRepository
import javax.sql.DataSource

class DBDaos(
    dataSource: DataSource,
) : Daos {
    override val annulleringRepository = PgAnnulleringRepository(dataSource)
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val dialogDao = PgDialogDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val legacyBehandlingDao = PgLegacyBehandlingDao(dataSource)
    override val meldingDao = PgMeldingDao(dataSource)
    override val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    override val notatDao = PgNotatDao(dataSource)
    override val oppgaveDao = PgOppgaveDao(dataSource)
    override val periodehistorikkDao = PgPeriodehistorikkDao(dataSource)
    override val personDao = PgPersonDao(dataSource)
    override val poisonPillDao = PgPoisonPillDao(dataSource)
    override val påVentDao = PgPåVentDao(dataSource)
    override val reservasjonDao = PgReservasjonDao(dataSource)
    override val saksbehandlerDao = PgSaksbehandlerDao(dataSource)
    override val tildelingDao = PgTildelingDao(dataSource)
    override val vedtakDao = PgVedtakDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)

    override val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    override val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    override val overstyringApiDao = PgOverstyringApiDao(dataSource)
    override val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    override val påVentApiDao = PgPåVentApiDao(dataSource)
    override val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    override val tildelingApiDao = PgTildelingApiDao(dataSource)
    override val varselApiRepository = PgVarselApiRepository(dataSource)
    override val oppgaveRepository = PgOppgaveRepository(dataSource)
    override val saksbehandlerRepository = PgSaksbehandlerRepository(dataSource)
}
