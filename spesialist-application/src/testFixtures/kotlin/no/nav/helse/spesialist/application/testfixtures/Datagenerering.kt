package no.nav.helse.spesialist.application.testfixtures

import no.nav.helse.spesialist.application.Ekskluderingsbegrunnelse
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
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
): Forsikringsvurdering =
    Forsikringsvurdering(
        identitetsnummer = identitetsnummer,
        harForsikring = harForsikring,
        dekning = dekning,
        ekskluderteForsikringer = ekskluderteForsikringer,
        gjeldendeForsikring = gjeldendeForsikring,
        dataHentetTidspunkt = dataHentetTidspunkt,
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
