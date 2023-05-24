package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.enums.GraphQLInntektstype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Alder
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

enum class Inntektstype { ENARBEIDSGIVER, FLEREARBEIDSGIVERE }

enum class Periodetype {
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT
}

enum class Periodetilstand {
    UtbetalingFeilet,
    RevurderingFeilet,
    AnnulleringFeilet,
    TilAnnullering,
    TilUtbetaling,
    Annullert,
    Utbetalt,
    IngenUtbetaling,
    TilInfotrygd,
    ForberederGodkjenning,
    ManglerInformasjon,
    TilGodkjenning,
    VenterPaEnAnnenPeriode,
    UtbetaltVenterPaEnAnnenPeriode,
    Ukjent
}

enum class Utbetalingstatus {
    ANNULLERT,
    FORKASTET,
    GODKJENT,
    GODKJENTUTENUTBETALING,
    IKKEGODKJENT,
    OVERFORT,
    SENDT,
    UBETALT,
    UTBETALINGFEILET,
    UTBETALT,
    UKJENT
}

enum class Utbetalingtype {
    ANNULLERING,
    ETTERUTBETALING,
    FERIEPENGER,
    REVURDERING,
    UTBETALING,
    UKJENT
}

enum class Varselstatus {
    AKTIV,
    VURDERT,
    GODKJENT,
    AVVIST,
}

data class Vurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: DateTimeString,
)

data class Simuleringslinje(
    val fom: DateString,
    val tom: DateString,
    val dagsats: Int,
    val grad: Int,
)

data class Simuleringsdetaljer(
    val fom: DateString,
    val tom: DateString,
    val belop: Int,
    val antallSats: Int,
    val klassekode: String,
    val klassekodebeskrivelse: String,
    val konto: String,
    val refunderesOrgNr: String,
    val sats: Double,
    val tilbakeforing: Boolean,
    val typeSats: String,
    val uforegrad: Int,
    val utbetalingstype: String,
)

data class Simuleringsutbetaling(
    val mottakerId: String,
    val mottakerNavn: String,
    val forfall: DateString,
    val feilkonto: Boolean,
    val detaljer: List<Simuleringsdetaljer>,
)

data class Simuleringsperiode(
    val fom: DateString,
    val tom: DateString,
    val utbetalinger: List<Simuleringsutbetaling>,
)

data class Simulering(
    val fagsystemId: String,
    val tidsstempel: DateTimeString,
    val utbetalingslinjer: List<Simuleringslinje>,
    val totalbelop: Int?,
    val perioder: List<Simuleringsperiode>?,
)

data class Utbetaling(
    val id: UUIDString,
    val arbeidsgiverFagsystemId: String,
    val arbeidsgiverNettoBelop: Int,
    val personFagsystemId: String,
    val personNettoBelop: Int,
    val status: Utbetalingstatus,
    val type: Utbetalingtype,
    val vurdering: Vurdering?,
    val arbeidsgiversimulering: Simulering?,
    val personsimulering: Simulering?,
)

data class Periodevilkar(
    val alder: Alder,
    val soknadsfrist: Soknadsfrist?,
    val sykepengedager: Sykepengedager,
)

data class Kommentar(
    val id: Int,
    val tekst: String,
    val opprettet: DateTimeString,
    val saksbehandlerident: String,
    val feilregistrert_tidspunkt: DateTimeString?,
)

data class Notater(
    val id: UUIDString,
    val notater: List<Notat>,
)

data class Notat(
    val id: Int,
    val tekst: String,
    val opprettet: DateTimeString,
    val saksbehandlerOid: UUIDString,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: UUIDString,
    val feilregistrert: Boolean,
    val feilregistrert_tidspunkt: DateTimeString?,
    val type: NotatType,
    val kommentarer: List<Kommentar>,
)

enum class NotatType {
    Retur,
    Generelt,
    PaaVent,
}

data class PeriodeHistorikkElement(
    val type: PeriodehistorikkType,
    val timestamp: DateTimeString,
    val saksbehandler_ident: String?,
    val notat_id: Int?,
)

