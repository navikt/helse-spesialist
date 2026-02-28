package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OpprettEllerOppdaterInntektskilder
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.application.InMemoryArbeidsgiverRepository
import no.nav.helse.spesialist.application.InMemoryAvviksvurderingRepository
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsdato
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpprettEllerOppdaterInntektskilderTest {
    private val arbeidsgiverRepository = InMemoryArbeidsgiverRepository()
    private val avviksvurderingRepository = InMemoryAvviksvurderingRepository()

    private val observer =
        object : CommandContextObserver {
            fun reset() = behov.clear()

            val behov = mutableListOf<Behov>()

            override fun behov(behov: Behov, commandContextId: UUID) {
                this.behov.add(behov)
            }
        }

    private val context =
        CommandContext(UUID.randomUUID()).also {
            it.nyObserver(observer)
        }

    @Test
    fun `suspenderer og ber om behov ved ny ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            listOf(Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(listOf(organisasjonsnummer.organisasjonsnummer))),
            observer.behov.toList()
        )
    }

    @Test
    fun `suspenderer og ber om behov ved ny ENK inntektskilde`() {
        val fødselsnummer = lagFødselsnummerIdentifikator()
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            listOf(Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(listOf(fødselsnummer.fødselsnummer))),
            observer.behov.toList()
        )
    }

    @Test
    fun `suspenderer og ber om behov ved komplett, men utdatert ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            listOf(Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(listOf(organisasjonsnummer.organisasjonsnummer))),
            observer.behov.toList()
        )
    }

    @Test
    fun `suspenderer og ber om behov ved komplett, men utdatert ENK inntektskilde`() {
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            listOf(Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(listOf(fødselsnummer.fødselsnummer))),
            observer.behov.toList()
        )
    }

    @Test
    fun `suspenderer ikke og ber ikke om behov ved komplett, oppdatert ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreOppdatertArbeidsgiver(organisasjonsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `suspenderer ikke og ber ikke om behov ved komplett, oppdatert ENK inntektskilde`() {
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreOppdatertArbeidsgiver(fødselsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `suspenderer ikke og ber ikke om behov om inntektskilde er SELVSTENDIG`() {
        val organisasjonsnummer = ArbeidsgiverIdentifikator.Organisasjonsnummer("SELVSTENDIG")
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `suspenderer og ber om behov ved en kombinasjon av ulike ORDINÆRE inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummerIdentifikator()
        lagreOppdatertArbeidsgiver(organisasjonsnummer1)
        val organisasjonsnummer2 = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer2)
        val organisasjonsnummer3 = lagOrganisasjonsnummerIdentifikator()
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(
                    organisasjonsnummer1.organisasjonsnummer,
                    organisasjonsnummer2.organisasjonsnummer,
                    organisasjonsnummer3.organisasjonsnummer
                ),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val actualBehovList = observer.behov.toList()
        assertEquals(1, actualBehovList.size)
        assertEquals(Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver::class.java, actualBehovList.first()::class.java)
        assertEquals(
            listOf(
                organisasjonsnummer2.organisasjonsnummer,
                organisasjonsnummer3.organisasjonsnummer
            ).sorted(),
            (actualBehovList.first() as Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver).organisasjonsnumre.sorted()
        )
    }

    @Test
    fun `suspenderer og ber om behov ved en kombinasjon av ulike ENK inntektskilder`() {
        val fødselsnummer1 = lagFødselsnummerIdentifikator()
        lagreOppdatertArbeidsgiver(fødselsnummer1)
        val fødselsnummer2 = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer2)
        val fødselsnummer3 = lagFødselsnummerIdentifikator()
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(
                    fødselsnummer1.fødselsnummer,
                    fødselsnummer2.fødselsnummer,
                    fødselsnummer3.fødselsnummer
                ),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val actualBehovList = observer.behov.toList()
        assertEquals(1, actualBehovList.size)
        assertEquals(Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak::class.java, actualBehovList.first()::class.java)
        assertEquals(
            listOf(
                fødselsnummer2.fødselsnummer,
                fødselsnummer3.fødselsnummer
            ).sorted(),
            (actualBehovList.first() as Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak).identer.sorted()
        )
    }

    @Test
    fun `suspenderer og ber om behov ved både ORDINÆR og ENK inntektskilder`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer)
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer, fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            setOf(
                Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(listOf(organisasjonsnummer.organisasjonsnummer)),
                Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(listOf(fødselsnummer.fødselsnummer))
            ), observer.behov.toSet()
        )
    }

    @Test
    fun `mottar ORDINÆR løsning og lagrer endring`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )
        command.execute(context)

        val arbeidsgivernavn = lagOrganisasjonsnavn()
        context.add(
            lagArbeidsgiverinformasjonløsning(
                orgnummer = organisasjonsnummer.organisasjonsnummer,
                navn = arbeidsgivernavn
            )
        )

        val ferdig = command.resume(context)
        assertTrue(ferdig)

        assertEquals(1, arbeidsgiverRepository.alle().size)
        arbeidsgiverRepository.alle().single().assertArbeidsgiver(
            forventetIdentifikator = organisasjonsnummer,
            forventetNavn = arbeidsgivernavn
        )
    }

    @Test
    fun `mottar ENK løsning og lagrer endring`() {
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer)
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        command.execute(context)

        val fornavn = lagFornavn()
        val etternavn = lagEtternavn()
        context.add(
            lagPersoninfoløsninger(
                ident = fødselsnummer.fødselsnummer,
                fornavn = fornavn,
                etternavn = etternavn
            )
        )

        val ferdig = command.resume(context)
        assertTrue(ferdig)

        assertEquals(1, arbeidsgiverRepository.alle().size)
        arbeidsgiverRepository.alle().single().assertArbeidsgiver(
            forventetIdentifikator = fødselsnummer,
            forventetNavn = "$fornavn $etternavn"
        )
    }

    @Test
    fun `mottar ORDINÆR og ENK løsning og lagrer endring`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer)
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer)

        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer, fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )
        command.execute(context)

        val arbeidsgivernavn = lagOrganisasjonsnavn()
        context.add(
            lagArbeidsgiverinformasjonløsning(
                orgnummer = organisasjonsnummer.organisasjonsnummer, navn = arbeidsgivernavn
            )
        )

        val fornavn = lagFornavn()
        val etternavn = lagEtternavn()
        context.add(
            lagPersoninfoløsninger(
                ident = fødselsnummer.fødselsnummer, fornavn = fornavn,
                etternavn = etternavn
            )
        )

        val ferdig = command.resume(context)
        assertTrue(ferdig)
        assertEquals(2, arbeidsgiverRepository.alle().size)
        arbeidsgiverRepository.finn(organisasjonsnummer).assertArbeidsgiver(
            forventetIdentifikator = organisasjonsnummer,
            forventetNavn = arbeidsgivernavn
        )
        arbeidsgiverRepository.finn(fødselsnummer).assertArbeidsgiver(
            forventetIdentifikator = fødselsnummer,
            forventetNavn = "$fornavn $etternavn"
        )
    }

    @Test
    fun `ber om nytt behov dersom løsning ikke inneholder løsning for alle inntektskilder som trenger det`() {
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(organisasjonsnummer)
        val fødselsnummer = lagFødselsnummerIdentifikator()
        lagreUtdatertArbeidsgiver(fødselsnummer)

        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = setOf(organisasjonsnummer.organisasjonsnummer, fødselsnummer.fødselsnummer),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )
        command.execute(context)

        observer.reset()

        val arbeidsgivernavn = lagOrganisasjonsnavn()
        context.add(
            lagArbeidsgiverinformasjonløsning(
                orgnummer = organisasjonsnummer.organisasjonsnummer,
                navn = arbeidsgivernavn
            )
        )

        val ferdig = command.resume(context)
        assertFalse(ferdig)

        assertEquals(2, arbeidsgiverRepository.alle().size)
        arbeidsgiverRepository.finn(organisasjonsnummer).assertArbeidsgiver(
            forventetIdentifikator = organisasjonsnummer,
            forventetNavn = arbeidsgivernavn
        )

        assertEquals(
            setOf(Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(listOf(fødselsnummer.fødselsnummer))),
            observer.behov.toSet()
        )
    }

    @Test
    fun `Får med arbeidsgivere som finnes i sammenligningsgrunnlag`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()

        avviksvurderingRepository.lagre(
            Avviksvurdering(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    totalbeløp = 600_000.0,
                    innrapporterteInntekter = listOf(InnrapportertInntekt(
                        arbeidsgiverreferanse = organisasjonsnummer.organisasjonsnummer,
                        inntekter = emptyList()
                    ))
                ),
                beregningsgrunnlag = Beregningsgrunnlag(600_000.0, emptyList())
            )
        )
        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = fødselsnummer,
                identifikatorer = emptySet(),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        assertEquals(
            listOf(Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(listOf(organisasjonsnummer.organisasjonsnummer))),
            observer.behov.toList()
        )
    }

    @Test
    fun `Hvis en inntektskilde i sammenligningsgrunnlaget finnes fra før får vi tilbake en eksisterende inntektskilde`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummerIdentifikator()

        avviksvurderingRepository.lagre(
            Avviksvurdering(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    600_000.0, listOf(InnrapportertInntekt(organisasjonsnummer.organisasjonsnummer, emptyList()))
                ),
                beregningsgrunnlag = Beregningsgrunnlag(600_000.0, emptyList())
            )
        )
        lagreOppdatertArbeidsgiver(organisasjonsnummer)

        val command =
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = lagFødselsnummer(),
                identifikatorer = emptySet(),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertEquals(emptyList<Behov>(), observer.behov.toList())
    }

    private fun lagArbeidsgiverinformasjonløsning(
        orgnummer: String,
        navn: String
    ): Arbeidsgiverinformasjonløsning =
        Arbeidsgiverinformasjonløsning(
            arbeidsgivere = listOf(
                Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(
                    orgnummer = orgnummer,
                    navn = navn,
                ),
            )
        )

    private fun lagPersoninfoløsninger(
        ident: String,
        fornavn: String,
        etternavn: String
    ): HentPersoninfoløsninger =
        HentPersoninfoløsninger(
            løsninger = listOf(
                HentPersoninfoløsning(
                    ident = ident,
                    fornavn = fornavn,
                    mellomnavn = null,
                    etternavn = etternavn,
                    fødselsdato = lagFødselsdato(),
                    kjønn = Kjønn.Kvinne,
                    adressebeskyttelse = Adressebeskyttelse.Ugradert,
                ),
            ),
        )

    private fun Arbeidsgiver?.assertArbeidsgiver(
        forventetIdentifikator: ArbeidsgiverIdentifikator,
        forventetNavn: String,
    ) {
        assertNotNull(this)
        assertEquals(forventetIdentifikator, this.id)
        assertEquals(forventetNavn, this.navn.navn)
    }

    private fun lagreOppdatertArbeidsgiver(identifikator: ArbeidsgiverIdentifikator) {
        lagreArbeidsgiver(
            identifikator = identifikator,
            navnSistOppdatertDato = LocalDate.now()
        )
    }

    private fun lagreUtdatertArbeidsgiver(identifikator: ArbeidsgiverIdentifikator) {
        lagreArbeidsgiver(
            identifikator = identifikator,
            navnSistOppdatertDato = LocalDate.now().minusDays(15L)
        )
    }

    private fun lagreArbeidsgiver(identifikator: ArbeidsgiverIdentifikator, navnSistOppdatertDato: LocalDate) {
        arbeidsgiverRepository.lagre(
            Arbeidsgiver.Factory.fraLagring(
                id = identifikator,
                navn = Arbeidsgiver.Navn(
                    navn = "et navn",
                    sistOppdatertDato = navnSistOppdatertDato,
                ),
            )
        )
    }

    private fun lagFødselsnummerIdentifikator() =
        ArbeidsgiverIdentifikator.Fødselsnummer(lagFødselsnummer())

    private fun lagOrganisasjonsnummerIdentifikator() =
        ArbeidsgiverIdentifikator.Organisasjonsnummer(lagOrganisasjonsnummer())
}
