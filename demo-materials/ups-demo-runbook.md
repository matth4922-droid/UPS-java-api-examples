# UPS Demo Runbook — Java 8 → 21 / Spring Boot 2.7 → 3.5 on `track`

Reference artifact (already done, use as fallback if the live run misbehaves):
https://github.com/matth4922-droid/UPS-java-api-examples/pull/1

## Why this repo / module
- Official UPS org (`UPS-API`), public, Maven, real Track API client that hits UPS CIE.
- `track/pom.xml` today: Java 8, Boot 2.7.4, Spring 5.3.18, JUnit 4, openapi-generator 6.2, jacoco 0.8.7, plus ~240 lines of hand-pinned CVE band-aids (commons-io, snakeyaml 1.33, plexus…). That is exactly the "we patch instead of upgrade" pattern a CIO / product-security lead recognizes.
- Hook line: "Today this module cannot even compile on the JDK your job postings ask for." (`mvn package` on JDK 21 fails with `NoSuchFieldError: JCTree$JCImport…qualid` — Lombok 1.18.24 vs JDK 21 javac. It passes on JDK 17.)

## Suggested 40-minute shape
| min | beat |
|---|---|
| 0–5 | Context: Track/Ship are revenue+SLA systems; Boot 2.7 / Java 8 EOL = security + hiring risk. Show `track/pom.xml` on main. |
| 5–8 | Show baseline: `mvn clean package -DskipTests` on JDK 21 fails (screenshot/log). |
| 8–12 | Kick off live Devin session with the prompt below. While it runs, narrate the loop: ingest → plan → code → build/test → PR. |
| 12–25 | Devin works. Walk through PR #1 (the pre-baked run) in parallel: before/after table, pom diff (-240 lines), `useJakartaEe`, gated live tests, new offline `MockRestServiceServer` test. |
| 25–32 | Live session lands its PR. Show `mvn clean package` green on JDK 21, `grep javax.annotation target/generated-sources` empty. |
| 32–36 | Scale-out story: shipping / rating / pickup… share the same pom shape → one playbook, N parallel sessions. Optionally kick one off on `shipping` live. |
| 36–40 | Q&A. Objections to be ready for: "does it run against CIE?" (yes, `UPS_CIE_TESTS=true` + creds runs the original live tests unchanged); "what about OpenShift?" (Boot 3.5 → Java 21 base image, no code change). |

## Live-session prompt (copy/paste)
```
In UPS-java-api-examples, upgrade the `track` module from Java 8 / Spring Boot 2.7.4 to Java 21 / the latest Spring Boot 3.x.
- Move generated OpenAPI clients to the Jakarta namespace, let the Boot BOM manage Jackson/SnakeYAML/Spring versions, and remove the hand-pinned CVE overrides that the BOM now covers.
- Keep the Track happy path (OAuth client-credentials → GET /track/v1/details) compiling and add an offline test that proves it without UPS credentials; keep the existing live CIE tests but make them opt-in.
- `mvn clean package` on JDK 21 must pass. Open a PR with a before/after summary.
```

## Talking points on the resulting diff
- Zero hand-written Java changed — the upgrade is pom + tests. Message: "the risk is in the build, not the business logic."
- `spring-boot-starter-parent` 3.5.16, `java.version=21`, openapi-generator 7.25 with `useJakartaEe=true`, jacoco 0.8.12.
- Removed: JUnit 4, javax JAX-RS Jackson provider, every per-plugin `<dependencies>` pin block.
- Dependency deltas: spring-web 5.3.18→6.2.19, jackson 2.14.0→2.21.4, snakeyaml 1.33→2.4.
- Tests: `TrackAppTest` gated by `@EnabledIfEnvironmentVariable(UPS_CIE_TESTS=true)`; new `TrackOfflineTest` mocks token + track endpoints and asserts the bearer is propagated and `TrackApiResponse` deserializes.

## Demo-day prep checklist
- Fork is `matth4922-droid/UPS-java-api-examples`; main is identical to upstream `UPS-API/java-api-examples@c5ffb9a`. Don't merge PR #1 before the demo (keep main "old").
- Devin environment needs JDK 21 + Maven 3.9 (blueprint update proposed in this session) — Maven Central rate-limited (HTTP 429) once during this session; a warm `~/.m2` in the snapshot avoids that on stage.
- No CI in the repo; if you want a green check on the PR, add a `.github/workflows/track.yml` running `mvn -B -f track/pom.xml clean package` on `temurin@21` (small, and makes the loop visibly close).
- Have JDK 21 + Maven locally so you can `git fetch && mvn clean package` on the PR branch on your own laptop as the "trust but verify" moment.
