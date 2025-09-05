package com.couchbase.perf.shared.config

import com.couchbase.perf.shared.config.PerfConfig.Implementation
import groovy.json.JsonGenerator
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * The parsed job-config.yaml.
 *
 * Try to parse the minimum required into objects, as we currently have very similar code here and in the driver
 * (which has to parse a similar per-run config), and it's brittle.  Aim to just parse through the YAML into the per-run
 * config as much as possible.
 */
@CompileStatic
@ToString(includeNames = true, includePackage = false)
class PerfConfig {
    public final Servers servers
    public final Database database
    public final Map<String, String> executables
    public final Matrix matrix
    public final Settings settings

    @ToString(includeNames = true, includePackage = false)
    static class Matrix {
        List<Cluster> clusters
        List<Implementation> implementations
        List<Workload> workloads
    }

    @ToString(includeNames = true, includePackage = false)
    static class Servers {
        String performer
    }

    @ToString(includeNames = true, includePackage = false)
    static class Database {
        String host
        int port
        String user
        String password
        String database
    }

    // This is the superset of all supported cluster params.  Not all of them are used for each cluster type.
    @ToString(includeNames = true, includePackage = false)
    static class Cluster {
        String version
        Integer nodeCount
        Integer memory
        Integer cpuCount
        String type
        String connection_string_driver
        String connection_string_performer
        String connection_string_driver_docker
        String connection_string_performer_docker
        String hostname_rest
        String hostname_rest_docker
        String cert_path
        Boolean insecure
        String storage
        Integer replicas

        // "c5.4xlarge"
        // Only present on AWS
        String instance

        // "disabled"
        String compaction

        // By topology we mean where the performer, driver and cluster are running.  There are many possible permuations
        // so we represent them with a code letter.
        // "A" = driver, performer and cluster all running on same AWS node, in docker
        String topology

        // Only present if Protostellar
        String cloudNativeGatewayVersion

        // Any new fields here probably want adding into toJsonRaw below, and into the driver config, and includeVariablesThatApplyToThisRun

        boolean isCouchbase2() {
            return connection_string_performer.startsWith("couchbase2")
        }

        @CompileDynamic
        def toJsonRaw(boolean forDatabaseComparison) {
            // As always we need to make sure we're comparing apples-to-apples when looking for matching runs in the database.
            // So anything that may affect the performance of the run should go into the database and be looked for in queries.
            // This includes many details of the cluster.
            def out = [
                    "version"   : version,
                    "nodeCount" : nodeCount,
                    "memory"    : memory,
                    "cpuCount"  : cpuCount,
                    "replicas"  : replicas,
                    "instance"  : instance,
                    "compaction": compaction,
                    "topology"  : topology
            ]
            // We need to write something to the database to distinguish Protostellar & OpenShift testing.  Using the
            // performer's connection string.  It may actually be using connection_string_performer_docker, but we can't
            // know that here, and it's not relevant for this purpose.
            // There's a lot of existing tests that don't have this connectionString field, so we only check it for
            // newer tests - e.g. Protostellar ones.
            // Update: now edited the database so all tests include it.
            // Update: have moved the connectionString check from here, as it messes up FaaS - which uses cbdinocluster
            // so we don't know what the connStr is.
            // It's also, strictly speaking, not a property of the cluster itself.
            // We will need to handle CNG differently when we get back to it.
            if (forDatabaseComparison) {
                // Checks here removed, see comments above.
            }

            if (!forDatabaseComparison) {
                out.put("connection_string_driver", connection_string_driver)
                out.put("connection_string_driver_docker", connection_string_driver_docker)
                out.put("connection_string_performer", connection_string_performer)
                out.put("connection_string_performer_docker", connection_string_performer_docker)
                out.put("hostname_rest", hostname_rest)
                out.put("hostname_rest_docker", hostname_rest_docker)
                out.put("cert_path", cert_path)
                out.put("insecure", insecure)
                out.put("type", type)
                // Storage has been removed from the database comparison partly for convenience.. 
                // Storage type changed at 8.0 to Magma, and it's just easier on the frontend if we don't have to hardcode that knowledge.
                out.put("storage", storage)
            }
            if (isCouchbase2()) {
                out.put("cloudNativeGatewayVersion", cloudNativeGatewayVersion)
            }
            return out
        }
    }

    @ToString(includeNames = true, includePackage = false)
    static class Implementation {
        // "Java"
        String language

        // "3.3.3" or "3.3.3-6abad3" (snapshot) or "refs/changes/94/184294/1" (gerrit) or "main"
        // It's public to allow it to written by the JSON slurper, but for safety callers should use version()
        String version

