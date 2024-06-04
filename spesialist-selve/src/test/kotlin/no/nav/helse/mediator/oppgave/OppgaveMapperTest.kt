package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.EgenskapForDatabase.DELVIS_REFUSJON
import no.nav.helse.db.EgenskapForDatabase.EN_ARBEIDSGIVER
import no.nav.helse.db.EgenskapForDatabase.FLERE_ARBEIDSGIVERE
import no.nav.helse.db.EgenskapForDatabase.FORLENGELSE
import no.nav.helse.db.EgenskapForDatabase.FORSTEGANGSBEHANDLING
import no.nav.helse.db.EgenskapForDatabase.INFOTRYGDFORLENGELSE
import no.nav.helse.db.EgenskapForDatabase.INGEN_UTBETALING
import no.nav.helse.db.EgenskapForDatabase.OVERGANG_FRA_IT
import no.nav.helse.db.EgenskapForDatabase.REVURDERING
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.db.EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.db.EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
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
        private const val aktørId = "1234567891011"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val saksbehandler = SaksbehandlerFraDatabase("epost", UUID.randomUUID(), "navn", "ident")
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
                egenskaper = setOf(SØKNAD, DELVIS_REFUSJON, FORSTEGANGSBEHANDLING, EN_ARBEIDSGIVER),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = tidsfrist,
                filtrertAntall = 1,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(oppgaveId.toString(), oppgaveTilBehandling.id)
        assertEquals(aktørId, oppgaveTilBehandling.aktorId)
        assertEquals(vedtaksperiodeId, oppgaveTilBehandling.vedtaksperiodeId)
        assertEquals(Personnavn("fornavn", "etternavn", "mellomnavn"), oppgaveTilBehandling.navn)
        assertEquals(opprettet, oppgaveTilBehandling.opprettet)
        assertEquals(opprinneligSøknadsdato, oppgaveTilBehandling.opprinneligSoknadsdato)
        assertEquals(tidsfrist, oppgaveTilBehandling.tidsfrist)
        assertEquals(AntallArbeidsforhold.ET_ARBEIDSFORHOLD, oppgaveTilBehandling.antallArbeidsforhold)
        assertEquals(Mottaker.BEGGE, oppgaveTilBehandling.mottaker)
        assertEquals(Oppgavetype.SOKNAD, oppgaveTilBehandling.oppgavetype)
        assertEquals(Periodetype.FORSTEGANGSBEHANDLING, oppgaveTilBehandling.periodetype)
        assertEquals(
            setOf(
                Oppgaveegenskap(Egenskap.SOKNAD, Kategori.Oppgavetype),
                Oppgaveegenskap(Egenskap.FORSTEGANGSBEHANDLING, Kategori.Periodetype),
                Oppgaveegenskap(Egenskap.EN_ARBEIDSGIVER, Kategori.Inntektskilde),
                Oppgaveegenskap(Egenskap.DELVIS_REFUSJON, Kategori.Mottaker),
            ),
            oppgaveTilBehandling.egenskaper.toSet(),
        )
    }

    @ParameterizedTest
    @EnumSource(names = ["FORLENGELSE", "FORSTEGANGSBEHANDLING", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT"], mode = EnumSource.Mode.INCLUDE)
    fun `map til periodetype`(egenskapSomMapperTilPeriodetype: EgenskapForDatabase) {
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(egenskapSomMapperTilPeriodetype, DELVIS_REFUSJON, SØKNAD, EN_ARBEIDSGIVER),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(enumValueOf<Periodetype>(egenskapSomMapperTilPeriodetype.name), oppgaveTilBehandling.periodetype)
    }

    @ParameterizedTest
    @EnumSource(names = ["FORLENGELSE", "FORSTEGANGSBEHANDLING", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT"], mode = EnumSource.Mode.EXCLUDE)
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
                egenskaper = setOf(egenskapSomMapperTilPeriodetype, DELVIS_REFUSJON, SØKNAD, EN_ARBEIDSGIVER),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
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
                SØKNAD to Oppgavetype.SOKNAD,
                REVURDERING to Oppgavetype.REVURDERING,
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(egenskapSomMapperTilOppgavetype, DELVIS_REFUSJON, FORSTEGANGSBEHANDLING, EN_ARBEIDSGIVER),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
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
                        INFOTRYGDFORLENGELSE,
                        FORLENGELSE,
                        FORSTEGANGSBEHANDLING,
                        OVERGANG_FRA_IT,
                    )
                ) {
                    null
                } else {
                    FORSTEGANGSBEHANDLING
                },
                if (egenskapSomIkkeMapperTilOppgavetype in
                    listOf(
                        DELVIS_REFUSJON,
                        UTBETALING_TIL_SYKMELDT,
                        UTBETALING_TIL_ARBEIDSGIVER,
                        INGEN_UTBETALING,
                    )
                ) {
                    null
                } else {
                    DELVIS_REFUSJON
                },
                if (egenskapSomIkkeMapperTilOppgavetype in listOf(EN_ARBEIDSGIVER, FLERE_ARBEIDSGIVERE)) null else EN_ARBEIDSGIVER,
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
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
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
                EN_ARBEIDSGIVER to AntallArbeidsforhold.ET_ARBEIDSFORHOLD,
                FLERE_ARBEIDSGIVERE to AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD,
            )
        val (fornavn, mellomnavn, etternavn) = navn
        val oppgaveFraDatabaseForVisning =
            OppgaveFraDatabaseForVisning(
                id = oppgaveId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                navn = PersonnavnFraDatabase(fornavn, mellomnavn, etternavn),
                egenskaper = setOf(egenskapSomMapperTilAntallArbeidsforhold, DELVIS_REFUSJON, FORSTEGANGSBEHANDLING, SØKNAD),
                tildelt = saksbehandler,
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
            )
        val oppgaverTilBehandling = listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        assertEquals(1, oppgaverTilBehandling.size)
        val oppgaveTilBehandling = oppgaverTilBehandling.single()
        assertEquals(map[egenskapSomMapperTilAntallArbeidsforhold], oppgaveTilBehandling.antallArbeidsforhold)
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
                        INFOTRYGDFORLENGELSE,
                        FORLENGELSE,
                        FORSTEGANGSBEHANDLING,
                        OVERGANG_FRA_IT,
                    )
                ) {
                    null
                } else {
                    FORSTEGANGSBEHANDLING
                },
                if (egenskapSomIkkeMapperTilAntallArbeidsforhold in
                    listOf(
                        DELVIS_REFUSJON,
                        UTBETALING_TIL_SYKMELDT,
                        UTBETALING_TIL_ARBEIDSGIVER,
                        INGEN_UTBETALING,
                    )
                ) {
                    null
                } else {
                    DELVIS_REFUSJON
                },
                if (egenskapSomIkkeMapperTilAntallArbeidsforhold in listOf(SØKNAD, REVURDERING)) null else SØKNAD,
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
                påVent = true,
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                tidsfrist = null,
                filtrertAntall = 1,
            )
        assertThrows<NoSuchElementException> {
            listOf(oppgaveFraDatabaseForVisning).tilOppgaverTilBehandling()
        }
    }

    @Test
    fun `map EgenskapForDatabase til OppgaveEgenskap (api)`() {
        val egenskaperForDatabase = setOf(SØKNAD, DELVIS_REFUSJON, FORSTEGANGSBEHANDLING, EN_ARBEIDSGIVER)
        val oppgaveEgenskaper = egenskaperForDatabase.tilEgenskaperForVisning()

        assertEquals(
            setOf(
                Oppgaveegenskap(Egenskap.SOKNAD, Kategori.Oppgavetype),
                Oppgaveegenskap(Egenskap.FORSTEGANGSBEHANDLING, Kategori.Periodetype),
                Oppgaveegenskap(Egenskap.EN_ARBEIDSGIVER, Kategori.Inntektskilde),
                Oppgaveegenskap(Egenskap.DELVIS_REFUSJON, Kategori.Mottaker),
            ),
            oppgaveEgenskaper.toSet(),
        )
    }
}
