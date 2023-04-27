package no.nav.helse

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.graphql.enums.GraphQLInntektstype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Alder
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLAktivitet
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Sykepengedager

object Testdata {
    // Modifiserbare globale verdier fører ofte til spaghetti. Ved å bruke et stygt navn brer det forhåpentligvis ikke om seg
    var _MODIFISERTBART_FØDSELSNUMMER = "12020052345"
    val FØDSELSNUMMER get() = _MODIFISERTBART_FØDSELSNUMMER

    const val AKTØR = "999999999"
    const val ORGNR = "222222222"
    const val ORGNR_GHOST = "666666666"

    const val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    const val SAKSBEHANDLER_IDENT = "X999999"
    const val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    val SAKSBEHANDLERTILGANGER_UTEN_TILGANGER =
        no.nav.helse.spesialist.api.SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            saksbehandlerIdent = SAKSBEHANDLER_IDENT,
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            riskSaksbehandlergruppe = UUID.randomUUID(),
            beslutterSaksbehandlergruppe = UUID.randomUUID(),
            skjermedePersonerSaksbehandlergruppe = UUID.randomUUID(),
            saksbehandlereMedTilgangTilStikkprøve = emptyList()
        )
    internal val UTBETALING_ID = UUID.randomUUID()
    internal val UTBETALING_ID2 = UUID.randomUUID()

    const val VARSEL_KODE_1 = "VARSEL_KODE_1"
    const val VARSEL_KODE_2 = "VARSEL_KODE_2"

    val VEDTAKSPERIODE_ID: UUID = UUID.randomUUID()
    val SNAPSHOT_MED_WARNINGS = snapshotMedWarnings(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        orgnr = ORGNR,
        fnr = FØDSELSNUMMER,
        aktørId = AKTØR,
        utbetalingId = UTBETALING_ID,
    )

    val SNAPSHOT_UTEN_WARNINGS = snapshot()

    fun snapshot(
        versjon: Int = 1,
        fødselsnummer: String = FØDSELSNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        arbeidsgiverbeløp: Int = 30000,
        personbeløp: Int = 0,
        utbetaling: GraphQLUtbetaling = GraphQLUtbetaling(
            id = utbetalingId.toString(),
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
            arbeidsgiverNettoBelop = arbeidsgiverbeløp,
            personFagsystemId = "EN_FAGSYSTEMID",
            personNettoBelop = personbeløp,
            statusEnum = GraphQLUtbetalingstatus.UBETALT,
            typeEnum = Utbetalingtype.UTBETALING,
            vurdering = null,
            personoppdrag = null,
            arbeidsgiveroppdrag = null
        ),
        regelverksvarsler: List<String> = emptyList(),
    ): GraphQLClientResponse<HentSnapshot.Result> =
        object : GraphQLClientResponse<HentSnapshot.Result> {
            val aktivitetslogg = regelverksvarsler.map {
                GraphQLAktivitet(
                    "W",
                    it,
                    "2020-06-12 13:21:24.072",
                    vedtaksperiodeId.toString()
                )
            }
            override val data = HentSnapshot.Result(
                GraphQLPerson(
                    aktorId = AKTØR,
                    fodselsnummer = fødselsnummer,
                    versjon = versjon,
                    arbeidsgivere = listOf(
                        GraphQLArbeidsgiver(
                            organisasjonsnummer = ORGNR,
                            ghostPerioder = emptyList(),
                            generasjoner = listOf(
                                GraphQLGenerasjon(
                                    id = UUID.randomUUID().toString(),
                                    perioder = listOf(
                                        GraphQLBeregnetPeriode(
                                            id = UUID.randomUUID().toString(),
                                            vedtaksperiodeId = vedtaksperiodeId.toString(),
                                            utbetaling = utbetaling,
                                            erForkastet = false,
                                            fom = "2020-01-01",
                                            tom = "2020-01-31",
                                            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                            opprettet = "2020-01-31",
                                            periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                            tidslinje = emptyList(),
                                            aktivitetslogg = this.aktivitetslogg,
                                            beregningId = UUID.randomUUID().toString(),
                                            forbrukteSykedager = null,
                                            gjenstaendeSykedager = null,
                                            hendelser = emptyList(),
                                            maksdato = "2021-01-01",
                                            periodevilkar = GraphQLPeriodevilkar(
                                                alder = Alder(
                                                    alderSisteSykedag = 30,
                                                    oppfylt = true,
                                                ),
                                                soknadsfrist = Soknadsfrist(
                                                    sendtNav = "2020-01-31",
                                                    soknadFom = "2020-01-01",
                                                    soknadTom = "2020-01-31",
                                                    oppfylt = true,
                                                ),
                                                sykepengedager = Sykepengedager(
                                                    forbrukteSykedager = null,
                                                    gjenstaendeSykedager = null,
                                                    maksdato = "2021-01-01",
                                                    skjaeringstidspunkt = "2020-01-01",
                                                    oppfylt = true,
                                                )
                                            ),
                                            skjaeringstidspunkt = "2020-01-01",
                                            vilkarsgrunnlagId = null,
                                            periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING
                                        )
                                    )
                                )
                            ),
                        )
                    ),
                    dodsdato = null,
                    vilkarsgrunnlag = emptyList(),
                )
            )
        }

    fun snapshotMedRevurderingUtbetaling(
        versjon: Int = 1,
        fødselsnummer: String = FØDSELSNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID,
        arbeidsgiverbeløp: Int = 30000,
        personbeløp: Int = 0,
    ): GraphQLClientResponse<HentSnapshot.Result> =
        snapshot(
            versjon, fødselsnummer, vedtaksperiodeId, utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            utbetaling = GraphQLUtbetaling(
                id = utbetalingId.toString(),
                arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
                arbeidsgiverNettoBelop = arbeidsgiverbeløp,
                personFagsystemId = "EN_FAGSYSTEMID",
                personNettoBelop = personbeløp,
                statusEnum = GraphQLUtbetalingstatus.UBETALT,
                typeEnum = Utbetalingtype.REVURDERING,
            )
        )
}
