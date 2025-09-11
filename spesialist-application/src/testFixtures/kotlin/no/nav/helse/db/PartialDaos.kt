package no.nav.helse.db

import no.nav.helse.db.api.AbonnementApiDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.BehandlingApiRepository
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PersoninfoDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.oppgave.OppgaveRepository

interface PartialDaos : Daos {
    override val annulleringRepository: AnnulleringRepository
        get() = error("Not implemented for this test")
    override val behandlingsstatistikkDao: BehandlingsstatistikkDao
        get() = error("Not implemented for this test")
    override val commandContextDao: CommandContextDao
        get() = error("Not implemented for this test")
    override val definisjonDao: DefinisjonDao
        get() = error("Not implemented for this test")
    override val dialogDao: DialogDao
        get() = error("Not implemented for this test")
    override val dokumentDao: DokumentDao
        get() = error("Not implemented for this test")
    override val egenAnsattDao: EgenAnsattDao
        get() = error("Not implemented for this test")
    override val generasjonDao: GenerasjonDao
        get() = error("Not implemented for this test")
    override val meldingDao: MeldingDao
        get() = error("Not implemented for this test")
    override val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao
        get() = error("Not implemented for this test")
    override val notatDao: NotatDao
        get() = error("Not implemented for this test")
    override val oppgaveDao: OppgaveDao
        get() = error("Not implemented for this test")
    override val opptegnelseDao: OpptegnelseDao
        get() = error("Not implemented for this test")
    override val periodehistorikkDao: PeriodehistorikkDao
        get() = error("Not implemented for this test")
    override val personDao: PersonDao
        get() = error("Not implemented for this test")
    override val poisonPillDao: PoisonPillDao
        get() = error("Not implemented for this test")
    override val påVentDao: PåVentDao
        get() = error("Not implemented for this test")
    override val reservasjonDao: ReservasjonDao
        get() = error("Not implemented for this test")
    override val saksbehandlerDao: SaksbehandlerDao
        get() = error("Not implemented for this test")
    override val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
        get() = error("Not implemented for this test")
    override val tildelingDao: TildelingDao
        get() = error("Not implemented for this test")
    override val varselDao: VarselDao
        get() = error("Not implemented for this test")
    override val vedtakDao: VedtakDao
        get() = error("Not implemented for this test")
    override val vedtakBegrunnelseDao: VedtakBegrunnelseDao
        get() = error("Not implemented for this test")
    override val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao
        get() = error("Not implemented for this test")
    override val abonnementApiDao: AbonnementApiDao
        get() = error("Not implemented for this test")
    override val arbeidsgiverApiDao: ArbeidsgiverApiDao
        get() = error("Not implemented for this test")
    override val egenAnsattApiDao: EgenAnsattApiDao
        get() = error("Not implemented for this test")
    override val behandlingApiRepository: BehandlingApiRepository
        get() = error("Not implemented for this test")
    override val notatApiDao: NotatApiDao
        get() = error("Not implemented for this test")
    override val oppgaveApiDao: OppgaveApiDao
        get() = error("Not implemented for this test")
    override val overstyringApiDao: OverstyringApiDao
        get() = error("Not implemented for this test")
    override val periodehistorikkApiDao: PeriodehistorikkApiDao
        get() = error("Not implemented for this test")
    override val personApiDao: PersonApiDao
        get() = error("Not implemented for this test")
    override val påVentApiDao: PåVentApiDao
        get() = error("Not implemented for this test")
    override val risikovurderingApiDao: RisikovurderingApiDao
        get() = error("Not implemented for this test")
    override val personinfoDao: PersoninfoDao
        get() = error("Not implemented for this test")
    override val tildelingApiDao: TildelingApiDao
        get() = error("Not implemented for this test")
    override val varselApiRepository: VarselApiRepository
        get() = error("Not implemented for this test")
    override val vergemålApiDao: VergemålApiDao
        get() = error("Not implemented for this test")
    override val oppgaveRepository: OppgaveRepository
        get() = error("Not implemented for this test")
}
