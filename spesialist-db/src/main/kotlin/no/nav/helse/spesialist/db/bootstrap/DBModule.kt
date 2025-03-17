package no.nav.helse.spesialist.db.bootstrap

import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.DataSourceBuilder
import no.nav.helse.spesialist.db.FlywayMigrator
import no.nav.helse.spesialist.db.TransactionalSessionFactory

class DBModule(configuration: Configuration) {
    data class Configuration(
        val jdbcUrl: String,
        val username: String,
        val password: String,
    ) {
        companion object {
            fun fraEnv(env: Map<String, String>): Configuration {
                return Configuration(
                    jdbcUrl =
                        "jdbc:postgresql://" +
                            env.getRequired("DATABASE_HOST") +
                            ":" +
                            env.getRequired("DATABASE_PORT") +
                            "/" +
                            env.getRequired("DATABASE_DATABASE"),
                    username = env.getRequired("DATABASE_USERNAME"),
                    password = env.getRequired("DATABASE_PASSWORD"),
                )
            }

            private fun Map<String, String>.getRequired(name: String) = this[name] ?: error("$name må settes")
        }
    }

    val dataSource = DataSourceBuilder(configuration).build()
    val daos = DBDaos(dataSource)
    val sessionFactory = TransactionalSessionFactory(dataSource)
    val flywayMigrator = FlywayMigrator(configuration)
}