data class Aktivitet(
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String,
    val vedtaksperiodeId: UUIDString,
)

data class Faresignal(
    val beskrivelse: String,
    val kategori: List<String>,
)

data class Risikovurdering(
    val funn: List<Faresignal>?,
    val kontrollertOk: List<Faresignal>,
)

// TODO: Fjern denne
data class Refusjon(
    val belop: Double?,
    val arbeidsgiverperioder: List<Refusjonsperiode>,
    val endringer: List<Endring>,
    val forsteFravaersdag: DateString?,
    val sisteRefusjonsdag: DateString?,
) {
    data class Refusjonsperiode(
        val fom: DateString,
        val tom: DateString,
    )

    data class Endring(
        val belop: Double,
        val dato: DateString,
    )
}

data class VarselDTO(
    val generasjonId: UUIDString,
    val definisjonId: UUIDString,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val vurdering: VarselvurderingDTO?,
) {
    data class VarselvurderingDTO(
        val ident: String,
        val tidsstempel: DateTimeString,
        val status: Varselstatus,
    )
}

interface Periode {
    fun erForkastet(): Boolean
    fun fom(): DateString
    fun tom(): DateString
    fun inntektstype(): Inntektstype
    fun opprettet(): DateTimeString
    fun periodetype(): Periodetype
    fun tidslinje(): List<Dag>
    fun vedtaksperiodeId(): UUIDString
    fun periodetilstand(): Periodetilstand
    fun skjaeringstidspunkt(): DateString
    fun varsler(): List<VarselDTO>
    fun hendelser(): List<Hendelse>

    @GraphQLIgnore
    fun periodetilstand(tilstand: GraphQLPeriodetilstand) = when (tilstand) {
        GraphQLPeriodetilstand.ANNULLERINGFEILET -> Periodetilstand.AnnulleringFeilet
        GraphQLPeriodetilstand.ANNULLERT -> Periodetilstand.Annullert
        GraphQLPeriodetilstand.INGENUTBETALING -> Periodetilstand.IngenUtbetaling
        GraphQLPeriodetilstand.REVURDERINGFEILET -> Periodetilstand.RevurderingFeilet
        GraphQLPeriodetilstand.TILANNULLERING -> Periodetilstand.TilAnnullering
        GraphQLPeriodetilstand.TILINFOTRYGD -> Periodetilstand.TilInfotrygd
        GraphQLPeriodetilstand.TILUTBETALING -> Periodetilstand.TilUtbetaling
        GraphQLPeriodetilstand.UTBETALT -> Periodetilstand.Utbetalt
        GraphQLPeriodetilstand.FORBEREDERGODKJENNING -> Periodetilstand.ForberederGodkjenning
        GraphQLPeriodetilstand.MANGLERINFORMASJON -> Periodetilstand.ManglerInformasjon
        GraphQLPeriodetilstand.TILGODKJENNING -> Periodetilstand.TilGodkjenning
        GraphQLPeriodetilstand.UTBETALINGFEILET -> Periodetilstand.UtbetalingFeilet
        GraphQLPeriodetilstand.VENTERPAANNENPERIODE -> Periodetilstand.VenterPaEnAnnenPeriode
        GraphQLPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> Periodetilstand.UtbetaltVenterPaEnAnnenPeriode
        else -> Periodetilstand.Ukjent
    }

    @GraphQLIgnore
    fun erForkastet(periode: GraphQLTidslinjeperiode): Boolean = periode.erForkastet

    @GraphQLIgnore
    fun fom(periode: GraphQLTidslinjeperiode): DateString = periode.fom

    @GraphQLIgnore
    fun tom(periode: GraphQLTidslinjeperiode): DateString = periode.tom

    @GraphQLIgnore
    fun inntektstype(periode: GraphQLTidslinjeperiode): Inntektstype = when (periode.inntektstype) {
        GraphQLInntektstype.ENARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
        GraphQLInntektstype.FLEREARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
        else -> throw Exception("Ukjent inntektstype ${periode.inntektstype}")
    }

    @GraphQLIgnore
    fun opprettet(periode: GraphQLTidslinjeperiode): DateTimeString = periode.opprettet

