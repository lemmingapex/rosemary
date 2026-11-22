# Releasing Rosemary

Rosemary is published to Maven Central through the [Sonatype Central Portal](https://central.sonatype.com), using the [Gradle Maven Publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/).

The old OSSRH service (`oss.sonatype.org`) was shut down in 2025. The `ossrhUsername` and `ossrhPassword` properties are no longer used.

## One-time setup

1. Sign in to the [Central Portal](https://central.sonatype.com), and check that the `com.lemmingapex` namespace is listed under **Namespaces**. OSSRH namespaces were migrated to the Central Portal, so it should already be there.
2. Generate a user token: **View Account** → **Generate User Token**. A user token is a separate username and password pair, not your login.
3. Add the token to `~/.gradle/gradle.properties`. Never add it to the project's `gradle.properties`:
   ```properties
   mavenCentralUsername=<token username>
   mavenCentralPassword=<token password>
   ```
4. Signing uses the Gradle signing plugin's standard properties, also in `~/.gradle/gradle.properties`:
   ```properties
   signing.keyId=<last 8 characters of the key id>
   signing.password=<key passphrase>
   signing.secretKeyRingFile=<path to secring.gpg>
   ```
   Or provide the key in memory instead, e.g. in CI:
   ```zsh
   export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <key id>)"
   export ORG_GRADLE_PROJECT_signingInMemoryKeyId=<last 8 characters of the key id>
   export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=<key passphrase>
   ```
   The public key must be on a public key server, such as `keys.openpgp.org`, so Maven Central can verify the signatures.

## Release steps

1. Update `version` in [lib/build.gradle.kts](lib/build.gradle.kts), and the version in the README's dependency snippets.
2. Make sure the build and tests pass:
   ```zsh
   ./gradlew clean build
   ```
3. Optionally, check the signed artifacts in a throwaway local repository:
   ```zsh
   ./gradlew publishToMavenLocal -Dmaven.repo.local=$PWD/build/m2
   ```
4. Upload to the Central Portal:
   ```zsh
   ./gradlew publishToMavenCentral
   ```
   Then review the deployment under **Deployments** in the Central Portal, and click **Publish**.
   Or upload and release in one step with `./gradlew publishAndReleaseToMavenCentral`.
   A release can't be changed or removed once it is published.
5. Tag the release and push the tag:
   ```zsh
   git tag <version>
   git push origin <version>
   ```

The release usually appears on [Maven Central](https://repo1.maven.org/maven2/com/lemmingapex/rosemary/) within about 30 minutes.
