CREATE TABLE behandling_soknad
(
    behandling_id     uuid not null,
    søknad_id         uuid not null,
    PRIMARY KEY (behandling_id, søknad_id)
);