    @GraphQLIgnore
    fun periodetype(periode: GraphQLTidslinjeperiode): Periodetype = when (periode.periodetype) {
        GraphQLPeriodetype.FORLENGELSE -> Periodetype.FORLENGELSE
        GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
        GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
        GraphQLPeriodetype.OVERGANGFRAIT -> Periodetype.OVERGANG_FRA_IT
        else -> throw Exception("Ukjent periodetype ${periode.periodetype}")
    }

    @GraphQLIgnore
    fun tidslinje(periode: GraphQLTidslinjeperiode): List<Dag> = periode.tidslinje.map { it.tilDag() }
}

data class UberegnetPeriode(
    val id: UUIDString,
    private val varselRepository: ApiVarselRepository,
    private val periode: GraphQLTidslinjeperiode,
    private val skalViseAktiveVarsler: Boolean,
) : Periode {
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): DateString = fom(periode)
    override fun tom(): DateString = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): DateTimeString = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<Dag> = tidslinje(periode)
    override fun vedtaksperiodeId(): UUIDString = periode.vedtaksperiodeId
    override fun periodetilstand(): Periodetilstand = periodetilstand(periode.periodetilstand)
    override fun skjaeringstidspunkt(): DateString = periode.skjaeringstidspunkt
    override fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }
    override fun varsler(): List<VarselDTO> = if (skalViseAktiveVarsler)
        varselRepository.finnVarslerForUberegnetPeriode(UUID.fromString(vedtaksperiodeId())).toList() else
        varselRepository.finnGodkjenteVarslerForUberegnetPeriode(UUID.fromString(vedtaksperiodeId())).toList()
}

enum class Periodehandling {
    UTBETALE
}

data class Handling(val type: Periodehandling, val tillatt: Boolean, val begrunnelse: String? = null)

