package no.nav.helse.mediator.api.graphql.schema

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.api.ReservasjonClient
import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGhostPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

enum class Kjonn { Mann, Kvinne, Ukjent }

data class Infotrygdutbetaling(
    val fom: String,
    val tom: String,
    val grad: String,
    val dagsats: Double,
    val typetekst: String,
    val organisasjonsnummer: String
)

data class Saksbehandler(
    val navn: String,
    val ident: String?
)

data class Reservasjon(
    val kanVarsles: Boolean,
    val reservert: Boolean,
)

data class Personinfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn?,
    val adressebeskyttelse: Adressebeskyttelse,
    val reservasjon: Reservasjon?
)

data class Enhet(
    val id: String,
    val navn: String
)

data class Tildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
    val reservert: Boolean
)

data class Person(
    private val snapshot: GraphQLPerson,
    private val personinfo: PersoninfoDto,
    private val personApiDao: PersonApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val oppgaveDao: OppgaveDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val reservasjonClient: ReservasjonClient,
) {
    fun versjon(): Int = snapshot.versjon

    fun aktorId(): String = snapshot.aktorId

    fun fodselsnummer(): String = snapshot.fodselsnummer

    fun dodsdato(): LocalDate? = snapshot.dodsdato

    fun personinfo(): Personinfo = Personinfo(
        fornavn = personinfo.fornavn,
        mellomnavn = personinfo.mellomnavn,
        etternavn = personinfo.etternavn,
        fodselsdato = personinfo.fødselsdato?.format(DateTimeFormatter.ISO_DATE),
        kjonn = when (personinfo.kjønn) {
            Kjønn.Mann -> Kjonn.Mann
            Kjønn.Kvinne -> Kjonn.Kvinne
            Kjønn.Ukjent -> Kjonn.Ukjent
            else -> null
        },
        adressebeskyttelse = personinfo.adressebeskyttelse,
        reservasjon = runBlocking {
            reservasjonClient.hentReservasjonsstatus(snapshot.fodselsnummer)
        }
    )

    fun enhet(): Enhet = personApiDao.finnEnhet(snapshot.fodselsnummer).let { Enhet(it.id, it.navn) }

    fun tildeling(): Tildeling? = tildelingDao.tildelingForPerson(snapshot.fodselsnummer)?.let {
        Tildeling(
            navn = it.navn,
            epost = it.epost,
            oid = it.oid.toString(),
            reservert = it.påVent
        )
    }

    fun arbeidsgivere(): List<Arbeidsgiver> = snapshot.arbeidsgivere.map {
        Arbeidsgiver(
            organisasjonsnummer = it.organisasjonsnummer,
            navn = arbeidsgiverApiDao.finnNavn(it.organisasjonsnummer) ?: "Ikke tilgjengelig",
            bransjer = arbeidsgiverApiDao.finnBransjer(it.organisasjonsnummer),
            ghostPerioder = it.ghostPerioder.tilGhostPerioder(it.organisasjonsnummer),
            fødselsnummer = snapshot.fodselsnummer,
            overstyringApiDao = overstyringApiDao,
            generasjoner = it.generasjoner,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            oppgaveDao = oppgaveDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
        )
    }

    fun infotrygdutbetalinger(): List<Infotrygdutbetaling>? =
        personApiDao.finnInfotrygdutbetalinger(snapshot.fodselsnummer)
            ?.let { objectMapper.readValue(it) }

    fun vilkarsgrunnlaghistorikk(): List<Vilkarsgrunnlaghistorikk> =
        snapshot.vilkarsgrunnlaghistorikk.map { it.tilVilkarsgrunnlaghistorikk() }

    private fun List<GraphQLGhostPeriode>.tilGhostPerioder(organisasjonsnummer: String): List<GhostPeriode> =
        map {
            GhostPeriode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                skjaeringstidspunkt = it.skjaeringstidspunkt,
                vilkarsgrunnlaghistorikkId = it.vilkarsgrunnlaghistorikkId,
                deaktivert = it.deaktivert,
                organisasjonsnummer = organisasjonsnummer
            )
        }
}
