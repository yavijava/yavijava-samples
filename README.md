# yavijava Samples

`yavijava-samples` provides runnable examples for using the yavijava vSphere API library against a vCenter or ESXi endpoint. The sample runner lets you discover available examples, inspect their usage, and run them with a consistent credential interface.

## Requirements

- Java 21
- `com.toastcoders:yavijava:9.0`
- A reachable vCenter or ESXi endpoint

Never commit real credentials, endpoint URLs with embedded passwords, shell history exports, or local configuration files that contain secrets.

## Listing Samples

Build the distribution first if needed:

```sh
./gradlew installDist
```

List available samples:

```sh
./samples list
```

Show usage for a specific sample:

```sh
./samples help HelloVM
```

## Running Samples

Run a sample with explicit credentials:

```sh
./samples HelloVM --url https://vcenter.example.com/sdk --user administrator@vsphere.local --password 'secret'
```

Locale is optional. When explicitly provided with `--locale`, `sample.locale`, or `YAVIJAVA_LOCALE`, samples that use `SampleUtil` apply it when creating a vSphere session. Other legacy samples may ignore it until they are migrated to the shared runner configuration.

```sh
./samples HelloVM --url https://vcenter.example.com/sdk --user administrator@vsphere.local --password 'secret' --locale en-US
```

You can also provide credentials with environment variables:

```sh
export YAVIJAVA_URL=https://vcenter.example.com/sdk
export YAVIJAVA_USER=administrator@vsphere.local
export YAVIJAVA_PASSWORD='secret'
./samples HelloVM
```

Credential source order is:

1. CLI flags: `--url`, `--user`, `--password`, `--locale`, `--session-token`
2. Java system properties: `sample.url`, `sample.user`, `sample.password`, `sample.locale`, `sample.sessionToken`
3. Environment variables: `YAVIJAVA_URL`, `YAVIJAVA_USER`, `YAVIJAVA_PASSWORD`, `YAVIJAVA_LOCALE`, `YAVIJAVA_SESSION_TOKEN`

Session-token configuration is supported by the runner for samples cataloged as `SESSION_TOKEN` once their legacy argument handling is compatible with the runner.

Unusual legacy or plugin callback samples that do not accept runner-managed connection settings are intentionally excluded from the catalog until migrated.

Pass sample-specific arguments after `--` so the runner can separate them from connection options:

```sh
./samples SomeSample --url https://vcenter.example.com/sdk --user administrator@vsphere.local --password 'secret' -- --sample-option value
```

## Gradle Equivalent

The Gradle application plugin can run the same CLI entrypoint:

```sh
./gradlew run --args="list"
./gradlew run --args="help HelloVM"
./gradlew run --args="HelloVM --url https://vcenter.example.com/sdk --user administrator@vsphere.local --password secret"
```

Java system properties can be supplied through `JAVA_TOOL_OPTIONS` or the Gradle command line as appropriate for your environment.

## Local Development

Useful development commands:

```sh
./gradlew classes
./gradlew test
./gradlew installDist
```
