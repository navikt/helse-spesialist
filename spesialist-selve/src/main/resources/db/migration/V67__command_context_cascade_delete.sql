ALTER TABLE command_context DROP CONSTRAINT command_context_spleisbehov_id_fkey;
ALTER TABLE command_context ADD CONSTRAINT command_context_hendelse_id_fkey FOREIGN KEY(hendelse_id) REFERENCES hendelse(id) ON DELETE CASCADE;
