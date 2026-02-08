package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.overstyringer.MinimumSykdomsgrad
import no.nav.helse.spesialist.domain.overstyringer.OverstyringId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag

class InMemoryTotrinnsvurderingRepository : TotrinnsvurderingRepository,
    AbstractLateIdInMemoryRepository<TotrinnsvurderingId, Totrinnsvurdering>() {
    override fun finnAktivForPerson(fødselsnummer: String) = alle().find { it.fødselsnummer == fødselsnummer }

    override fun tildelIderSomMangler(root: Totrinnsvurdering) {
        if (!root.harFåttTildeltId())
            root.tildelId(genererTotrinnsvurderingId())
        root.overstyringer.forEach { overstyring ->
            if (!overstyring.harFåttTildeltId())
                overstyring.tildelId(genererOverstyringId())
        }
    }

    private fun genererTotrinnsvurderingId(): TotrinnsvurderingId =
        TotrinnsvurderingId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)

    private fun genererOverstyringId(): OverstyringId =
        OverstyringId((alle().flatMap { it.overstyringer }.maxOfOrNull { it.id().value } ?: 0) + 1)

    override fun deepCopy(original: Totrinnsvurdering): Totrinnsvurdering = Totrinnsvurdering.fraLagring(
        id = original.id(),
        fødselsnummer = original.fødselsnummer,
        saksbehandler = original.saksbehandler,
        beslutter = original.beslutter,
        opprettet = original.opprettet,
        oppdatert = original.oppdatert,
        overstyringer = original.overstyringer.map { originalOverstyring ->
            when (originalOverstyring) {
                is MinimumSykdomsgrad -> MinimumSykdomsgrad.fraLagring(
                    id = originalOverstyring.id(),
                    eksternHendelseId = originalOverstyring.eksternHendelseId,
                    opprettet = originalOverstyring.opprettet,
                    ferdigstilt = originalOverstyring.ferdigstilt,
                    saksbehandlerOid = originalOverstyring.saksbehandlerOid,
                    fødselsnummer = originalOverstyring.fødselsnummer,
                    aktørId = originalOverstyring.aktørId,
                    vedtaksperiodeId = originalOverstyring.vedtaksperiodeId,
                    perioderVurdertOk = originalOverstyring.perioderVurdertOk.toList(),
                    perioderVurdertIkkeOk = originalOverstyring.perioderVurdertIkkeOk.toList(),
                    begrunnelse = originalOverstyring.begrunnelse,
                    arbeidsgivere = originalOverstyring.arbeidsgivere.toList(),
                )

                is OverstyrtArbeidsforhold -> OverstyrtArbeidsforhold.fraLagring(
                    id = originalOverstyring.id(),
                    eksternHendelseId = originalOverstyring.eksternHendelseId,
                    opprettet = originalOverstyring.opprettet,
                    ferdigstilt = originalOverstyring.ferdigstilt,
                    saksbehandlerOid = originalOverstyring.saksbehandlerOid,
                    fødselsnummer = originalOverstyring.fødselsnummer,
                    aktørId = originalOverstyring.aktørId,
                    vedtaksperiodeId = originalOverstyring.vedtaksperiodeId,
                    skjæringstidspunkt = originalOverstyring.skjæringstidspunkt,
                    overstyrteArbeidsforhold = originalOverstyring.overstyrteArbeidsforhold.toList(),
                )

                is OverstyrtInntektOgRefusjon -> OverstyrtInntektOgRefusjon.fraLagring(
                    id = originalOverstyring.id(),
                    eksternHendelseId = originalOverstyring.eksternHendelseId,
                    opprettet = originalOverstyring.opprettet,
                    ferdigstilt = originalOverstyring.ferdigstilt,
                    saksbehandlerOid = originalOverstyring.saksbehandlerOid,
                    fødselsnummer = originalOverstyring.fødselsnummer,
                    aktørId = originalOverstyring.aktørId,
                    vedtaksperiodeId = originalOverstyring.vedtaksperiodeId,
                    skjæringstidspunkt = originalOverstyring.skjæringstidspunkt,
                    arbeidsgivere = originalOverstyring.arbeidsgivere.toList(),
                )

                is OverstyrtTidslinje -> OverstyrtTidslinje.fraLagring(
                    id = originalOverstyring.id(),
                    eksternHendelseId = originalOverstyring.eksternHendelseId,
                    opprettet = originalOverstyring.opprettet,
                    ferdigstilt = originalOverstyring.ferdigstilt,
                    saksbehandlerOid = originalOverstyring.saksbehandlerOid,
                    fødselsnummer = originalOverstyring.fødselsnummer,
                    aktørId = originalOverstyring.aktørId,
                    vedtaksperiodeId = originalOverstyring.vedtaksperiodeId,
                    organisasjonsnummer = originalOverstyring.organisasjonsnummer,
                    dager = originalOverstyring.dager.toList(),
                    begrunnelse = originalOverstyring.begrunnelse,
                )

                is SkjønnsfastsattSykepengegrunnlag -> SkjønnsfastsattSykepengegrunnlag.fraLagring(
                    id = originalOverstyring.id(),
                    eksternHendelseId = originalOverstyring.eksternHendelseId,
                    opprettet = originalOverstyring.opprettet,
                    ferdigstilt = originalOverstyring.ferdigstilt,
                    saksbehandlerOid = originalOverstyring.saksbehandlerOid,
                    fødselsnummer = originalOverstyring.fødselsnummer,
                    aktørId = originalOverstyring.aktørId,
                    vedtaksperiodeId = originalOverstyring.vedtaksperiodeId,
                    skjæringstidspunkt = originalOverstyring.skjæringstidspunkt,
                    arbeidsgivere = originalOverstyring.arbeidsgivere.toList(),
                )
            }
        },
        tilstand = original.tilstand,
        vedtaksperiodeForkastet = original.vedtaksperiodeForkastet,
    )
}
