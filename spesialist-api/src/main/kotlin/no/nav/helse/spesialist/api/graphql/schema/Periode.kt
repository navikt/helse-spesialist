package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.Toggle
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

enum class Inntektstype { ENARBEIDSGIVER, FLEREARBEIDSGIVERE }

enum class Periodetype {
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
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
    TilSkjonnsfastsettelse,
    Ukjent,
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
    UKJENT,
}

enum class Utbetalingtype {
    ANNULLERING,
    ETTERUTBETALING,
    FERIEPENGER,
    REVURDERING,
    UTBETALING,
    UKJENT,
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
    val tidsstempel: LocalDateTime,
)

data class Simuleringslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int,
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
    val utbetalingstype: String,
)

data class Simuleringsutbetaling(
    val mottakerId: String,
    val mottakerNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<Simuleringsdetaljer>,
)

data class Simuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<Simuleringsutbetaling>,
)

data class Simulering(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val utbetalingslinjer: List<Simuleringslinje>,
    val totalbelop: Int?,
    val perioder: List<Simuleringsperiode>?,
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
    val personsimulering: Simulering?,
)

data class Periodevilkar(
    val alder: Alder,
    val sykepengedager: Sykepengedager,
)

data class Kommentar(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrert_tidspunkt: LocalDateTime?,
)

data class Notater(
    val id: UUID,
    val notater: List<Notat>,
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
    val type: NotatType,
    val kommentarer: List<Kommentar>,
)

enum class NotatType {
    Retur,
    Generelt,
    PaaVent,
    OpphevStans,
}

data class PeriodeHistorikkElement(
    val type: PeriodehistorikkType,
    val timestamp: LocalDateTime,
    val saksbehandler_ident: String?,
    val notat_id: Int?,
)

data class Faresignal(
    val beskrivelse: String,
    val kategori: List<String>,
)

data class Risikovurdering(
    val funn: List<Faresignal>?,
    val kontrollertOk: List<Faresignal>,
)

data class VarselDTO(
    val generasjonId: UUID,
    val definisjonId: UUID,
    val opprettet: LocalDateTime,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val vurdering: VarselvurderingDTO?,
) {
    data class VarselvurderingDTO(
        val ident: String,
        val tidsstempel: LocalDateTime,
        val status: Varselstatus,
    )
}

interface Periode {
    fun behandlingId(): UUID

    fun erForkastet(): Boolean

    fun fom(): LocalDate

    fun tom(): LocalDate

    fun id(): UUID

    fun inntektstype(): Inntektstype

    fun opprettet(): LocalDateTime

    fun periodetype(): Periodetype

    fun tidslinje(): List<Dag>

    fun vedtaksperiodeId(): UUID

    fun periodetilstand(): Periodetilstand

    fun skjaeringstidspunkt(): LocalDate

    fun varsler(): List<VarselDTO>

    fun hendelser(): List<Hendelse>

    @GraphQLIgnore
    fun periodetilstand(
        tilstand: GraphQLPeriodetilstand,
        erSisteGenerasjon: Boolean,
    ) = when (tilstand) {
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
        GraphQLPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> {
            if (Toggle.BehandleEnOgEnPeriode.enabled && erSisteGenerasjon) {
                Periodetilstand.VenterPaEnAnnenPeriode
            } else {
                Periodetilstand.UtbetaltVenterPaEnAnnenPeriode
            }
        }

        GraphQLPeriodetilstand.TILSKJONNSFASTSETTELSE -> Periodetilstand.TilSkjonnsfastsettelse
        else -> Periodetilstand.Ukjent
    }

    @GraphQLIgnore
    fun erForkastet(periode: GraphQLTidslinjeperiode): Boolean = periode.erForkastet

    @GraphQLIgnore
    fun fom(periode: GraphQLTidslinjeperiode): LocalDate = periode.fom

    @GraphQLIgnore
    fun tom(periode: GraphQLTidslinjeperiode): LocalDate = periode.tom

    @GraphQLIgnore
    fun inntektstype(periode: GraphQLTidslinjeperiode): Inntektstype =
        when (periode.inntektstype) {
            GraphQLInntektstype.ENARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
            GraphQLInntektstype.FLEREARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
            else -> throw Exception("Ukjent inntektstype ${periode.inntektstype}")
        }

    @GraphQLIgnore
    fun opprettet(periode: GraphQLTidslinjeperiode): LocalDateTime = periode.opprettet

    @GraphQLIgnore
    fun periodetype(periode: GraphQLTidslinjeperiode): Periodetype =
        when (periode.periodetype) {
            GraphQLPeriodetype.FORLENGELSE -> Periodetype.FORLENGELSE
            GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
            GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
            GraphQLPeriodetype.OVERGANGFRAIT -> Periodetype.OVERGANG_FRA_IT
            else -> throw Exception("Ukjent periodetype ${periode.periodetype}")
        }

