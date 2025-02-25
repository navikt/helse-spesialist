package no.nav.helse.spesialist.api.bootstrap

import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.application.Snapshothenter

data class ApiAvhengigheter(
    val saksbehandlerMediatorProvider: () -> SaksbehandlerMediator,
    val apiOppgaveServiceProvider: () -> ApiOppgaveService,
    val godkjenninghåndtererProvider: () -> Godkjenninghåndterer,
    val personhåndtererProvider: () -> Personhåndterer,
    val dokumenthåndtererProvider: () -> Dokumenthåndterer,
    val stansAutomatiskBehandlinghåndterer: () -> StansAutomatiskBehandlinghåndterer,
    val behandlingstatistikk: IBehandlingsstatistikkService,
    val snapshothenter: Snapshothenter,
)
