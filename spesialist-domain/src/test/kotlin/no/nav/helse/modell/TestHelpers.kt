package no.nav.helse.modell

import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 699999_99999).toString().padStart(11, '0')

fun lagAktørId() = nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()
