package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.Daos
import no.nav.helse.db.DefinisjonDao
import no.nav.helse.db.DokumentDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MeldingDuplikatkontrollDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PartialReservasjonDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.VarselDao
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase
import no.nav.helse.db.VedtakDao
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
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VedtaksperiodeDbDto
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.spesialist.api.notat.KommentarDto
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDto
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InMemoryDaos(
    override val oppgaveRepository: InMemoryOppgaveRepository,
    override val notatDao: NotatDao,
    override val oppgaveDao: OppgaveDao,
    override val dialogDao: InMemoryDialogDao,
    override val stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
) : Daos {
    override val behandlingsstatistikkDao: BehandlingsstatistikkDao = object : BehandlingsstatistikkDao {
        override fun getAntallTilgjengeligeBeslutteroppgaver(): Int {
            TODO("Not yet implemented")
        }

        override fun getAntallTilgjengeligeEgenAnsattOppgaver(): Int {
            TODO("Not yet implemented")
        }

        override fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate): Int {
            TODO("Not yet implemented")
        }

        override fun getAntallFullførteBeslutteroppgaver(fom: LocalDate): Int {
            TODO("Not yet implemented")
        }

        override fun getAutomatiseringPerKombinasjon(fom: LocalDate): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
            TODO("Not yet implemented")
        }

        override fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
            TODO("Not yet implemented")
        }

        override fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
            TODO("Not yet implemented")
        }

        override fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase): Int {
            TODO("Not yet implemented")
        }

        override fun antallFerdigstilteOppgaverFor(
            egenskap: EgenskapForDatabase,
            fom: LocalDate
        ): Int {
            TODO("Not yet implemented")
        }

        override fun getAntallAnnulleringer(fom: LocalDate): Int {
            TODO("Not yet implemented")
        }

        override fun getAntallAvvisninger(fom: LocalDate): Int {
            TODO("Not yet implemented")
        }
    }
    override val commandContextDao = object : CommandContextDao {
        override fun nyContext(meldingId: UUID): CommandContext {
            TODO("Not yet implemented")
        }

        override fun opprett(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun ferdig(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun suspendert(
            hendelseId: UUID,
            contextId: UUID,
            hash: UUID,
            sti: List<Int>
        ) {
            TODO("Not yet implemented")
        }

        override fun feil(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun tidsbrukForContext(contextId: UUID): Int {
            TODO("Not yet implemented")
        }

        override fun avbryt(
            vedtaksperiodeId: UUID,
            contextId: UUID
        ): List<Pair<UUID, UUID>> {
            TODO("Not yet implemented")
        }

        override fun finnSuspendert(contextId: UUID): CommandContext? {
            TODO("Not yet implemented")
        }

        override fun finnSuspendertEllerFeil(contextId: UUID): CommandContext? {
            TODO("Not yet implemented")
        }

    }
    override val definisjonDao = object : DefinisjonDao {
        override fun definisjonFor(definisjonId: UUID): Varseldefinisjon {
            TODO("Not yet implemented")
        }

        override fun sisteDefinisjonFor(varselkode: String): Varseldefinisjon {
            TODO("Not yet implemented")
        }

        override fun lagreDefinisjon(
            unikId: UUID,
            kode: String,
            tittel: String,
            forklaring: String?,
            handling: String?,
            avviklet: Boolean,
            opprettet: LocalDateTime
        ) {
            TODO("Not yet implemented")
        }
    }
    override val dokumentDao = object : DokumentDao {
        override fun lagre(
            fødselsnummer: String,
            dokumentId: UUID,
            dokument: JsonNode
        ) {
            TODO("Not yet implemented")
        }

        override fun hent(
            fødselsnummer: String,
            dokumentId: UUID
        ): JsonNode? {
            TODO("Not yet implemented")
        }

        override fun slettGamleDokumenter(): Int {
            TODO("Not yet implemented")
        }
    }
    override val egenAnsattDao: EgenAnsattDao
        get() = TODO("Not yet implemented")
    override val generasjonDao = object : GenerasjonDao {
        override fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? {
            TODO("Not yet implemented")
        }

        override fun finnGenerasjoner(vedtaksperiodeId: UUID): List<BehandlingDto> {
            TODO("Not yet implemented")
        }

        override fun lagreGenerasjon(behandlingDto: BehandlingDto) {
            TODO("Not yet implemented")
        }

        override fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
            TODO("Not yet implemented")
        }

        override fun førsteKjenteDag(fødselsnummer: String): LocalDate? {
            TODO("Not yet implemented")
        }
    }

    override val meldingDao: InMemoryMeldingDao = InMemoryMeldingDao()
    override val meldingDuplikatkontrollDao = object : MeldingDuplikatkontrollDao {
        override fun lagre(meldingId: UUID, type: String) {}

        override fun erBehandlet(meldingId: UUID) = false
    }
    override val opptegnelseDao = object : OpptegnelseDao {
        override fun opprettOpptegnelse(
            fødselsnummer: String,
            payload: String,
            type: OpptegnelseDao.Opptegnelse.Type
        ) {
            TODO("Not yet implemented")
        }

        override fun finnOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDao.Opptegnelse> {
            TODO("Not yet implemented")
        }
    }
    override val periodehistorikkDao: PeriodehistorikkDao = object : PeriodehistorikkDao {
        override fun lagreMedOppgaveId(
            historikkinnslag: Historikkinnslag,
            oppgaveId: Long
        ) {
            TODO("Not yet implemented")
        }

        override fun lagre(
            historikkinnslag: Historikkinnslag,
            generasjonId: UUID
        ) {
            TODO("Not yet implemented")
        }
    }
    override val personDao: PersonDao = object : PersonDao {
        override fun personKlargjort(fødselsnummer: String) {
            TODO("Not yet implemented")
        }

        override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? {
            TODO("Not yet implemented")
        }

        override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
            TODO("Not yet implemented")
        }

        override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate? {
            TODO("Not yet implemented")
        }

        override fun oppdaterEnhet(fødselsnummer: String, enhetNr: Int): Int {
            TODO("Not yet implemented")
        }

        override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate? {
            TODO("Not yet implemented")
        }

        override fun upsertInfotrygdutbetalinger(
            fødselsnummer: String,
            utbetalinger: JsonNode
        ): Long {
            TODO("Not yet implemented")
        }

        override fun upsertPersoninfo(
            fødselsnummer: String,
            fornavn: String,
            mellomnavn: String?,
            etternavn: String,
            fødselsdato: LocalDate,
            kjønn: Kjønn,
            adressebeskyttelse: Adressebeskyttelse
        ) {
            TODO("Not yet implemented")
        }

        override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate? {
            TODO("Not yet implemented")
        }

        override fun finnInntekter(
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate
        ): List<Inntekter>? {
            TODO("Not yet implemented")
        }

        override fun lagreInntekter(
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            inntekter: List<Inntekter>
        ): Long? {
            TODO("Not yet implemented")
        }

        override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? {
            TODO("Not yet implemented")
        }

        override fun finnPersoninfoRef(fødselsnummer: String): Long? {
            TODO("Not yet implemented")
        }

        override fun finnEnhetId(fødselsnummer: String): String {
            TODO("Not yet implemented")
        }

        override fun finnAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? {
            TODO("Not yet implemented")
        }

        override fun finnAktørId(fødselsnummer: String): String? {
            TODO("Not yet implemented")
        }

        override fun insertPerson(
            fødselsnummer: String,
            aktørId: String,
            personinfoId: Long,
            enhetId: Int,
            infotrygdutbetalingerId: Long
        ): Long {
            TODO("Not yet implemented")
        }

    }
    override val poisonPillDao = object : PoisonPillDao {
        override fun poisonPills() = PoisonPills(emptyMap())
    }
    override val påVentDao: PåVentDao = object : PåVentDao {
        override fun erPåVent(vedtaksperiodeId: UUID): Boolean {
            TODO("Not yet implemented")
        }

        override fun lagrePåVent(
            oppgaveId: Long,
            saksbehandlerOid: UUID,
            frist: LocalDate?,
            årsaker: List<PåVentÅrsak>,
            notatTekst: String?,
            dialogRef: Long
        ) {
            TODO("Not yet implemented")
        }

        override fun slettPåVent(oppgaveId: Long): Int? {
            TODO("Not yet implemented")
        }

        override fun erPåVent(oppgaveId: Long): Boolean {
            TODO("Not yet implemented")
        }

        override fun oppdaterPåVent(
            oppgaveId: Long,
            saksbehandlerOid: UUID,
            frist: LocalDate?,
            årsaker: List<PåVentÅrsak>,
            notatTekst: String?,
            dialogRef: Long
        ) {
            TODO("Not yet implemented")
        }
    }
    override val reservasjonDao = object : PartialReservasjonDao {}
    override val saksbehandlerDao: InMemorySaksbehandlerDao = InMemorySaksbehandlerDao(saksbehandlerRepository)
    override val tildelingDao: TildelingDao
        get() = TODO("Not yet implemented")
    override val varselDao = object : VarselDao {
        override fun avvikleVarsel(varselkode: String, definisjonId: UUID) {
            TODO("Not yet implemented")
        }
    }
    override val vedtakDao: VedtakDao
        get() = TODO("Not yet implemented")
    override val vedtakBegrunnelseDao: VedtakBegrunnelseDao = object : VedtakBegrunnelseDao {
        override fun invaliderVedtakBegrunnelse(oppgaveId: Long): Int {
            TODO("Not yet implemented")
        }

        override fun finnVedtakBegrunnelse(
            vedtaksperiodeId: UUID,
            generasjonId: Long
        ): VedtakBegrunnelse? {
            TODO("Not yet implemented")
        }

        override fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase? {
            TODO("Not yet implemented")
        }

        override fun finnAlleVedtakBegrunnelser(
            vedtaksperiodeId: UUID,
            utbetalingId: UUID
        ): List<VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase> {
            TODO("Not yet implemented")
        }

        override fun lagreVedtakBegrunnelse(
            oppgaveId: Long,
            vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
            saksbehandlerOid: UUID
        ): Int {
            TODO("Not yet implemented")
        }
    }
    override val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao =
        object : StansAutomatiskBehandlingSaksbehandlerDao {
            override fun lagreStans(fødselsnummer: String) {
                TODO("Not yet implemented")
            }

            override fun opphevStans(fødselsnummer: String) {
                TODO("Not yet implemented")
            }

            override fun erStanset(fødselsnummer: String): Boolean {
                TODO("Not yet implemented")
            }
        }
    override val abonnementApiDao: AbonnementApiDao = object : AbonnementApiDao {
        override fun opprettAbonnement(saksbehandlerId: UUID, aktørId: String) {
            TODO("Not yet implemented")
        }

        override fun registrerSistekvensnummer(
            saksbehandlerIdent: UUID,
            sisteSekvensId: Int
        ): Int {
            TODO("Not yet implemented")
        }
    }
    override val arbeidsgiverApiDao: ArbeidsgiverApiDao = object : ArbeidsgiverApiDao {
        override fun finnArbeidsforhold(
            fødselsnummer: String,
            arbeidsgiverIdentifikator: String
        ): List<ArbeidsforholdApiDto> {
            TODO("Not yet implemented")
        }

        override fun finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer: String,
            orgnummer: String
        ): List<ArbeidsgiverApiDao.ArbeidsgiverInntekterFraAOrdningen> {
            TODO("Not yet implemented")
        }
    }
    override val egenAnsattApiDao: EgenAnsattApiDao = object : EgenAnsattApiDao {
        override fun erEgenAnsatt(fødselsnummer: String): Boolean? {
            TODO("Not yet implemented")
        }
    }
    override val behandlingApiRepository: BehandlingApiRepository = object : BehandlingApiRepository {
        override fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto> {
            TODO("Not yet implemented")
        }

        override fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto {
            TODO("Not yet implemented")
        }
    }
    override val notatApiDao: NotatApiDao = object : NotatApiDao {
        override fun finnNotater(vedtaksperiodeId: UUID): List<NotatApiDao.NotatDto> {
            TODO("Not yet implemented")
        }

        override fun finnKommentarer(dialogRef: Long): List<KommentarDto> {
            TODO("Not yet implemented")
        }
    }
    override val oppgaveApiDao: OppgaveApiDao = object : OppgaveApiDao {
        override fun finnOppgaveId(fødselsnummer: String): Long? {
            TODO("Not yet implemented")
        }

        override fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? {
            TODO("Not yet implemented")
        }

        override fun finnFødselsnummer(oppgaveId: Long): String {
            TODO("Not yet implemented")
        }
    }
    override val overstyringApiDao: OverstyringApiDao = object : OverstyringApiDao {
        override fun finnOverstyringer(fødselsnummer: String): List<OverstyringDto> {
            TODO("Not yet implemented")
        }
    }
    override val periodehistorikkApiDao: PeriodehistorikkApiDao = object : PeriodehistorikkApiDao {
        override fun finn(utbetalingId: UUID): List<PeriodehistorikkDto> {
            TODO("Not yet implemented")
        }
    }
    override val personApiDao: PersonApiDao = object : PersonApiDao {
        override fun personKlargjøres(fødselsnummer: String) {
            TODO("Not yet implemented")
        }

        override fun klargjøringPågår(fødselsnummer: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun finnEnhet(fødselsnummer: String): EnhetDto {
            TODO("Not yet implemented")
        }

        override fun finnInfotrygdutbetalinger(fødselsnummer: String): String? {
            TODO("Not yet implemented")
        }

        override fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun hentAdressebeskyttelse(fødselsnummer: String): no.nav.helse.spesialist.api.person.Adressebeskyttelse? {
            TODO("Not yet implemented")
        }

        override fun finnAktørId(fødselsnummer: String): String {
            TODO("Not yet implemented")
        }

        override fun finnFødselsnumre(aktørId: String): List<String> {
            TODO("Not yet implemented")
        }

        override fun harDataNødvendigForVisning(fødselsnummer: String): Boolean {
            TODO("Not yet implemented")
        }
    }
    override val påVentApiDao: PåVentApiDao = object : PåVentApiDao {
        override fun hentAktivPåVent(vedtaksperiodeId: UUID): PåVentApiDao.PaVentDto? {
            TODO("Not yet implemented")
        }
    }
    override val risikovurderingApiDao: RisikovurderingApiDao = object : RisikovurderingApiDao {
        override fun finnRisikovurderinger(fødselsnummer: String): Map<UUID, RisikovurderingApiDto> {
            TODO("Not yet implemented")
        }
    }
    override val personinfoDao: PersoninfoDao = object : PersoninfoDao {
        override fun hentPersoninfo(fødselsnummer: String): PersoninfoDao.Personinfo? {
            TODO("Not yet implemented")
        }
    }
    override val tildelingApiDao: TildelingApiDao = object : TildelingApiDao {
        override fun tildelingForPerson(fødselsnummer: String): TildelingApiDto? {
            TODO("Not yet implemented")
        }
    }
    override val varselApiRepository: VarselApiRepository = object : VarselApiRepository {
        override fun finnVarslerSomIkkeErInaktiveFor(
            vedtaksperiodeId: UUID,
            utbetalingId: UUID
        ): Set<VarselDbDto> {
            TODO("Not yet implemented")
        }

        override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
            vedtaksperiodeId: UUID,
            utbetalingId: UUID
        ): Set<VarselDbDto> {
            TODO("Not yet implemented")
        }

        override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
            TODO("Not yet implemented")
        }

        override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
            TODO("Not yet implemented")
        }

        override fun godkjennVarslerFor(oppgaveId: Long) {
            TODO("Not yet implemented")
        }

        override fun vurderVarselFor(
            varselId: UUID,
            gjeldendeStatus: VarselDbDto.Varselstatus,
            saksbehandlerIdent: String
        ) {
            TODO("Not yet implemented")
        }

        override fun erAktiv(varselkode: String, generasjonId: UUID): Boolean? {
            TODO("Not yet implemented")
        }

        override fun erGodkjent(varselkode: String, generasjonId: UUID): Boolean? {
            TODO("Not yet implemented")
        }

        override fun settStatusVurdert(
            generasjonId: UUID,
            definisjonId: UUID,
            varselkode: String,
            ident: String
        ): VarselDbDto? {
            TODO("Not yet implemented")
        }

        override fun settStatusAktiv(
            generasjonId: UUID,
            varselkode: String,
            ident: String
        ): VarselDbDto? {
            TODO("Not yet implemented")
        }

        override fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID> {
            TODO("Not yet implemented")
        }
    }
    override val vergemålApiDao: VergemålApiDao = object : VergemålApiDao {
        override fun harFullmakt(fødselsnummer: String): Boolean? {
            TODO("Not yet implemented")
        }
    }
}