@Suppress("unused")
data class BeregnetPeriode(
    val id: UUIDString,
    private val orgnummer: String,
    private val periode: GraphQLBeregnetPeriode,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val tilganger: SaksbehandlerTilganger,
    private val erSisteGenerasjon: Boolean,
) : Periode {
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): DateString = fom(periode)
    override fun tom(): DateString = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): DateTimeString = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<Dag> = tidslinje(periode)
    override fun vedtaksperiodeId(): UUIDString = periode.vedtaksperiodeId
    override fun periodetilstand(): Periodetilstand = periodetilstand(periode.periodetilstand)
    fun handlinger() = byggHandlinger()

    private fun byggHandlinger(): List<Handling> {
        return if (periodetilstand(periode.periodetilstand) != Periodetilstand.TilGodkjenning)
            listOf(Handling(Periodehandling.UTBETALE, false, "perioden er ikke til godkjenning")
        ) else {
            val oppgavetype = oppgaveApiDao.finnOppgavetype(UUID.fromString(vedtaksperiodeId()))
            if (oppgavetype == Oppgavetype.RISK_QA && !tilganger.harTilgangTilRiskOppgaver())
                listOf(Handling(Periodehandling.UTBETALE, false, "IkkeTilgangTilRisk"))
            else listOf(Handling(Periodehandling.UTBETALE, true))
        }
    }

    override fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }

    fun inntektFraAordningen(): List<InntektFraAOrdningen> =
        oppgaveApiDao.finnPeriodensInntekterFraAordningen(
            periode.vedtaksperiodeId,
            periode.skjaeringstidspunkt,
            orgnummer
        )

    fun notater(): List<Notat> = notatDao.finnNotater(UUID.fromString(vedtaksperiodeId())).map {
        Notat(
            id = it.id,
            tekst = it.tekst,
            opprettet = it.opprettet.toString(),
            saksbehandlerOid = it.saksbehandlerOid.toString(),
            saksbehandlerNavn = it.saksbehandlerNavn,
            saksbehandlerEpost = it.saksbehandlerEpost,
            saksbehandlerIdent = it.saksbehandlerIdent,
            vedtaksperiodeId = it.vedtaksperiodeId.toString(),
            feilregistrert = it.feilregistrert,
            feilregistrert_tidspunkt = it.feilregistrert_tidspunkt.toString(),
            type = it.type,
            kommentarer = it.kommentarer.map { kommentar ->
                Kommentar(
                    id = kommentar.id,
                    tekst = kommentar.tekst,
                    opprettet = kommentar.opprettet.toString(),
                    saksbehandlerident = kommentar.saksbehandlerident,
                    feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt?.toString(),
                )
            }
        )
    }

    fun periodehistorikk(): List<PeriodeHistorikkElement> =
        periodehistorikkDao.finn(UUID.fromString(utbetaling().id)).map {
            PeriodeHistorikkElement(
                type = it.type,
                saksbehandler_ident = it.saksbehandler_ident,
                timestamp = it.timestamp.toString(),
                notat_id = it.notat_id
            )
        }

    fun aktivitetslogg(): List<Aktivitet> = periode.aktivitetslogg.map {
        Aktivitet(
            alvorlighetsgrad = it.alvorlighetsgrad,
            melding = it.melding,
            tidsstempel = it.tidsstempel,
            vedtaksperiodeId = it.vedtaksperiodeId
        )
    }

    fun beregningId(): UUIDString = periode.beregningId

    fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    fun maksdato(): DateString = periode.maksdato

    fun periodevilkar(): Periodevilkar = Periodevilkar(
        alder = periode.periodevilkar.alder,
        soknadsfrist = periode.periodevilkar.soknadsfrist,
        sykepengedager = periode.periodevilkar.sykepengedager
    )

    override fun skjaeringstidspunkt(): DateString = periode.skjaeringstidspunkt

    fun utbetaling(): Utbetaling = periode.utbetaling.let {
        Utbetaling(
            id = it.id,
            arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
            arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
            personFagsystemId = it.personFagsystemId,
            personNettoBelop = it.personNettoBelop,
            status = when (it.statusEnum) {
                GraphQLUtbetalingstatus.ANNULLERT -> Utbetalingstatus.ANNULLERT
                GraphQLUtbetalingstatus.FORKASTET -> Utbetalingstatus.FORKASTET
                GraphQLUtbetalingstatus.GODKJENT -> Utbetalingstatus.GODKJENT
                GraphQLUtbetalingstatus.GODKJENTUTENUTBETALING -> Utbetalingstatus.GODKJENTUTENUTBETALING
                GraphQLUtbetalingstatus.IKKEGODKJENT -> Utbetalingstatus.IKKEGODKJENT
                GraphQLUtbetalingstatus.OVERFORT -> Utbetalingstatus.OVERFORT
                GraphQLUtbetalingstatus.SENDT -> Utbetalingstatus.SENDT
                GraphQLUtbetalingstatus.UBETALT -> Utbetalingstatus.UBETALT
                GraphQLUtbetalingstatus.UTBETALINGFEILET -> Utbetalingstatus.UTBETALINGFEILET
                GraphQLUtbetalingstatus.UTBETALT -> Utbetalingstatus.UTBETALT
                GraphQLUtbetalingstatus.__UNKNOWN_VALUE -> Utbetalingstatus.UKJENT
            },
            type = when (it.typeEnum) {
                GraphQLUtbetalingtype.ANNULLERING -> Utbetalingtype.ANNULLERING
                GraphQLUtbetalingtype.ETTERUTBETALING -> Utbetalingtype.ETTERUTBETALING
                GraphQLUtbetalingtype.FERIEPENGER -> Utbetalingtype.FERIEPENGER
                GraphQLUtbetalingtype.REVURDERING -> Utbetalingtype.REVURDERING
                GraphQLUtbetalingtype.UTBETALING -> Utbetalingtype.UTBETALING
                GraphQLUtbetalingtype.__UNKNOWN_VALUE -> Utbetalingtype.UKJENT
            },
            vurdering = it.vurdering?.let { vurdering ->
                Vurdering(
                    automatisk = vurdering.automatisk,
                    godkjent = vurdering.godkjent,
                    ident = vurdering.ident,
                    tidsstempel = vurdering.tidsstempel
                )
            },
            arbeidsgiversimulering = it.arbeidsgiveroppdrag?.tilSimulering(),
            personsimulering = it.personoppdrag?.tilSimulering()
        )
    }

    fun vilkarsgrunnlagId(): UUIDString? = periode.vilkarsgrunnlagId

    fun risikovurdering(): Risikovurdering? =
        risikovurderingApiDao.finnRisikovurdering(UUID.fromString(vedtaksperiodeId()))?.let { vurdering ->
            Risikovurdering(
                funn = vurdering.funn.tilFaresignaler(),
                kontrollertOk = vurdering.kontrollertOk.tilFaresignaler()
            )
        }

    override fun varsler(): List<VarselDTO> =
        if (erSisteGenerasjon) varselRepository.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
            UUID.fromString(vedtaksperiodeId()),
            UUID.fromString(periode.utbetaling.id)
        ).toList()
        else varselRepository.finnVarslerSomIkkeErInaktiveFor(
            UUID.fromString(vedtaksperiodeId()),
            UUID.fromString(periode.utbetaling.id)
        ).toList()

    @Deprecated("Oppgavereferanse bør hentes fra periodens oppgave")
    fun oppgavereferanse(): String? =
        oppgaveApiDao.finnOppgaveId(UUID.fromString(vedtaksperiodeId()))?.toString()

    fun oppgave(): OppgaveForPeriodevisning? =
        oppgaveApiDao.finnPeriodeoppgave(UUID.fromString(vedtaksperiodeId()))?.let { oppgaveForPeriodevisningDto ->
            OppgaveForPeriodevisning(
                id = oppgaveForPeriodevisningDto.id
            )
        }

    fun totrinnsvurdering(): Totrinnsvurdering? =
        totrinnsvurderingApiDao.hentAktiv(UUID.fromString(vedtaksperiodeId()))?.let {
            Totrinnsvurdering(
                erRetur = it.erRetur,
                saksbehandler = it.saksbehandler?.toString(),
                beslutter = it.beslutter?.toString(),
                erBeslutteroppgave = !it.erRetur && it.saksbehandler != null
            )
        }
}