    @GraphQLIgnore
    fun notater(
        notatDao: NotatDao,
        vedtaksperiodeId: UUID,
    ): List<Notat> =
        notatDao.finnNotater(vedtaksperiodeId).map {
            Notat(
                id = it.id,
                tekst = it.tekst,
                opprettet = it.opprettet,
                saksbehandlerOid = it.saksbehandlerOid,
                saksbehandlerNavn = it.saksbehandlerNavn,
                saksbehandlerEpost = it.saksbehandlerEpost,
                saksbehandlerIdent = it.saksbehandlerIdent,
                vedtaksperiodeId = it.vedtaksperiodeId,
                feilregistrert = it.feilregistrert,
                feilregistrert_tidspunkt = it.feilregistrert_tidspunkt,
                type = it.type,
                kommentarer =
                    it.kommentarer.map { kommentar ->
                        Kommentar(
                            id = kommentar.id,
                            tekst = kommentar.tekst,
                            opprettet = kommentar.opprettet,
                            saksbehandlerident = kommentar.saksbehandlerident,
                            feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                        )
                    },
            )
        }

    @GraphQLIgnore
    fun tidslinje(periode: GraphQLTidslinjeperiode): List<Dag> = periode.tidslinje.map { it.tilDag() }
}

@Suppress("unused")
data class UberegnetPeriode(
    private val varselRepository: ApiVarselRepository,
    private val periode: GraphQLTidslinjeperiode,
    private val skalViseAktiveVarsler: Boolean,
    private val notatDao: NotatDao,
    private val index: Int,
) : Periode {
    override fun behandlingId(): UUID = periode.behandlingId

    override fun erForkastet(): Boolean = erForkastet(periode)

    override fun fom(): LocalDate = fom(periode)

    override fun tom(): LocalDate = tom(periode)

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): Inntektstype = inntektstype(periode)

    override fun opprettet(): LocalDateTime = opprettet(periode)

    override fun periodetype(): Periodetype = periodetype(periode)

    override fun tidslinje(): List<Dag> = tidslinje(periode)

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): Periodetilstand = periodetilstand(periode.periodetilstand, true)

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    override fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }

    override fun varsler(): List<VarselDTO> =
        if (skalViseAktiveVarsler) {
            varselRepository.finnVarslerForUberegnetPeriode(vedtaksperiodeId()).toList()
        } else {
            varselRepository.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId()).toList()
        }

    fun notater(): List<Notat> = notater(notatDao, vedtaksperiodeId())
}

enum class Periodehandling {
    UTBETALE,
    AVVISE,
}

data class Handling(
    val type: Periodehandling,
    val tillatt: Boolean,
    val begrunnelse: String? = null,
)

