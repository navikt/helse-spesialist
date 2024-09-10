package no.nav.helse.modell.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.test.fødselsdato
import no.nav.helse.spesialist.test.lagEtternavn
import no.nav.helse.spesialist.test.lagFornavn
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnavn
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OpprettEllerOppdaterArbeidsgivereTest {
    private val repository = object : InntektskilderRepository {
        val inntektskilderSomHarBlittLagret = mutableListOf<InntektskildeDto>()
        override fun lagre(inntektskilder: List<InntektskildeDto>) {
            inntektskilderSomHarBlittLagret.addAll(inntektskilder)
        }
    }

    private val contextObserver = object : CommandContextObserver {
        fun reset() = behov.clear()
        val behov = mutableMapOf<String, Map<String, Any>>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov[behov] = detaljer
        }
    }

    private val context = CommandContext(UUID.randomUUID()).also {
        it.nyObserver(contextObserver)
    }

    @Test
    fun `suspenderer og ber om behov ved ny ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(nyInntektskilde(organisasjonsnummer)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["Arbeidsgiverinformasjon"]
        assertEquals(mapOf("organisasjonsnummer" to listOf(organisasjonsnummer)), behov)
    }

    @Test
    fun `suspenderer og ber om behov ved ny ENK inntektskilde`() {
        val organisasjonsnummer = lagFødselsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(nyInntektskilde(organisasjonsnummer, Inntektskildetype.ENKELTPERSONFORETAK)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["HentPersoninfoV2"]
        assertEquals(mapOf("ident" to listOf(organisasjonsnummer)), behov)
    }

    @Test
    fun `suspenderer og ber om behov ved komplett, men utdatert ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(utdatertInntektskilde(organisasjonsnummer)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["Arbeidsgiverinformasjon"]
        assertEquals(mapOf("organisasjonsnummer" to listOf(organisasjonsnummer)), behov)
    }

    @Test
    fun `suspenderer og ber om behov ved komplett, men utdatert ENK inntektskilde`() {
        val organisasjonsnummer = lagFødselsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(utdatertInntektskilde(organisasjonsnummer, Inntektskildetype.ENKELTPERSONFORETAK)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["HentPersoninfoV2"]
        assertEquals(mapOf("ident" to listOf(organisasjonsnummer)), behov)
    }

    @Test
    fun `suspenderer ikke og ber ikke om behov ved komplett, oppdatert ORDINÆR inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(oppdatertInntektskilde(organisasjonsnummer)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertTrue(contextObserver.behov.isEmpty())
    }

    @Test
    fun `suspenderer ikke og ber ikke om behov ved komplett, oppdatert ENK inntektskilde`() {
        val organisasjonsnummer = lagFødselsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(oppdatertInntektskilde(organisasjonsnummer, Inntektskildetype.ENKELTPERSONFORETAK)),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertTrue(ferdig)
        assertTrue(contextObserver.behov.isEmpty())
    }

    @Test
    fun `suspenderer og ber om behov ved en kombinasjon av ulike ORDINÆRE inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val organisasjonsnummer3 = lagOrganisasjonsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                oppdatertInntektskilde(organisasjonsnummer1),
                utdatertInntektskilde(organisasjonsnummer2),
                nyInntektskilde(organisasjonsnummer3),
            ),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["Arbeidsgiverinformasjon"]
        assertEquals(mapOf("organisasjonsnummer" to listOf(organisasjonsnummer2, organisasjonsnummer3)), behov)
    }

    @Test
    fun `suspenderer og ber om behov ved en kombinasjon av ulike ENK inntektskilder`() {
        val organisasjonsnummer1 = lagFødselsnummer()
        val organisasjonsnummer2 = lagFødselsnummer()
        val organisasjonsnummer3 = lagFødselsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                oppdatertInntektskilde(organisasjonsnummer1, Inntektskildetype.ENKELTPERSONFORETAK),
                utdatertInntektskilde(organisasjonsnummer2, Inntektskildetype.ENKELTPERSONFORETAK),
                nyInntektskilde(organisasjonsnummer3, Inntektskildetype.ENKELTPERSONFORETAK),
            ),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val behov = contextObserver.behov["HentPersoninfoV2"]
        assertEquals(mapOf("ident" to listOf(organisasjonsnummer2, organisasjonsnummer3)), behov)
    }

    @Test
    fun `suspenderer og ber om behov ved både ORDINÆR og ENK inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagFødselsnummer()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                utdatertInntektskilde(organisasjonsnummer1, Inntektskildetype.ORDINÆR),
                utdatertInntektskilde(organisasjonsnummer2, Inntektskildetype.ENKELTPERSONFORETAK),
            ),
            inntektskilderRepository = repository,
        )

        val ferdig = command.execute(context)
        assertFalse(ferdig)
        val arbeidsgiverinformasjonbehov = contextObserver.behov["Arbeidsgiverinformasjon"]
        val personinfobehov = contextObserver.behov["HentPersoninfoV2"]
        assertEquals(mapOf("organisasjonsnummer" to listOf(organisasjonsnummer1)), arbeidsgiverinformasjonbehov)
        assertEquals(mapOf("ident" to listOf(organisasjonsnummer2)), personinfobehov)
    }

    @Test
    fun `mottar ORDINÆR løsning og lagrer endring`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val arbeidsgivernavn = lagOrganisasjonsnavn()
        val bransjer = listOf("Uteliv")
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                utdatertInntektskilde(organisasjonsnummer, Inntektskildetype.ORDINÆR),
            ),
            inntektskilderRepository = repository,
        )

        command.execute(context)
        mottaOrdinærLøsning(organisasjonsnummer, arbeidsgivernavn, bransjer)
        val ferdig = command.resume(context)
        assertTrue(ferdig)

        assertEquals(1, repository.inntektskilderSomHarBlittLagret.size)
        repository.inntektskilderSomHarBlittLagret.single().assertLagretOrdinærInntektskilde(
            forventetOrganisasjonsnummer = organisasjonsnummer,
            forventetType = InntektskildetypeDto.ORDINÆR,
            forventetNavn = arbeidsgivernavn,
            forventetBransjer = bransjer,
        )
    }

    @Test
    fun `mottar ENK løsning og lagrer endring`() {
        val organisasjonsnummer = lagFødselsnummer()
        val fornavn = lagFornavn()
        val etternavn = lagEtternavn()
        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                utdatertInntektskilde(organisasjonsnummer, Inntektskildetype.ENKELTPERSONFORETAK),
            ),
            inntektskilderRepository = repository,
        )

        command.execute(context)
        mottaEnkLøsning(organisasjonsnummer, fornavn, etternavn)
        val ferdig = command.resume(context)
        assertTrue(ferdig)

        assertEquals(1, repository.inntektskilderSomHarBlittLagret.size)
        repository.inntektskilderSomHarBlittLagret.single().assertLagretOrdinærInntektskilde(
            forventetOrganisasjonsnummer = organisasjonsnummer,
            forventetType = InntektskildetypeDto.ENKELTPERSONFORETAK,
            forventetNavn = "$fornavn $etternavn",
            forventetBransjer = emptyList(),
        )
    }

    @Test
    fun `mottar ORDINÆR og ENK løsning og lagrer endring`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val arbeidsgivernavn = lagOrganisasjonsnavn()
        val bransjer = listOf("Uteliv")

        val organisasjonsnummer2 = lagFødselsnummer()
        val fornavn = lagFornavn()
        val etternavn = lagEtternavn()

        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                utdatertInntektskilde(organisasjonsnummer1, Inntektskildetype.ORDINÆR),
                utdatertInntektskilde(organisasjonsnummer2, Inntektskildetype.ENKELTPERSONFORETAK),
            ),
            inntektskilderRepository = repository,
        )

        command.execute(context)
        mottaOrdinærLøsning(organisasjonsnummer1, arbeidsgivernavn, bransjer)
        mottaEnkLøsning(organisasjonsnummer2, fornavn, etternavn)
        val ferdig = command.resume(context)
        assertTrue(ferdig)
        assertEquals(2, repository.inntektskilderSomHarBlittLagret.size)
        repository.inntektskilderSomHarBlittLagret[0].assertLagretOrdinærInntektskilde(
            forventetOrganisasjonsnummer = organisasjonsnummer1,
            forventetType = InntektskildetypeDto.ORDINÆR,
            forventetNavn = arbeidsgivernavn,
            forventetBransjer = bransjer,
        )
        repository.inntektskilderSomHarBlittLagret[1].assertLagretOrdinærInntektskilde(
            forventetOrganisasjonsnummer = organisasjonsnummer2,
            forventetType = InntektskildetypeDto.ENKELTPERSONFORETAK,
            forventetNavn = "$fornavn $etternavn",
            forventetBransjer = emptyList(),
        )
    }

    @Test
    fun `ber om nytt behov dersom løsning ikke inneholder løsning for alle inntektskilder som trenger det`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val arbeidsgivernavn = lagOrganisasjonsnavn()
        val bransjer = listOf("Uteliv")

        val organisasjonsnummer2 = lagFødselsnummer()

        val command = OpprettEllerOppdaterArbeidsgivere(
            inntektskilder = listOf(
                utdatertInntektskilde(organisasjonsnummer1, Inntektskildetype.ORDINÆR),
                utdatertInntektskilde(organisasjonsnummer2, Inntektskildetype.ENKELTPERSONFORETAK),
            ),
            inntektskilderRepository = repository,
        )

        command.execute(context)
        contextObserver.reset()

        mottaOrdinærLøsning(organisasjonsnummer1, arbeidsgivernavn, bransjer)

        val ferdig = command.resume(context)
        assertFalse(ferdig)

        assertEquals(1, repository.inntektskilderSomHarBlittLagret.size)
        repository.inntektskilderSomHarBlittLagret[0].assertLagretOrdinærInntektskilde(
            forventetOrganisasjonsnummer = organisasjonsnummer1,
            forventetType = InntektskildetypeDto.ORDINÆR,
            forventetNavn = arbeidsgivernavn,
            forventetBransjer = bransjer,
        )

        val personinfobehov = contextObserver.behov["HentPersoninfoV2"]
        val arbeidsgiverinformasjonbehov = contextObserver.behov["Arbeidsgiverinformasjon"]
        assertNotNull(personinfobehov)
        assertNull(arbeidsgiverinformasjonbehov) // Har løsning for arbeidsgiverinformasjon, trenger ikke etterspørre denne på nytt
    }

    private fun nyInntektskilde(
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        inntektskilde: Inntektskildetype = Inntektskildetype.ORDINÆR
    ) = NyInntektskilde(organisasjonsnummer, inntektskilde)

    private fun utdatertInntektskilde(
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        inntektskilde: Inntektskildetype = Inntektskildetype.ORDINÆR
    ) = KomplettInntektskilde(organisasjonsnummer, inntektskilde, "et navn", listOf("en bransje"), LocalDate.now().minusDays(2))

    private fun oppdatertInntektskilde(
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        inntektskilde: Inntektskildetype = Inntektskildetype.ORDINÆR
    ) = KomplettInntektskilde(organisasjonsnummer, inntektskilde, "et navn", listOf("en bransje"), LocalDate.now())

    private fun mottaOrdinærLøsning(
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        navn: String = lagOrganisasjonsnavn(),
        bransjer: List<String> = emptyList(),
    ) {
        context.add(
            Arbeidsgiverinformasjonløsning(
                listOf(
                    Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(organisasjonsnummer, navn, bransjer),
                )
            )
        )
    }

    private fun mottaEnkLøsning(
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        fornavn: String = lagFornavn(),
        etternavn: String = lagEtternavn(),
    ) {
        context.add(
            HentPersoninfoløsninger(
                listOf(
                    HentPersoninfoløsning(organisasjonsnummer, fornavn, null, etternavn, fødselsdato(), Kjønn.Kvinne, Adressebeskyttelse.Ugradert),
                )
            )
        )
    }

    private fun InntektskildeDto.assertLagretOrdinærInntektskilde(
        forventetOrganisasjonsnummer: String,
        forventetType: InntektskildetypeDto,
        forventetNavn: String,
        forventetBransjer: List<String>,
    ) {
        check(this is KomplettInntektskildeDto)
        assertEquals(forventetOrganisasjonsnummer, this.organisasjonsnummer)
        assertEquals(forventetType, this.type)
        assertEquals(forventetNavn, this.navn)
        assertEquals(forventetBransjer, this.bransjer)
    }
}
