package no.nav.helse.spesialist.test

import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavnOrNull
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import java.util.UUID

class TestPerson {
    val fødselsnummer: String = lagFødselsnummer()
    val aktørId: String = lagAktørId()
    val fornavn: String = lagFornavn()
    val mellomnavn: String? = lagMellomnavnOrNull()
    val etternavn: String = lagEtternavn()
    val kjønn = Kjønn.entries.toTypedArray().random()
    private val arbeidsgivere = mutableMapOf<Int, TestArbeidsgiver>()
    private val arbeidsgiver1 = nyArbeidsgiver()
    private val arbeidsgiver2 = nyArbeidsgiver()
    private val vedtaksperiode1 = arbeidsgiver1.nyVedtaksperiode()
    private val vedtaksperiode2 = arbeidsgiver1.nyVedtaksperiode()
    val orgnummer: String = arbeidsgiver1.organisasjonsnummer
    val orgnummer2: String = arbeidsgiver2.organisasjonsnummer
    val vedtaksperiodeId1 = vedtaksperiode1.vedtaksperiodeId
    val vedtaksperiodeId2 = vedtaksperiode2.vedtaksperiodeId
    val utbetalingId1 = vedtaksperiode1.utbetalingId
    private val utbetalingId2 = vedtaksperiode2.utbetalingId

    override fun toString(): String {
        return "Testdatasett(fødselsnummer='$fødselsnummer', aktørId='$aktørId', orgnummer='$orgnummer', orgnummer2='$orgnummer2', vedtaksperiodeId1=$vedtaksperiodeId1, vedtaksperiodeId2=$vedtaksperiodeId2, utbetalingId1=$utbetalingId1, utbetalingId2=$utbetalingId2)"
    }

    val Int.arbeidsgiver
        get() = arbeidsgivere[this - 1] ?: throw IllegalArgumentException("Arbeidsgiver med index $this finnes ikke")

    fun nyArbeidsgiver() = TestArbeidsgiver(fødselsnummer, aktørId).also {
        arbeidsgivere[arbeidsgivere.size] = it
    }
}

class TestArbeidsgiver(
    val fødselsnummer: String,
    val aktørId: String,
) {
    private val vedtaksperioder = mutableMapOf<Int, TestVedtaksperiode>()
    val organisasjonsnummer = lagOrganisasjonsnummer()
    val organisasjonsnavn = lagOrganisasjonsnavn()

    fun nyVedtaksperiode() = TestVedtaksperiode(fødselsnummer, aktørId, organisasjonsnummer).also {
        vedtaksperioder[vedtaksperioder.size] = it
    }

    val Int.vedtaksperiode
        get() = vedtaksperioder[this] ?: throw IllegalArgumentException(
            "Vedtaksperiode med index $this for arbeidsgiver $organisasjonsnummer finnes ikke",
        )
}

class TestVedtaksperiode(
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
) {
    val vedtaksperiodeId: UUID = UUID.randomUUID()
    val spleisBehandlingId: UUID = UUID.randomUUID()
    val utbetalingId: UUID = UUID.randomUUID()
}
