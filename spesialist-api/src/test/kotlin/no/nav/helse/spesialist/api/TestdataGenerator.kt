package no.nav.helse.spesialist.api

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import kotlin.random.Random

internal object TestdataGenerator {

    private const val ANDEL_TILDELTE = 0.15
    private const val ANDEL_PÅ_VENT = 0.3

    private val saksbehandlere = List(10) {
        tilfeldigSaksbehandler()
    }

    private val ukategoriserteOppgaveegenskaper get() = mutableListOf<Oppgaveegenskap>().apply {
        if (Random.nextFloat() > 0.9) {
            if (Random.nextFloat() > 0.2) add(Oppgaveegenskap(Egenskap.BESLUTTER, Kategori.Ukategorisert))
            else add(Oppgaveegenskap(Egenskap.RETUR, Kategori.Ukategorisert))
        }

        if (Random.nextFloat() > 0.85) {
            if (Random.nextFloat() > 0.1) add(Oppgaveegenskap(Egenskap.RISK_QA, Kategori.Ukategorisert))
            else add(Oppgaveegenskap(Egenskap.STIKKPROVE, Kategori.Ukategorisert))
        }

        if (Random.nextFloat() > 0.95) {
            if (Random.nextFloat() > 0.5) add(Oppgaveegenskap(Egenskap.VERGEMAL, Kategori.Ukategorisert))
            else add(Oppgaveegenskap(Egenskap.FULLMAKT, Kategori.Ukategorisert))
        }

        if (Random.nextFloat() > 0.95) add(Oppgaveegenskap(Egenskap.EGEN_ANSATT, Kategori.Ukategorisert))
        if (Random.nextFloat() > 0.95) add(Oppgaveegenskap(Egenskap.FORTROLIG_ADRESSE, Kategori.Ukategorisert))
        if (Random.nextFloat() > 0.8) add(Oppgaveegenskap(Egenskap.HASTER, Kategori.Ukategorisert))
        if (Random.nextFloat() > 0.9) add(Oppgaveegenskap(Egenskap.UTLAND, Kategori.Ukategorisert))
        if (Random.nextFloat() > 0.97) add(Oppgaveegenskap(Egenskap.SPESIALSAK, Kategori.Ukategorisert))
    }

