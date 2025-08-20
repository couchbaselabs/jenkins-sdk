A Jenkins shared library, for use by SDK team.

(Well, that was the intent, but Jenkins won't allow untrusted libraries to use third-party libs.)

# SDK Performance
See https://github.com/couchbaselabs/transactions-fit-performer/blob/master/perf-driver/README.md for instructions.

# CLI tools

## Tags
A pre-processor conditionally comments-out bits of code depending on a target SDK version.
The pre-processor directives are referred to as "tags". See the [Tag Syntax Guide](TAG_SYNTAX.md).

To run `TagProcessor` from the command line using Gradle:
```
./gradlew tags --args="-d <some_path> -v 3.0.0"
```

To restore the code to its "pre-preprocessed" state, use the -r flag:
```
./gradlew tags --args="-d <some_path> -v 3.0.0 -r"
```

# Building performer Docker images
See the comments on `BuilderPerformer` for more.

```
./gradlew buildPerformer --args="-d <some_path> -s kotlin -v 1.0.0 -i performer"
```

# Setting up performance database
Creates all required tables.

```
./gradlew setupPerfDatabase
```

or with args:

```
./gradlew setupPerfDatabase --args="--host timescaledb"
```