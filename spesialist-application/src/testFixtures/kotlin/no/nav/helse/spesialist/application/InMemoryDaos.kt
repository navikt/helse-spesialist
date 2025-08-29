package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.AnnullertAvSaksbehandlerRow
import no.nav.helse.db.BehandlingISykefraværstilfelleRow
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.Daos
import no.nav.helse.db.DefinisjonDao
import no.nav.helse.db.DialogDao
import no.nav.helse.db.DokumentDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MeldingDuplikatkontrollDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.VarselDao
import no.nav.helse.db.VedtakBegrunnelseDao
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
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InMemoryDaos : Daos {
    override val annulleringDao: AnnulleringDao = object : AnnulleringDao {
        override fun find10Annulleringer(): List<AnnullertAvSaksbehandlerRow> {
            TODO("Not yet implemented")
        }

        override fun findUtbetalingId(
            arbeidsgiverFagsystemId: String,
            personFagsystemId: String
        ): UUID? {
            TODO("Not yet implemented")
        }

        override fun finnBehandlingISykefraværstilfelle(utbetalingId: UUID): BehandlingISykefraværstilfelleRow? {
            TODO("Not yet implemented")
        }

        override fun finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandlingISykefraværstilfelleRow: BehandlingISykefraværstilfelleRow): UUID? {
            TODO("Not yet implemented")
        }

        override fun oppdaterAnnulleringMedVedtaksperiodeId(
            annulleringId: Int,
            vedtaksperiodeId: UUID
        ): Int {
            TODO("Not yet implemented")
        }

    }
    override val annulleringRepository: AnnulleringRepository
        get() = TODO("Not yet implemented")
    override val behandlingsstatistikkDao: BehandlingsstatistikkDao
        get() = TODO("Not yet implemented")
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
    override val dialogDao: DialogDao
        get() = TODO("Not yet implemented")
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
    override val notatDao: NotatDao
        get() = TODO("Not yet implemented")
    override val oppgaveDao: OppgaveDao
        get() = TODO("Not yet implemented")
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
    override val periodehistorikkDao: PeriodehistorikkDao
        get() = TODO("Not yet implemented")
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
    override val påVentDao: PåVentDao
        get() = TODO("Not yet implemented")
    override val reservasjonDao: ReservasjonDao
        get() = TODO("Not yet implemented")
    override val saksbehandlerDao: SaksbehandlerDao
        get() = TODO("Not yet implemented")
    override val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
        get() = TODO("Not yet implemented")
    override val tildelingDao: TildelingDao
        get() = TODO("Not yet implemented")
    override val varselDao = object : VarselDao {
        override fun avvikleVarsel(varselkode: String, definisjonId: UUID) {
            TODO("Not yet implemented")
        }
    }
    override val vedtakDao: VedtakDao
        get() = TODO("Not yet implemented")
    override val vedtakBegrunnelseDao: VedtakBegrunnelseDao
        get() = TODO("Not yet implemented")
    override val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao
        get() = TODO("Not yet implemented")
    override val abonnementApiDao: AbonnementApiDao
        get() = TODO("Not yet implemented")
    override val arbeidsgiverApiDao: ArbeidsgiverApiDao
        get() = TODO("Not yet implemented")
    override val egenAnsattApiDao: EgenAnsattApiDao
        get() = TODO("Not yet implemented")
    override val behandlingApiRepository: BehandlingApiRepository
        get() = TODO("Not yet implemented")
    override val notatApiDao: NotatApiDao
        get() = TODO("Not yet implemented")
    override val oppgaveApiDao: OppgaveApiDao
        get() = TODO("Not yet implemented")
    override val overstyringApiDao: OverstyringApiDao
        get() = TODO("Not yet implemented")
    override val periodehistorikkApiDao: PeriodehistorikkApiDao
        get() = TODO("Not yet implemented")
    override val personApiDao: PersonApiDao
        get() = TODO("Not yet implemented")
    override val påVentApiDao: PåVentApiDao
        get() = TODO("Not yet implemented")
    override val risikovurderingApiDao: RisikovurderingApiDao
        get() = TODO("Not yet implemented")
    override val personinfoDao: PersoninfoDao
        get() = TODO("Not yet implemented")
    override val tildelingApiDao: TildelingApiDao
        get() = TODO("Not yet implemented")
    override val varselApiRepository: VarselApiRepository
        get() = TODO("Not yet implemented")
    override val vergemålApiDao: VergemålApiDao
        get() = TODO("Not yet implemented")
    override val oppgaveRepository: OppgaveRepository
        get() = TODO("Not yet implemented")
}
