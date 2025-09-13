package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.KommentarFraDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.PaVentInfoFraDatabase
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiMottaker
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveMapperTest {
    private companion object {
        private const val oppgaveId = 1L
        private val aktørId = lagAktørId()
        private val vedtaksperiodeId = UUID.randomUUID()
        private val saksbehandler =
            Saksbehandler(
                id = SaksbehandlerOid(UUID.randomUUID()),
                navn = "navn",
                epost = "epost",
                ident = "ident"
            )
        private val opprettet = LocalDateTime.now()
        private val opprinneligSøknadsdato = LocalDateTime.now()
        private val tidsfrist = LocalDate.now()
    }

    private val navn = Triple("fornavn", "mellomnavn", "etternavn")

    @Test
    fun `map OppgaveFraDatabaseForVisning til OppgaveTilBehandling (api)`() {
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(
                    EgenskapForDatabase.SØKNAD,
                    EgenskapForDatabase.DELVIS_REFUSJON,
                    EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                    EgenskapForDatabase.EN_ARBEIDSGIVER
                ),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = tidsfrist,
                filtrertAntall = 1,
                paVentInfo =
                    PaVentInfoFraDatabase(
                        årsaker = listOf("årsak"),
                        tekst = "tekst",
                        dialogRef = 1L,
                        saksbehandler = saksbehandler.ident,
                        opprettet = opprettet,
                        tidsfrist = tidsfrist,
                        kommentarer =
                            listOf(
                                KommentarFraDatabase(
                                    id = 1,
                                    tekst = "kommentar",
                                    opprettet = opprettet,
                                    saksbehandlerident = saksbehandler.ident,
                                ),
                            ),
                    ),
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(oppgaveId.toString(), oppgaveTilBehandling.id)
        assertEquals(aktørId, oppgaveTilBehandling.aktorId)
        assertEquals(vedtaksperiodeId, oppgaveTilBehandling.vedtaksperiodeId)
        assertEquals(ApiPersonnavn("fornavn", "etternavn", "mellomnavn"), oppgaveTilBehandling.navn)
        assertEquals(opprettet, oppgaveTilBehandling.opprettet)
        assertEquals(opprinneligSøknadsdato, oppgaveTilBehandling.opprinneligSoknadsdato)
        assertEquals(tidsfrist, oppgaveTilBehandling.tidsfrist)
        assertEquals(ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD, oppgaveTilBehandling.antallArbeidsforhold)
        assertEquals(ApiMottaker.BEGGE, oppgaveTilBehandling.mottaker)
        assertEquals(ApiOppgavetype.SOKNAD, oppgaveTilBehandling.oppgavetype)
        assertEquals(ApiPeriodetype.FORSTEGANGSBEHANDLING, oppgaveTilBehandling.periodetype)
        assertEquals(
            setOf(
                ApiOppgaveegenskap(ApiEgenskap.SOKNAD, ApiKategori.Oppgavetype),
                ApiOppgaveegenskap(ApiEgenskap.FORSTEGANGSBEHANDLING, ApiKategori.Periodetype),
                ApiOppgaveegenskap(ApiEgenskap.EN_ARBEIDSGIVER, ApiKategori.Inntektskilde),
                ApiOppgaveegenskap(ApiEgenskap.DELVIS_REFUSJON, ApiKategori.Mottaker),
            ),
            oppgaveTilBehandling.egenskaper.toSet(),
        )
        assertEquals(listOf("årsak"), oppgaveTilBehandling.paVentInfo?.arsaker)
        assertEquals("tekst", oppgaveTilBehandling.paVentInfo?.tekst)
        assertEquals(1, oppgaveTilBehandling.paVentInfo?.dialogRef)
        assertEquals(saksbehandler.ident, oppgaveTilBehandling.paVentInfo?.saksbehandler)
        assertEquals(opprettet, oppgaveTilBehandling.paVentInfo?.opprettet)
        assertEquals(tidsfrist, oppgaveTilBehandling.paVentInfo?.tidsfrist)
        assertEquals(
            1,
            oppgaveTilBehandling.paVentInfo
                ?.kommentarer
                ?.first()
                ?.id,
        )
        assertEquals(
            opprettet,
            oppgaveTilBehandling.paVentInfo
                ?.kommentarer
                ?.first()
                ?.opprettet,
        )
        assertEquals(
            "kommentar",
            oppgaveTilBehandling.paVentInfo
                ?.kommentarer
                ?.first()
                ?.tekst,
        )
        assertEquals(
            saksbehandler.ident,
            oppgaveTilBehandling.paVentInfo
                ?.kommentarer
                ?.first()
                ?.saksbehandlerident,
        )
    }

    @ParameterizedTest
    @EnumSource(
        names = ["FORLENGELSE", "FORSTEGANGSBEHANDLING", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `map til periodetype`(egenskapSomMapperTilPeriodetype: EgenskapForDatabase) {
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(
                    egenskapSomMapperTilPeriodetype,
                    EgenskapForDatabase.DELVIS_REFUSJON,
                    EgenskapForDatabase.SØKNAD,
                    EgenskapForDatabase.EN_ARBEIDSGIVER
                ),
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(
            enumValueOf<ApiPeriodetype>(egenskapSomMapperTilPeriodetype.name),
            oppgaveTilBehandling.periodetype
        )
    }

    @ParameterizedTest
    @EnumSource(
        names = ["FORLENGELSE", "FORSTEGANGSBEHANDLING", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `map til periodetype kaster exception når det mangler en egenskap som er periodetype`(
        egenskapSomMapperTilPeriodetype: EgenskapForDatabase,
    ) {
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(
                    egenskapSomMapperTilPeriodetype,
                    EgenskapForDatabase.DELVIS_REFUSJON,
                    EgenskapForDatabase.SØKNAD,
                    EgenskapForDatabase.EN_ARBEIDSGIVER
                ),
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        assertThrows<NoSuchElementException> {
            listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["SØKNAD", "REVURDERING"], mode = EnumSource.Mode.INCLUDE)
    fun `map til oppgavetype`(egenskapSomMapperTilOppgavetype: EgenskapForDatabase) {
        val map =
            mapOf(
                EgenskapForDatabase.SØKNAD to ApiOppgavetype.SOKNAD,
                EgenskapForDatabase.REVURDERING to ApiOppgavetype.REVURDERING,
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper =
                    setOf(
                        egenskapSomMapperTilOppgavetype,
                        EgenskapForDatabase.DELVIS_REFUSJON,
                        EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                        EgenskapForDatabase.EN_ARBEIDSGIVER,
                    ),
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(map[egenskapSomMapperTilOppgavetype], oppgaveTilBehandling.oppgavetype)
    }

    @ParameterizedTest
    @EnumSource(names = ["SØKNAD", "REVURDERING"], mode = EnumSource.Mode.EXCLUDE)
    fun `map til oppgavetype kaster exception når det mangler en egenskap som er oppgavetype`(
        egenskapSomIkkeMapperTilOppgavetype: EgenskapForDatabase,
    ) {
        // Må gjøres fordi vi skal ha kun én av disse egenskapene pr. kategori.
        val andreEgenskaper =
            setOfNotNull(
                if (egenskapSomIkkeMapperTilOppgavetype in
                    listOf(
                        EgenskapForDatabase.INFOTRYGDFORLENGELSE,
                        EgenskapForDatabase.FORLENGELSE,
                        EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                        EgenskapForDatabase.OVERGANG_FRA_IT,
                    )
                ) {
                    null
                } else {
                    EgenskapForDatabase.FORSTEGANGSBEHANDLING
                },
                if (egenskapSomIkkeMapperTilOppgavetype in
                    listOf(
                        EgenskapForDatabase.DELVIS_REFUSJON,
                        EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
                        EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER,
                        EgenskapForDatabase.INGEN_UTBETALING,
                    )
                ) {
                    null
                } else {
                    EgenskapForDatabase.DELVIS_REFUSJON
                },
                if (egenskapSomIkkeMapperTilOppgavetype in
                    listOf(
                        EgenskapForDatabase.EN_ARBEIDSGIVER,
                        EgenskapForDatabase.FLERE_ARBEIDSGIVERE,
                    )
                ) {
                    null
                } else {
                    EgenskapForDatabase.EN_ARBEIDSGIVER
                },
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = andreEgenskaper + egenskapSomIkkeMapperTilOppgavetype,
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        assertThrows<NoSuchElementException> {
            listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["EN_ARBEIDSGIVER", "FLERE_ARBEIDSGIVERE"], mode = EnumSource.Mode.INCLUDE)
    fun `map til antallArbeidsforhold`(egenskapSomMapperTilAntallArbeidsforhold: EgenskapForDatabase) {
        val map =
            mapOf(
                EgenskapForDatabase.EN_ARBEIDSGIVER to ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD,
                EgenskapForDatabase.FLERE_ARBEIDSGIVERE to ApiAntallArbeidsforhold.FLERE_ARBEIDSFORHOLD,
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper =
                    setOf(
                        egenskapSomMapperTilAntallArbeidsforhold,
                        EgenskapForDatabase.DELVIS_REFUSJON,
                        EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                        EgenskapForDatabase.SØKNAD,
                    ),
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(
            map[egenskapSomMapperTilAntallArbeidsforhold],
            oppgaveTilBehandling.antallArbeidsforhold
        )
    }

    @ParameterizedTest
    @EnumSource(names = ["EN_ARBEIDSGIVER", "FLERE_ARBEIDSGIVERE"], mode = EnumSource.Mode.EXCLUDE)
    fun `map til antallArbeidsforhold kaster exception når det mangler en egenskap som er antallArbeidsforhold`(
        egenskapSomIkkeMapperTilAntallArbeidsforhold: EgenskapForDatabase,
    ) {
        // Må gjøres fordi vi skal ha kun én av disse egenskapene pr. kategori.
        val andreEgenskaper =
            setOfNotNull(
                if (egenskapSomIkkeMapperTilAntallArbeidsforhold in
                    listOf(
                        EgenskapForDatabase.INFOTRYGDFORLENGELSE,
                        EgenskapForDatabase.FORLENGELSE,
                        EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                        EgenskapForDatabase.OVERGANG_FRA_IT,
                    )
                ) {
                    null
                } else {
                    EgenskapForDatabase.FORSTEGANGSBEHANDLING
                },
                if (egenskapSomIkkeMapperTilAntallArbeidsforhold in
                    listOf(
                        EgenskapForDatabase.DELVIS_REFUSJON,
                        EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
                        EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER,
                        EgenskapForDatabase.INGEN_UTBETALING,
                    )
                ) {
                    null
                } else {
                    EgenskapForDatabase.DELVIS_REFUSJON
                },
                if (egenskapSomIkkeMapperTilAntallArbeidsforhold in listOf(
                        EgenskapForDatabase.SØKNAD,
                        EgenskapForDatabase.REVURDERING
                    )) null else EgenskapForDatabase.SØKNAD,
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = andreEgenskaper + egenskapSomIkkeMapperTilAntallArbeidsforhold,
                tildelt = saksbehandler,
                påVent = false,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
                paVentInfo = null,
            )
        assertThrows<NoSuchElementException> {
            listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        }
    }

    @Test
    fun `map EgenskapForDatabase til OppgaveEgenskap (api)`() {
        val egenskaperForDatabase = setOf(
            EgenskapForDatabase.SØKNAD,
            EgenskapForDatabase.DELVIS_REFUSJON,
            EgenskapForDatabase.FORSTEGANGSBEHANDLING,
            EgenskapForDatabase.EN_ARBEIDSGIVER
        )
        val oppgaveEgenskaper = egenskaperForDatabase.tilEgenskaperForVisning()

        assertEquals(
            setOf(
                ApiOppgaveegenskap(ApiEgenskap.SOKNAD, ApiKategori.Oppgavetype),
                ApiOppgaveegenskap(ApiEgenskap.FORSTEGANGSBEHANDLING, ApiKategori.Periodetype),
                ApiOppgaveegenskap(ApiEgenskap.EN_ARBEIDSGIVER, ApiKategori.Inntektskilde),
                ApiOppgaveegenskap(ApiEgenskap.DELVIS_REFUSJON, ApiKategori.Mottaker),
            ),
            oppgaveEgenskaper.toSet(),
        )
    }
}