@Suppress("unused")
data class BeregnetPeriode(
    private val orgnummer: String,
    private val periode: GraphQLBeregnetPeriode,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val tilganger: SaksbehandlerTilganger,
    private val erSisteGenerasjon: Boolean,
    private val index: Int,
) : Periode {
    private val periodetilstand = periodetilstand(periode.periodetilstand, erSisteGenerasjon)

    override fun behandlingId(): UUID = periode.behandlingId

    override fun erForkastet(): Boolean = erForkastet(periode)

    override fun fom(): LocalDate = fom(periode)

    override fun tom(): LocalDate = tom(periode)

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): Inntektstype = inntektstype(periode)

    override fun opprettet(): LocalDateTime = opprettet(periode)

    override fun periodetype(): Periodetype = periodetype(periode)

    override fun tidslinje(): List<Dag> = tidslinje(periode)

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): Periodetilstand = periodetilstand

    fun handlinger() = byggHandlinger()

    fun egenskaper(): List<Oppgaveegenskap> = oppgavehåndterer.hentEgenskaper(periode.vedtaksperiodeId, periode.utbetaling.id)

    private fun byggHandlinger(): List<Handling> =
        if (periodetilstand != Periodetilstand.TilGodkjenning) {
            listOf(
                Handling(Periodehandling.UTBETALE, false, "perioden er ikke til godkjenning"),
            )
        } else {
            val handlinger = listOf(Handling(Periodehandling.UTBETALE, true))
            handlinger +
                when (oppgaveDto?.kanAvvises) {
                    true -> Handling(Periodehandling.AVVISE, true)
                    else -> Handling(Periodehandling.AVVISE, false, "Spleis støtter ikke å avvise perioden")
                }
        }

    override fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }

    fun notater(): List<Notat> = notater(notatDao, vedtaksperiodeId())

    fun periodehistorikk(): List<PeriodeHistorikkElement> =
        periodehistorikkDao.finn(utbetaling().id).map {
            PeriodeHistorikkElement(
                type = it.type,
                saksbehandler_ident = it.saksbehandler_ident,
                timestamp = it.timestamp,
                notat_id = it.notat_id,
            )
        }

    fun beregningId(): UUID = periode.beregningId

    fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    fun maksdato(): LocalDate = periode.maksdato

    fun periodevilkar(): Periodevilkar =
        Periodevilkar(
            alder = periode.periodevilkar.alder,
            sykepengedager = periode.periodevilkar.sykepengedager,
        )

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    fun utbetaling(): Utbetaling =
        periode.utbetaling.let {
            Utbetaling(
                id = it.id,
                arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
                personFagsystemId = it.personFagsystemId,
                personNettoBelop = it.personNettoBelop,
                status =
                    when (it.statusEnum) {
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
                type =
                    when (it.typeEnum) {
                        GraphQLUtbetalingtype.ANNULLERING -> Utbetalingtype.ANNULLERING
                        GraphQLUtbetalingtype.ETTERUTBETALING -> Utbetalingtype.ETTERUTBETALING
                        GraphQLUtbetalingtype.FERIEPENGER -> Utbetalingtype.FERIEPENGER
                        GraphQLUtbetalingtype.REVURDERING -> Utbetalingtype.REVURDERING
                        GraphQLUtbetalingtype.UTBETALING -> Utbetalingtype.UTBETALING
                        GraphQLUtbetalingtype.__UNKNOWN_VALUE -> Utbetalingtype.UKJENT
                    },
                vurdering =
                    it.vurdering?.let { vurdering ->
                        Vurdering(
                            automatisk = vurdering.automatisk,
                            godkjent = vurdering.godkjent,
                            ident = vurdering.ident,
                            tidsstempel = vurdering.tidsstempel,
                        )
                    },
                arbeidsgiversimulering = it.arbeidsgiveroppdrag?.tilSimulering(),
                personsimulering = it.personoppdrag?.tilSimulering(),
            )
        }

    fun vilkarsgrunnlagId(): UUID? = periode.vilkarsgrunnlagId

    fun risikovurdering(): Risikovurdering? =
        risikovurderingApiDao.finnRisikovurdering(vedtaksperiodeId())?.let { vurdering ->
            Risikovurdering(
                funn = vurdering.funn.tilFaresignaler(),
                kontrollertOk = vurdering.kontrollertOk.tilFaresignaler(),
            )
        }

    override fun varsler(): List<VarselDTO> =
        if (erSisteGenerasjon) {
            varselRepository
                .finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
                    vedtaksperiodeId(),
                    periode.utbetaling.id,
                ).toList()
        } else {
            varselRepository
                .finnVarslerSomIkkeErInaktiveFor(
                    vedtaksperiodeId(),
                    periode.utbetaling.id,
                ).toList()
        }

    @Deprecated("Oppgavereferanse bør hentes fra periodens oppgave")
    fun oppgavereferanse(): String? = oppgaveApiDao.finnOppgaveId(vedtaksperiodeId())?.toString()

    private val oppgaveDto: OppgaveForPeriodevisningDto? by lazy {
        oppgaveApiDao.finnPeriodeoppgave(periode.vedtaksperiodeId)
    }

    val oppgave = oppgaveDto?.let { oppgaveDto -> OppgaveForPeriodevisning(id = oppgaveDto.id) }

    fun totrinnsvurdering(): Totrinnsvurdering? =
        totrinnsvurderingApiDao.hentAktiv(vedtaksperiodeId())?.let {
            Totrinnsvurdering(
                erRetur = it.erRetur,
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erBeslutteroppgave = !it.erRetur && it.saksbehandler != null,
            )
        }

    fun paVent(): PaVent? =
        påVentApiDao.hentAktivPåVent(vedtaksperiodeId())?.let {
            PaVent(
                frist = it.frist,
                begrunnelse = it.begrunnelse,
                oid = it.oid,
            )
        }

    fun avslag(): List<Avslag> = saksbehandlerhåndterer.hentAvslag(periode.vedtaksperiodeId, periode.utbetaling.id).toList()

    fun annullering(): Annullering? = saksbehandlerhåndterer.hentAnnullering(periode.utbetaling.id)
}

private fun GraphQLOppdrag.tilSimulering(): Simulering =
    Simulering(
        fagsystemId = fagsystemId,
        tidsstempel = tidsstempel,
        utbetalingslinjer =
            utbetalingslinjer.map { linje ->
                Simuleringslinje(
                    fom = linje.fom,
                    tom = linje.tom,
                    dagsats = linje.dagsats,
                    grad = linje.grad,
                )
            },
        totalbelop = simulering?.totalbelop,
        perioder =
            simulering?.perioder?.map { periode ->
                Simuleringsperiode(
                    fom = periode.fom,
                    tom = periode.tom,
                    utbetalinger =
                        periode.utbetalinger.map { utbetaling ->
                            Simuleringsutbetaling(
                                mottakerNavn = utbetaling.utbetalesTilNavn,
                                mottakerId = utbetaling.utbetalesTilId,
                                forfall = utbetaling.forfall,
                                feilkonto = utbetaling.feilkonto,
                                detaljer =
                                    utbetaling.detaljer.map { detaljer ->
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
                                            utbetalingstype = detaljer.utbetalingstype,
                                        )
                                    },
                            )
                        },
                )
            },
    )

private fun List<JsonNode>.tilFaresignaler(): List<Faresignal> =
    map { objectMapper.readValue(it.traverse(), object : TypeReference<Faresignal>() {}) }
