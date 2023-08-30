package no.nav.helse.spesialist.api.graphql.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson

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

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reservasjon(
    val kanVarsles: Boolean,
    val reservert: Boolean,
)

data class Enhet(
    val id: String,
    val navn: String
)

data class Tildeling(
    val navn: String,
    val epost: String,
    val oid: UUIDString,
    val paaVent: Boolean,
) {
    @Deprecated("Skal fjernes til fordel for paaVent")
    val reservert: Boolean? = null
}

data class Person(
    private val snapshot: GraphQLPerson,
    private val personinfo: Personinfo,
    private val personApiDao: PersonApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val tilganger: SaksbehandlerTilganger,
) {
    fun versjon(): Int = snapshot.versjon

    fun aktorId(): String = snapshot.aktorId

    fun fodselsnummer(): String = snapshot.fodselsnummer

    fun dodsdato(): DateString? = snapshot.dodsdato

    fun personinfo(): Personinfo = personinfo

    fun enhet(): Enhet = personApiDao.finnEnhet(snapshot.fodselsnummer).let { Enhet(it.id, it.navn) }

    fun tildeling(): Tildeling? = tildelingDao.tildelingForPerson(snapshot.fodselsnummer)?.let {
        Tildeling(
            navn = it.navn,
            epost = it.epost,
            oid = it.oid.toString(),
            paaVent = it.påVent,
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
            varselRepository = varselRepository,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
            tilganger = tilganger,
        )
    }

    fun infotrygdutbetalinger(): List<Infotrygdutbetaling>? =
        personApiDao.finnInfotrygdutbetalinger(snapshot.fodselsnummer)
            ?.let { objectMapper.readValue(it) }

    fun vilkarsgrunnlag(): List<Vilkarsgrunnlag> = snapshot.vilkarsgrunnlag.map { it.tilVilkarsgrunnlag() }

    private fun List<GraphQLGhostPeriode>.tilGhostPerioder(organisasjonsnummer: String): List<GhostPeriode> =
        map {
            GhostPeriode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                skjaeringstidspunkt = it.skjaeringstidspunkt,
                vilkarsgrunnlagId = it.vilkarsgrunnlagId,
                deaktivert = it.deaktivert,
                organisasjonsnummer = organisasjonsnummer
            )
        }
}