        // "6abad3", nullable - used for some languages to handle snapshot builds
        String sha

        // A null port means jenkins-sdk needs to bring it up
        Integer port

        // True iff "snapshot" was the originally specified version
        boolean isSnapshot

        Implementation() {}

        Implementation(String language, String version, Integer port, String sha = null, boolean isSnapshot = false) {
            this.language = language
            this.version = version
            this.port = port
            this.sha = sha
            this.isSnapshot = isSnapshot
        }

        String version() {
            return version
        }

        boolean isGerrit() {
            return version.startsWith("refs/")
        }

        boolean isMain() {
            return version == "main"
        }

        boolean hasVersion() {
            if (version == null) {
                // Internal bug
                throw new IllegalStateException("version field is somehow null and shouldn't be")
            }
            return !isGerrit() && !isMain()
        }

        @CompileDynamic
        def toJson() {
            return [
                    "language": language,
                    "version" : version
            ]
        }
    }
}

/**
 * Whether a given thing should be included in this run.
 *
 * Includes are AND-based - all Includes must be satisfied for the run to be included.
 */
@ToString(includeNames = true, includePackage = false)
class Include {
    public final Implementation implementation;
    public final PerfConfig.Cluster cluster;
}

/**
 * Read from job-config.yaml.
 *
 * Either value or values will be non-null, not both.
 *
 * At the point all variables have been permuted ready to be written to a per-run config, only name and value will be present.
 *
 * include decides whether a variable is included - usually used to specify per-SDK tunables
 */
@ToString(includeNames = true, includePackage = false, includeFields = true)
class Variable {
    public final String name;
    public final Object value;
    // "tunable" or null
    public final String type;
    public final List<Object> values = null;
    public final List<Include> include = null;

    Variable() {}

    Variable(String name, Object value, String type) {
        this.name = name
        this.value = value
        this.type = type
    }
}

@ToString(includeNames = true, includePackage = false, includeFields = true, excludes = ["type"])
record PermutedVariable(String name, Object value, String type) {
    // By this point the variables have been permuted and only value is present
    @CompileDynamic
    def asYaml() {
        return [
                name: this.name,
                value: this.value,
                type: this.type,
        ]
    }
}

@ToString(includeNames = true, includePackage = false, includeFields = true)
class Settings {
    public final List<Variable> variables;
    public final Object grpc;

    Settings() {}

    Settings(List<Variable> variables, Object grpc) {
        this.variables = variables
        this.grpc = grpc
    }
}

record PermutedSettings(List<PermutedVariable> variables, Object grpc) {}

@ToString(includeNames = true, includePackage = false, includeFields = true)
class Workload {
    Object operations;
    Settings settings;
    Object include;
    Object exclude;

    Workload() {}

    Workload(Object operations, Settings settings, Object include, Object exclude) {
        this.operations = operations
        this.settings = settings
        this.include = include
        this.exclude = exclude
    }

    @CompileDynamic
    def toJson() {
        // Some workload variables are used for meta purposes but we don't want to compare the database runs with them
        return [
                "operations": operations
        ]
    }
}

record PermutedWorkload(Object operations, PermutedSettings settings, Object include, Object exclude) {
    @CompileDynamic
    def toJson() {
        // Some workload variables are used for meta purposes but we don't want to compare the database runs with them
        return [
                "operations": operations
        ]
    }
}

@CompileStatic
@ToString(includeNames = true, includePackage = false, includeFields = true)
class Run {
    public final PerfConfig.Implementation impl
    public final PermutedWorkload workload
    public final PerfConfig.Cluster cluster

    Run(PerfConfig.Implementation impl, PermutedWorkload workload, PerfConfig.Cluster cluster) {
        this.impl = impl
        this.workload = workload
        this.cluster = cluster
    }

    @CompileDynamic
    def toJson() {
        Map<String, Object> jsonVars = new HashMap<>()
        if (workload.settings() != null && workload.settings().variables() != null) {
            workload.settings().variables().forEach(var -> {
                jsonVars.put(var.name(), var.value())
            })
        }

        jsonVars.put("driverVer", 6)
        int performerVersion = 1
        jsonVars.put("performerVer", performerVersion)

        def gen = new JsonGenerator.Options()
                .excludeNulls()
                .build()

        // This is for comparison to the database, which isn't a 1:1 match to the config YAML, since some stuff
        // gets removed and flattened out to simplify the database JSON.  For example, no need to record GRPC settings.
        def out = gen.toJson([
                "impl"    : impl.toJson(),
                "vars"    : jsonVars,
                "cluster" : cluster.toJsonRaw(true),
                "workload": workload.toJson(),
        ])
        return out
    }
}
