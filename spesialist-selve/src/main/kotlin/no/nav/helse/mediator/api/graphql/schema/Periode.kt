package no.nav.helse.mediator.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.hentsnapshot.Alder
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatType
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import no.nav.helse.mediator.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

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

data class Vurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: LocalDateTime
)

data class Simuleringslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int
)

data class Simuleringsdetaljer(
    val fom: LocalDate,
    val tom: LocalDate,
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
    val utbetalingstype: String
)

data class Simuleringsutbetaling(
    val mottakerId: String,
    val mottakerNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<Simuleringsdetaljer>
)

data class Simuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<Simuleringsutbetaling>
)

data class Simulering(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val utbetalingslinjer: List<Simuleringslinje>,
    val totalbelop: Int?,
    val perioder: List<Simuleringsperiode>?
)

data class Utbetaling(
    val id: UUID,
    val arbeidsgiverFagsystemId: String,
    val arbeidsgiverNettoBelop: Int,
    val personFagsystemId: String,
    val personNettoBelop: Int,
    val status: Utbetalingstatus,
    val type: Utbetalingtype,
    val vurdering: Vurdering?,
    val arbeidsgiversimulering: Simulering?,
    val personsimulering: Simulering?
)

data class Periodevilkar(
    val alder: Alder,
    val soknadsfrist: Soknadsfrist?,
    val sykepengedager: Sykepengedager
)

data class Notat(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: UUID,
    val feilregistrert: Boolean,
    val feilregistrert_tidspunkt: LocalDateTime?,
    val type: NotatType
)

data class PeriodeHistorikkElement(
    val type: PeriodehistorikkType,
    val timestamp: LocalDateTime,
    val saksbehandler_ident: String,
    val notat_id: Int?
)

data class Aktivitet(
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String,
    val vedtaksperiodeId: UUID
)

data class Faresignal(
    val beskrivelse: String,
    val kategori: List<String>
)

data class Risikovurdering(
    val funn: List<Faresignal>?,
    val kontrollertOk: List<Faresignal>
)

data class Refusjon(
    val belop: Double?,
    val arbeidsgiverperioder: List<Refusjonsperiode>,
    val endringer: List<Endring>,
    val forsteFravaersdag: LocalDate?,
    val sisteRefusjonsdag: LocalDate?
) {
    data class Refusjonsperiode(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class Endring(
        val belop: Double,
        val dato: LocalDate
    )
}

interface Periode {
    fun erForkastet(): Boolean
    fun fom(): LocalDate
    fun tom(): LocalDate
    fun inntektstype(): Inntektstype
    fun opprettet(): LocalDateTime
    fun periodetype(): Periodetype
    fun tidslinje(): List<Dag>
    fun vedtaksperiodeId(): UUID
    fun periodetilstand(): Periodetilstand
    fun skjaeringstidspunkt(): LocalDate

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
        else -> Periodetilstand.Ukjent
    }

    @GraphQLIgnore
    fun erForkastet(periode: GraphQLTidslinjeperiode): Boolean = periode.erForkastet

    @GraphQLIgnore
    fun fom(periode: GraphQLTidslinjeperiode): LocalDate = periode.fom

    @GraphQLIgnore
    fun tom(periode: GraphQLTidslinjeperiode): LocalDate = periode.tom

    @GraphQLIgnore
    fun inntektstype(periode: GraphQLTidslinjeperiode): Inntektstype = when (periode.inntektstype) {
        GraphQLInntektstype.ENARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
        GraphQLInntektstype.FLEREARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
        else -> throw Exception("Ukjent inntektstype ${periode.inntektstype}")
    }

    @GraphQLIgnore
    fun opprettet(periode: GraphQLTidslinjeperiode): LocalDateTime = periode.opprettet

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
    val id: UUID,
    private val periode: GraphQLTidslinjeperiode
) : Periode {
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): LocalDate = fom(periode)
    override fun tom(): LocalDate = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): LocalDateTime = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<Dag> = tidslinje(periode)
    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId
    override fun periodetilstand(): Periodetilstand = periodetilstand(periode.periodetilstand)
    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt
}


