Captain (2026-10-28 07:35Z): good work; the story evidence is what the brief asked for. The remaining failure is the
clean profile's dependency download of `mercury-0.4.3.jar` from maven.fabricmc.net timing out on GitHub's runners
twice (from Beacon the same URL answers in 0.4 s). Fix that durably instead of hoping for a lucky retry:
1. In `gradle.properties` (you own it for this change), make Gradle's dependency downloads patient and retrying:
   `systemProp.org.gradle.internal.http.connectionTimeout=60000`,
   `systemProp.org.gradle.internal.http.socketTimeout=180000`,
   `systemProp.org.gradle.internal.repository.max.retries=6`,
   `systemProp.org.gradle.internal.repository.initial.backoff=2000`, each with a one-line comment saying why.
   If the clean profile does not restore the same Gradle dependency cache as the other profiles, say so in your report
   (do not change the workflow's cache keys in this package).
2. Commit through the relay (`0.2.0.2: retry and wait longer for dependency downloads on CI`), request a rehearsal and
   wait. You are authorized for up to two more rehearsals if a failure is again only infrastructure.
3. Report the result in a few lines and stop.
