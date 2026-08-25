package no.nav.helse.spesialist.application.testfixtures

import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.IndividuellForsikring
import no.nav.helse.spesialist.application.KollektivForsikring
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import java.time.Instant
import java.time.LocalDate

fun lagForsikringsvurdering(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    samletDekning: Forsikringsvurdering.Dekning? = null,
    kollektivForsikring: KollektivForsikring? = null,
    individuelleForsikringer: List<IndividuellForsikring> = emptyList(),
    vurdertTidspunkt: Instant = Instant.parse("2018-01-01T12:00:00Z"),
): Forsikringsvurdering =
    Forsikringsvurdering(
        identitetsnummer = identitetsnummer,
        samletDekning = samletDekning,
        kollektivForsikring = kollektivForsikring,
        individuelleForsikringer = individuelleForsikringer,
        vurdertTidspunkt = vurdertTidspunkt,
    )

fun lagKollektivForsikring(
    navn: String = "100 % fra 17. dag (Kollektiv)",
    dekningFolketrygdlovenreferanse: Folketrygdlovenreferanse = lagFolketrygdlovenreferanse(),
    kollektivFolketrygdlovenreferanse: Folketrygdlovenreferanse =
        lagFolketrygdlovenreferanse(paragrafIKapittel = 39, ledd = null, bokstav = null),
): KollektivForsikring =
    KollektivForsikring(
        navn = navn,
        dekningFolketrygdlovenreferanse = dekningFolketrygdlovenreferanse,
        kollektivFolketrygdlovenreferanse = kollektivFolketrygdlovenreferanse,
    )

fun lagIndividuellForsikring(
    navn: String = "80 % fra 1. dag (Individuell)",
    dekningFolketrygdlovenreferanse: Folketrygdlovenreferanse = lagFolketrygdlovenreferanse(),
    virkningsdato: LocalDate = LocalDate.of(2018, 1, 1),
    opphørsdato: LocalDate? = null,
    konklusjon: IndividuellForsikring.Konklusjon =
        IndividuellForsikring.Konklusjon(
            forklaring = "Lagt til grunn",
            folketrygdlovenreferanse = null,
        ),
    lagtTilGrunn: Boolean = true,
): IndividuellForsikring =
    IndividuellForsikring(
        navn = navn,
        dekningFolketrygdlovenreferanse = dekningFolketrygdlovenreferanse,
        virkningsdato = virkningsdato,
        opphørsdato = opphørsdato,
        konklusjon = konklusjon,
        lagtTilGrunn = lagtTilGrunn,
    )

fun lagFolketrygdlovenreferanse(
    kapittel: Int = 8,
    paragrafIKapittel: Int = 36,
    ledd: Int? = 1,
    bokstav: Char? = 'a',
): Folketrygdlovenreferanse =
    Folketrygdlovenreferanse(
        kapittel = kapittel,
        paragrafIKapittel = paragrafIKapittel,
        ledd = ledd,
        bokstav = bokstav,
    )