    internal fun oppgave(): OppgaveTilBehandling {
        val tildelt = Random.nextFloat() <= ANDEL_TILDELTE
        val tildeltTil = if (tildelt) saksbehandlere.random() else null
        val (periodetypeegenskap, periodetype) = periodetype()
        val (oppgavetypeegenskap, oppgavetype) = oppgavetype()
        val (mottakeregenskap, mottaker) = mottaker()
        val (antallArbeidsforholdegenskap, antallArbeidsforhold) = antallArbeidsforhold()
        return OppgaveTilBehandling(
            id = Random.nextLong().toString(),
            opprettet = LocalDateTime.now().toString(),
            opprinneligSoknadsdato = LocalDateTime.now().toString(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            navn = tilfeldigNavn(),
            aktorId = tilfeldigAktørId(),
            tildeling = tildeltTil?.let { Tildeling(it.navn, it.epostadresse, it.oid.toString(), Random.nextFloat() <= ANDEL_PÅ_VENT) },
            egenskaper = listOf(periodetypeegenskap, oppgavetypeegenskap, mottakeregenskap, antallArbeidsforholdegenskap) + tilfeldigeUkategoriserteEgenskaper(),
            periodetype = periodetype,
            oppgavetype = oppgavetype,
            mottaker = mottaker,
            antallArbeidsforhold = antallArbeidsforhold
        )
    }

    private fun periodetype(): Pair<Oppgaveegenskap, Periodetype> {
        val periodetyper = listOf(Periodetype.FORSTEGANGSBEHANDLING, Periodetype.FORLENGELSE, Periodetype.INFOTRYGDFORLENGELSE, Periodetype.OVERGANG_FRA_IT)
        val periodetype = periodetyper.random()
        val egenskap = when (periodetype) {
            Periodetype.FORLENGELSE -> Egenskap.FORLENGELSE
            Periodetype.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
            Periodetype.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
            Periodetype.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
        }
        return Oppgaveegenskap(egenskap, Kategori.Periodetype) to periodetype
    }

    private fun oppgavetype(): Pair<Oppgaveegenskap, Oppgavetype> {
        val oppgavetyper = listOf(Oppgavetype.SOKNAD, Oppgavetype.REVURDERING)
        val oppgavetype = oppgavetyper.random()
        val egenskap = when (oppgavetype) {
            Oppgavetype.SOKNAD -> Egenskap.SOKNAD
            Oppgavetype.REVURDERING -> Egenskap.REVURDERING
            else -> throw IllegalArgumentException()
        }
        return Oppgaveegenskap(egenskap, Kategori.Oppgavetype) to oppgavetype
    }

    private fun mottaker(): Pair<Oppgaveegenskap, Mottaker> {
        val mottakere = listOf(Mottaker.BEGGE, Mottaker.ARBEIDSGIVER, Mottaker.SYKMELDT, Mottaker.INGEN)
        val mottaker = mottakere.random()
        val egenskap = when (mottaker) {
            Mottaker.SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
            Mottaker.ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            Mottaker.BEGGE -> Egenskap.DELVIS_REFUSJON
            Mottaker.INGEN -> Egenskap.INGEN_UTBETALING
        }
        return Oppgaveegenskap(egenskap, Kategori.Mottaker) to mottaker
    }

    private fun tilfeldigeUkategoriserteEgenskaper(): List<Oppgaveegenskap> = ukategoriserteOppgaveegenskaper

    private fun antallArbeidsforhold(): Pair<Oppgaveegenskap, AntallArbeidsforhold> {
        val arbeidsforhold = listOf(AntallArbeidsforhold.ET_ARBEIDSFORHOLD, AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD)
        val antallArbeidsforhold = arbeidsforhold.random()
        val egenskap = when (antallArbeidsforhold) {
            AntallArbeidsforhold.ET_ARBEIDSFORHOLD -> Egenskap.EN_ARBEIDSGIVER
            AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD -> Egenskap.FLERE_ARBEIDSGIVERE
        }
        return Oppgaveegenskap(egenskap, Kategori.Inntektskilde) to antallArbeidsforhold
    }

    private fun tilfeldigNavn(): Personnavn {
        val fornavn = listOf("Skitten", "Kul", "Snill", "Lat", "Perfekt", "Enorm", "Ekkel")
        val mellomnavn = listOf("Rar", "Teit", "Spesiell", "Fantastisk", "Tullete", "Fabelaktig", null, null, null)
        val etternavn = listOf("Paraply", "Frosk", "Fugl", "Flaske", "Vindu", "Pensjonist", "Sykkel")

        return Personnavn(
            fornavn = fornavn.random(),
            mellomnavn = mellomnavn.random(),
            etternavn = etternavn.random()
        )
    }

    private fun tilfeldigAktørId(): String {
        return "100000" + "${Random.nextInt(1000000, 9999999)}"
    }

    private class TestSaksbehandler(
        val oid: UUID,
        val epostadresse: String,
        val navn: String,
    )

    private fun tilfeldigSaksbehandler(): TestSaksbehandler {
        val saksbehandlerNavn = tilfeldigNavn()
        val (fornavn, etternavn, mellomnavn) = saksbehandlerNavn
        return TestSaksbehandler(
            oid = UUID.randomUUID(),
            epostadresse = saksbehandlerNavn.epostadresse(),
            navn = "${etternavn}, ${fornavn}${if (mellomnavn != null) " $mellomnavn" else ""}"
        )
    }

    private fun Personnavn.epostadresse(): String {
        return buildString {
            append(fornavn.lowercase())
            append(".")
            mellomnavn?.let {
                append(it.lowercase())
                append(".")
            }
            append(etternavn.lowercase())
            append("@nav.no")
        }
    }
}