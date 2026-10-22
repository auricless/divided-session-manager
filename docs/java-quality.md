# Java quality checks

Ticket: [Java quality workflow](https://app.notion.com/p/40161e61aca1813d8260d032dc289966).

## Acceptance criteria

- Keep application compilation and tests compatible with Java 17.
- Enforce Google Java Format, Checkstyle, and SpotBugs for the Maven Java modules in PR CI.
- Reject representative formatting, style, and bytecode bug violations; the restored source must pass the full test and coverage suite.
- Add SonarQube Cloud Free analysis for `master` and same-repository PRs targeting `master`, importing JaCoCo XML and waiting for the quality gate.
- Keep analysis credentials out of fork/Dependabot PRs and ordinary local builds; report unconfigured or unverified service activation honestly.

## Local commands

Use Maven 3.9+ and JDK 21 for the `quality` profile. The compiler uses `--release 17`, which checks Java 17 APIs and produces Java 17 bytecode even on JDK 21.

```sh
mvn -Pquality verify
mvn -Pquality spotless:apply
mvn -Pquality checkstyle:check
mvn -Pquality spotbugs:check
```

`mvn verify` remains available on JDK 17 without the quality profile. CI runs that Java 17 verification separately from the Java 21 quality job.

Spotless 3.10.4 uses Google Java Format 1.36.1, its supported default release. Checkstyle 14.3.0 runs through Maven Checkstyle Plugin 3.6.0. The small ruleset covers imports, tabs, braces, modifiers, and class/value contracts; formatting is owned by the formatter. Static wildcard imports remain available for JUnit assertions. SpotBugs Maven Plugin 4.10.5.0 checks production bytecode at maximum effort and medium-or-higher confidence. PMD is omitted to avoid overlapping checks. No runtime dependency is added to the core.

The checked-in SpotBugs filter is limited to identified methods/classes: tested immutable result lists, mutable builder setters whose built values are defensively copied, the existing domain name `FacilitatorException`, and validation constructors of resource-free values. Constructor-throw warnings concern finalizer attacks; these constructors hold no credentials or resources and perform no privileged operation. This exception preserves the current public subclassing API and must be reassessed if those classes gain sensitive resources or construction-time authority. It does not suppress that detector for future classes.

## SonarQube Cloud setup

The [Free plan](https://docs.sonarsource.com/sonarqube-cloud/administering-sonarcloud/managing-subscription/subscription-plans) supports public projects, main-branch analysis, and pull requests targeting the main branch. This repository uses `master`; choose that as the Sonar project's main branch. Paid plans are not required for this workflow.

1. Sign in with GitHub at [SonarQube Cloud](https://sonarcloud.io/projects/create), choose the Free plan, and import this repository.
2. Select GitHub Actions/Maven as the analysis method. Disable Automatic Analysis if enabled, because CI analysis must not run alongside it.
3. Store a personal analysis token as the repository Actions secret `SONAR_TOKEN`. Never commit or paste it into a PR.
4. Set repository Actions variables `SONAR_ORGANIZATION` and `SONAR_PROJECT_KEY` to the exact keys from setup. Set `SONAR_REGION` to `eu` (default) or `us` to match the account.
5. Run the workflow on `master` to establish a baseline, then verify PR analysis and the SonarQube quality gate before relying on it as a merge requirement.

Both key variables activate the job. Without them it is skipped, which means analysis is not configured and is not a Sonar pass. Once activated, a missing token or invalid region fails the job. Fork and Dependabot PRs are skipped because they cannot receive the credential; Java tests and local static checks still run. The workflow uses ordinary `pull_request`, never `pull_request_target`, and grants only repository content read access.

The pinned Maven scanner runs `verify` and analysis in one invocation, reads each module's default `target/site/jacoco/jacoco.xml`, and waits up to five minutes for the server quality gate. Java 17 is used for the build and supplied as the project JDK; the scanner provisions its own supported runtime. JaCoCo's existing algorithm thresholds remain enforced independently. A failed gate or scanner error fails the job. The [Maven scanner documentation](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/scanners/sonarscanner-for-maven) and [Java coverage documentation](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/java-test-coverage) describe these defaults.

## Verification progress

- [x] Existing formatting violations rejected before applying the formatter.
- [x] Checkstyle rejected 23 existing import/braces violations; those were corrected.
- [x] SpotBugs rejected a temporary definite null dereference (`NP_ALWAYS_NULL`); the probe was removed.
- [x] Restored source passes formatting, style, bug checks, all 114 tests, and coverage.
- [x] Initial PR head passed Java 17 CI and Java 21 quality CI; subsequent heads must pass again.
- [ ] Sonar account configuration and live analysis verified.
- [ ] Native Codex review of the final PR head completed without unresolved findings.

The initial live scan failed because the scanner also reads the `SONAR_REGION` environment variable and rejects the value `eu`. The workflow now reads the repository variable into `SONAR_ACCOUNT_REGION`; EU leaves the scanner setting unset, while US passes `-Dsonar.region=us`. Final live verification remains required.