private fun GraphQLOppdrag.tilSimulering(): Simulering =
    Simulering(
        fagsystemId = fagsystemId,
        tidsstempel = tidsstempel,
        utbetalingslinjer = utbetalingslinjer.map { linje ->
            Simuleringslinje(
                fom = linje.fom,
                tom = linje.tom,
                dagsats = linje.dagsats,
                grad = linje.grad
            )
        },
        totalbelop = simulering?.totalbelop,
        perioder = simulering?.perioder?.map { periode ->
            Simuleringsperiode(
                fom = periode.fom,
                tom = periode.tom,
                utbetalinger = periode.utbetalinger.map { utbetaling ->
                    Simuleringsutbetaling(
                        mottakerNavn = utbetaling.utbetalesTilNavn,
                        mottakerId = utbetaling.utbetalesTilId,
                        forfall = utbetaling.forfall,
                        feilkonto = utbetaling.feilkonto,
                        detaljer = utbetaling.detaljer.map { detaljer ->
                            Simuleringsdetaljer(
                                fom = detaljer.faktiskFom,
                                tom = detaljer.faktiskTom,
                                belop = detaljer.belop,
                                antallSats = detaljer.antallSats,
                                klassekode = detaljer.klassekode,
                                klassekodebeskrivelse = detaljer.klassekodeBeskrivelse,
                                konto = detaljer.konto,
                                refunderesOrgNr = detaljer.refunderesOrgNr,
                                sats = detaljer.sats,
                                tilbakeforing = detaljer.tilbakeforing,
                                typeSats = detaljer.typeSats,
                                uforegrad = detaljer.uforegrad,
                                utbetalingstype = detaljer.utbetalingstype
                            )
                        }
                    )
                }
            )
        }
    )

private fun List<JsonNode>.tilFaresignaler(): List<Faresignal> =
    map { objectMapper.readValue(it.traverse(), object : TypeReference<Faresignal>() {}) }
