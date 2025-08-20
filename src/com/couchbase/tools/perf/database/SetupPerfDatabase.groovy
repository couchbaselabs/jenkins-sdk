package com.couchbase.tools.perf.database

import com.couchbase.context.environments.Environment
import com.couchbase.perf.shared.config.PerfConfig
import com.couchbase.perf.shared.database.PerfDatabase
import com.couchbase.tools.performer.*
import com.couchbase.tools.tags.TagProcessor
import com.couchbase.versions.*
import groovy.cli.picocli.CliBuilder

import java.util.logging.Logger

/**
 * Sets up the performance database.
 */
class SetupPerfDatabase {
    private static Logger logger = Logger.getLogger("")

    static void main(String[] args) {
        TagProcessor.configureLogging(logger)

        def env = new Environment()

        def cli = new CliBuilder(usage: 'setup-perf-db [options]')
        cli.h(longOpt: 'help', 'Show usage information')
        cli.H(longOpt: 'host', args: 1, argName: 'host', "Postgres host (default 'localhost')")
        cli.P(longOpt: 'port', args: 1, argName: 'port', "Postgres port (default 5432)")
        cli.d(longOpt: 'db', args: 1, argName: 'db', "Database name (default 'perf')")
        cli.u(longOpt: 'username', args: 1, argName: 'username', "Database username (default 'postgres')")
        cli.p(longOpt: 'password', args: 1, argName: 'password', "Database password (default 'password')")

        def options = cli.parse(args)
        if (!options) {
            return
        }
        if (options.h) {
            cli.usage()
            return
        }

        def host = options.H ?: 'localhost'
        def port = (options.P ? Integer.parseInt(options.P as String) : 5432)
        def db = options.d ?: 'perf'
        def username = options.u ?: 'postgres'
        def password = options.p ?: 'password'

        def jdbc = "jdbc:postgresql://${host}:${port}/${db}"

        env.log("Running DB migration against ${jdbc} as user '${username}'")
        PerfDatabase.migrate(jdbc as String, username as String, password as String, env)
    }
}