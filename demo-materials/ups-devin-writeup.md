# Devin × UPS: Java 8 → 21 / Spring Boot 2.7 → 3.5 upgrade of the official `track` API example

## What we did

Starting point: a fork of UPS's own public repo, [UPS-API/java-api-examples](https://github.com/UPS-API/java-api-examples). The `track` module (OAuth client-credentials + Track API happy path) was on Java 8, Spring Boot 2.7.4, Spring 5.3.18, JUnit 4, with ~240 lines of hand-pinned CVE-workaround versions in the pom. It does not compile on JDK 21 today (Lombok 1.18.24 vs the JDK 21 compiler: `NoSuchFieldError: JCTree$JCImport ... qualid`).

One Devin session ingested the repo, planned, executed and opened a PR — [PR #1](https://github.com/matth4922-droid/UPS-java-api-examples/pull/1), now merged:

| | before (main) | after (PR #1) |
|---|---|---|
| `mvn clean package` on JDK 21 | **fails** | passes |
| Java / Spring Boot | 8 / 2.7.4 | 21 / 3.5.16 |
| spring-web / jackson / snakeyaml | 5.3.18 / 2.14.0 / 1.33 | 6.2.19 / 2.21.4 / 2.4 |
| generated client annotations | `javax.*` | `jakarta.*` |
| pom.xml | 265 lines, ~20 plugin pin blocks | −240 lines, Boot BOM manages versions |
| tests | 3 live calls needing UPS CIE credentials | + offline `MockRestServiceServer` test (OAuth → Bearer → Track GET → parsed response); live tests gated behind `UPS_CIE_TESTS=true` |

Hand-written application code was untouched; the change is pom + test hardening. Devin Review found no issues. The whole thing (baseline repro, upgrade, tests, PR description with before/after evidence) took one session of a few hours wall-clock.

## Why this matters for UPS

- **Security and support exposure.** Spring Boot 2.7 and Spring 5.3 are past OSS support; Java 8 is a 2014 platform. Every month on that stack is unpatched CVE exposure in revenue-and-SLA systems (Track, Ship, Rating). Devin turns "we should upgrade" into a reviewed PR in a day.
- **It scales across the portfolio.** UPS's API examples alone have ~10 modules with identical pom shape (shipping, rating, pickup, timeInTransit, addressValidation…). Internally the pattern is the same: many similar Spring services. Once the recipe is proven on one, Devin runs it in parallel across the rest — the marginal cost of module N is minutes of review, not a sprint.
- **Engineers review, they don't grind.** The tedious part of a framework upgrade — chasing transitive versions, Jakarta namespace, generator flags, dead pins, JaCoCo/Lombok/JDK compatibility — is exactly what Devin does well and what senior engineers hate. Humans stay on architecture and sign-off.
- **Evidence, not assertions.** Every PR ships with the failing baseline reproduced, the passing build, dependency deltas, and tests that run without production credentials — the artifact a product-security lead or change board actually needs.
- **Hiring and platform story.** UPS job posts already ask for Java 17/21 and Spring Boot on OpenShift. Getting the codebase onto that stack makes those roles easier to fill and unblocks Boot 3 features (virtual threads, observability, native images).
- **Repeatable loop.** Repo ingest → plan → code → build/test → PR → review → merge, with Devin Review on the PR and a persisted dev environment (JDK 21 + Maven blueprint) so the next session starts warm.

## Suggested next steps

1. Run the same recipe on `shipping` live during the meeting (still Java 8 / Boot 2.7.4 → visibly fails on JDK 21 today).
2. Add a small GitHub Actions workflow so the PR loop visibly closes with green CI.
3. Pilot on one internal UPS Spring service behind the same before/after evidence format.
