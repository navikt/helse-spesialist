package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.UUID

class DelegatingVedtakDao(
    private val automatiseringDao: InMemoryAutomatiseringDao,
) : VedtakDao {
    private data class Vedtaksperiodetype(
        val type: Periodetype,
        val inntektskilde: Inntektskilde,
    )

    private val vedtaksperiodetyper = mutableMapOf<UUID, Vedtaksperiodetype>()

    override fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    ) {
        vedtaksperiodetyper[vedtaksperiodeId] = Vedtaksperiodetype(type, inntektskilde)
    }

    fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype? = vedtaksperiodetyper[vedtaksperiodeId]?.type

    override fun erAutomatiskGodkjent(utbetalingId: UUID): Boolean = automatiseringDao.automatisert.contains(utbetalingId)

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        TODO("Not yet implemented")
    }

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? = vedtaksperiodetyper[vedtaksperiodeId]?.inntektskilde

    override fun finnOrganisasjonsnummer(vedtaksperiodeId: UUID): String? {
        TODO("Not yet implemented")
    }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto? {
        TODO("Not yet implemented")
    }

    override fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto,
    ) {
        TODO("Not yet implemented")
    }
}
