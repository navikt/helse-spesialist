package no.nav.helse.spesialist.db

import no.nav.helse.db.Daos
import no.nav.helse.spesialist.db.dao.PgAnnulleringRepository
import no.nav.helse.spesialist.db.dao.PgBehandlingsstatistikkDao
import no.nav.helse.spesialist.db.dao.PgCommandContextDao
import no.nav.helse.spesialist.db.dao.PgDefinisjonDao
import no.nav.helse.spesialist.db.dao.PgDialogDao
import no.nav.helse.spesialist.db.dao.PgDokumentDao
import no.nav.helse.spesialist.db.dao.PgEgenAnsattDao
import no.nav.helse.spesialist.db.dao.PgLegacyBehandlingDao
import no.nav.helse.spesialist.db.dao.PgLegacyVarselDao
import no.nav.helse.spesialist.db.dao.PgMeldingDao
import no.nav.helse.spesialist.db.dao.PgMeldingDuplikatkontrollDao
import no.nav.helse.spesialist.db.dao.PgNotatDao
import no.nav.helse.spesialist.db.dao.PgOppgaveDao
import no.nav.helse.spesialist.db.dao.PgPeriodehistorikkDao
import no.nav.helse.spesialist.db.dao.PgPersonDao
import no.nav.helse.spesialist.db.dao.PgPoisonPillDao
import no.nav.helse.spesialist.db.dao.PgPåVentDao
import no.nav.helse.spesialist.db.dao.PgReservasjonDao
import no.nav.helse.spesialist.db.dao.PgSaksbehandlerDao
import no.nav.helse.spesialist.db.dao.PgStansAutomatiskBehandlingDao
import no.nav.helse.spesialist.db.dao.PgStansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.spesialist.db.dao.PgTildelingDao
import no.nav.helse.spesialist.db.dao.PgVedtakBegrunnelseDao
import no.nav.helse.spesialist.db.dao.PgVedtakDao
import no.nav.helse.spesialist.db.dao.api.PgArbeidsgiverApiDao
import no.nav.helse.spesialist.db.dao.api.PgBehandlingApiRepository
import no.nav.helse.spesialist.db.dao.api.PgEgenAnsattApiDao
import no.nav.helse.spesialist.db.dao.api.PgOppgaveApiDao
import no.nav.helse.spesialist.db.dao.api.PgOverstyringApiDao
import no.nav.helse.spesialist.db.dao.api.PgPeriodehistorikkApiDao
import no.nav.helse.spesialist.db.dao.api.PgPersonApiDao
import no.nav.helse.spesialist.db.dao.api.PgPåVentApiDao
import no.nav.helse.spesialist.db.dao.api.PgRisikovurderingApiDao
import no.nav.helse.spesialist.db.dao.api.PgTildelingApiDao
import no.nav.helse.spesialist.db.dao.api.PgVarselApiRepository
import no.nav.helse.spesialist.db.dao.api.PgVergemålApiDao
import no.nav.helse.spesialist.db.repository.PgOppgaveRepository
import no.nav.helse.spesialist.db.repository.PgSaksbehandlerRepository
import javax.sql.DataSource

class DBDaos(
    dataSource: DataSource,
) : Daos {
    override val annulleringRepository = PgAnnulleringRepository(dataSource)
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val definisjonDao = PgDefinisjonDao(dataSource)
    override val dialogDao = PgDialogDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val egenAnsattDao = PgEgenAnsattDao(dataSource)
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
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(dataSource)
    override val tildelingDao = PgTildelingDao(dataSource)
    override val legacyVarselDao = PgLegacyVarselDao(dataSource)
    override val vedtakDao = PgVedtakDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)
    override val stansAutomatiskBehandlingSaksbehandlerDao = PgStansAutomatiskBehandlingSaksbehandlerDao(dataSource)

    override val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    override val egenAnsattApiDao = PgEgenAnsattApiDao(dataSource)
    override val behandlingApiRepository = PgBehandlingApiRepository(dataSource)
    override val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    override val overstyringApiDao = PgOverstyringApiDao(dataSource)
    override val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    override val personApiDao = PgPersonApiDao(dataSource)
    override val påVentApiDao = PgPåVentApiDao(dataSource)
    override val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    override val tildelingApiDao = PgTildelingApiDao(dataSource)
    override val varselApiRepository = PgVarselApiRepository(dataSource)
    override val vergemålApiDao = PgVergemålApiDao(dataSource)
    override val oppgaveRepository = PgOppgaveRepository(dataSource)
    override val saksbehandlerRepository = PgSaksbehandlerRepository(dataSource)
}
