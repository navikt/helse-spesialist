package no.nav.helse.spesialist.application.testfixtures

import no.nav.helse.spesialist.application.Ekskluderingsbegrunnelse
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.KollektivForsikring
import no.nav.helse.spesialist.application.NavKjøptForsikring
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import java.time.Instant
import java.time.LocalDate

fun lagForsikringsvurdering(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    harForsikring: Boolean = false,
    dekning: Forsikringsvurdering.Dekning? = null,
    ekskluderteForsikringer: List<EkskludertForsikring> = emptyList(),
    gjeldendeForsikring: Forsikring? = null,
    dataHentetTidspunkt: Instant = Instant.parse("2018-01-01T12:00:00Z"),
    samletDekning: Forsikringsvurdering.Dekning? = dekning,
    kollektivForsikring: KollektivForsikring? = null,
    navKjøpteForsikringer: List<NavKjøptForsikring> = emptyList(),
    vurdertTidspunkt: Instant = dataHentetTidspunkt,
): Forsikringsvurdering =
    Forsikringsvurdering(
        identitetsnummer = identitetsnummer,
        harForsikring = harForsikring,
        dekning = dekning,
        ekskluderteForsikringer = ekskluderteForsikringer,
        gjeldendeForsikring = gjeldendeForsikring,
        dataHentetTidspunkt = dataHentetTidspunkt,
        samletDekning = samletDekning,
        kollektivForsikring = kollektivForsikring,
        navKjøpteForsikringer = navKjøpteForsikringer,
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

fun lagNavKjøptForsikring(
    navn: String = "80 % fra 1. dag (Nav-kjøpt)",
    dekningFolketrygdlovenreferanse: Folketrygdlovenreferanse = lagFolketrygdlovenreferanse(),
    virkningsdato: LocalDate = LocalDate.of(2018, 1, 1),
    opphørsdato: LocalDate? = null,
    konklusjon: NavKjøptForsikring.Konklusjon =
        NavKjøptForsikring.Konklusjon(
            forklaring = "Lagt til grunn",
            folketrygdlovenreferanse = null,
        ),
    lagtTilGrunn: Boolean = true,
): NavKjøptForsikring =
    NavKjøptForsikring(
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

fun lagForsikring(
    virkningsdato: LocalDate = LocalDate.of(2018, 1, 1),
    opphørsdato: LocalDate? = null,
    dekningsgrad: Int = 80,
    dekningIVentetid: Boolean = true,
    navn: String = "80 % fra dag 1",
    folketrygdlovenreferanse: Folketrygdlovenreferanse = lagFolketrygdlovenreferanse(),
): Forsikring =
    Forsikring(
        virkningsdato = virkningsdato,
        opphørsdato = opphørsdato,
        dekningsgrad = dekningsgrad,
        dekningIVentetid = dekningIVentetid,
        navn = navn,
        folketrygdlovenreferanse = folketrygdlovenreferanse,
    )

fun lagEkskludertForsikring(
    virkningsdato: LocalDate = LocalDate.of(2018, 1, 1),
    opphørsdato: LocalDate? = null,
    dekningsgrad: Int = 80,
    dekningIVentetid: Boolean = true,
    navn: String = "80 % fra dag 1",
    folketrygdlovenreferanse: Folketrygdlovenreferanse = lagFolketrygdlovenreferanse(),
    ekskluderingsårsak: Ekskluderingsårsak = Ekskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
    ekskluderingsbegrunnelse: Ekskluderingsbegrunnelse =
        Ekskluderingsbegrunnelse(
            forklaring = "Forsikringen var opphørt på skjæringstidspunktet",
            folketrygdlovenreferanse = lagFolketrygdlovenreferanse(paragrafIKapittel = 37, ledd = null, bokstav = null),
        ),
): EkskludertForsikring =
    EkskludertForsikring(
        virkningsdato = virkningsdato,
        opphørsdato = opphørsdato,
        dekningsgrad = dekningsgrad,
        dekningIVentetid = dekningIVentetid,
        navn = navn,
        folketrygdlovenreferanse = folketrygdlovenreferanse,
        ekskluderingsårsak = ekskluderingsårsak,
        ekskluderingsbegrunnelse = ekskluderingsbegrunnelse,
    )