data class BeregnetPeriode(
    val id: UUID,
    private val periode: GraphQLBeregnetPeriode,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val oppgaveDao: OppgaveDao,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
) : Periode {
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): LocalDate = fom(periode)
    override fun tom(): LocalDate = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): LocalDateTime = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<Dag> = tidslinje(periode)
    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId
    override fun periodetilstand(): Periodetilstand = periodetilstand(periode.periodetilstand)

    @Deprecated("erBeslutterOppgave bør hentes fra periodens oppgave")
    fun erBeslutterOppgave(): Boolean = oppgaveDao.erBeslutteroppgave(java.util.UUID.fromString(vedtaksperiodeId()))

    @Deprecated("erReturOppgave bør hentes fra periodens oppgave")
    fun erReturOppgave(): Boolean = oppgaveDao.erReturOppgave(java.util.UUID.fromString(vedtaksperiodeId()))

    @Deprecated("trengerTotrinnsvurdering bør hentes fra periodens oppgave")
    fun trengerTotrinnsvurdering(): Boolean = oppgaveDao.trengerTotrinnsvurdering(java.util.UUID.fromString(vedtaksperiodeId()))

    @Deprecated("tidligereSaksbehandlerOid bør hentes fra periodens oppgave")
    fun tidligereSaksbehandlerOid(): UUID? = oppgaveDao.hentTidligereSaksbehandlerOid(java.util.UUID.fromString(vedtaksperiodeId()))?.toString()

    fun notater(): List<Notat> = notatDao.finnNotater(java.util.UUID.fromString(vedtaksperiodeId())).map {
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
            type = it.type
        )
    }

    fun periodehistorikk(): List<PeriodeHistorikkElement> =
        periodehistorikkDao.finn(java.util.UUID.fromString(utbetaling().id)).map {
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

    fun beregningId(): UUID = periode.beregningId

    fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }

    fun maksdato(): LocalDate = periode.maksdato

    fun periodevilkar(): Periodevilkar = Periodevilkar(
        alder = periode.periodevilkar.alder,
        soknadsfrist = periode.periodevilkar.soknadsfrist,
        sykepengedager = periode.periodevilkar.sykepengedager
    )

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

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

    fun vilkarsgrunnlaghistorikkId(): UUID = periode.vilkarsgrunnlaghistorikkId

    fun risikovurdering(): Risikovurdering? =
        risikovurderingApiDao.finnRisikovurdering(vedtaksperiodeId().java())?.let { vurdering ->
            Risikovurdering(
                funn = vurdering.funn.tilFaresignaler(),
                kontrollertOk = vurdering.kontrollertOk.tilFaresignaler()
            )
        }

    fun varsler(): List<String> = varselDao.finnAktiveVarsler(vedtaksperiodeId().java()).distinct()

    fun refusjon(): Refusjon? = periode.refusjon?.let { refusjon ->
        Refusjon(
            belop = refusjon.belop,
            arbeidsgiverperioder = refusjon.arbeidsgiverperioder.map {
                Refusjon.Refusjonsperiode(it.fom, it.tom)
            },
            endringer = refusjon.endringer.map {
                Refusjon.Endring(it.belop, it.dato)
            },
            forsteFravaersdag = refusjon.forsteFravaersdag,
            sisteRefusjonsdag = refusjon.sisteRefusjonsdag
        )
    }

    @Deprecated("Oppgavereferanse bør hentes fra periodens oppgave")
    fun oppgavereferanse(): String? =
        oppgaveDao.finnOppgaveId(java.util.UUID.fromString(vedtaksperiodeId()))?.toString()

    fun oppgave(): OppgaveForPeriodevisning? =
        oppgaveApiDao.finnPeriodeoppgave(java.util.UUID.fromString(vedtaksperiodeId()))?.let {
            OppgaveForPeriodevisning(
                id = it.id,
                erBeslutter = it.erBeslutter,
                erRetur = it.erRetur,
                trengerTotrinnsvurdering = it.trengerTotrinnsvurdering,
                tidligereSaksbehandler = it.tidligereSaksbehandler,
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

private fun UUID.java() = java.util.UUID.fromString(this)
