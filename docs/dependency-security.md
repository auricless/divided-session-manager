# Service dependency security verification

Checked 2026-10-22 while preparing SM-20 / PR #11. Sonar's passing code quality gate also reported dependency risks, so dependency versions were checked separately through the public [OSV API](https://google.github.io/osv.dev/api/).

## Findings and correction

| Component | Boot 4.1.1 managed version | Compatible patched version |
| --- | --- | --- |
| Jackson Core / Databind | 3.1.5 | 3.1.8 |
| Tomcat Embed Core / EL / WebSocket | 11.0.24 | 11.0.27 |
| Logback Classic / Core | 1.5.38 | 1.6.5 |
| SLF4J API / bridges | 2.0.18 | 2.0.19 |

The original resolved graph matched ten advisories across three artifacts: Jackson Databind (GHSA-cxp5-3px4-pw24, GHSA-gx83-3vf8-gh7j, GHSA-q4xh-88c3-wmh7, GHSA-wjgm-6hv5-3cvf, GHSA-wv8q-qhhj-9h54), Jackson Core (GHSA-7hhh-6rmp-j9qf, GHSA-p6pp-m3f8-5c89), and Tomcat Embed Core (GHSA-9xv2-5v5q-p794, GHSA-gcx9-497g-6cp6, GHSA-h3x4-894j-xpx5).

Upstream examples: [Jackson Databind quadratic forward-reference completion](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-cxp5-3px4-pw24), [Jackson Core unbounded error-token parsing](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf), and [Tomcat 11 security advisories](https://tomcat.apache.org/security-11.html).

The service imports Jackson's patched BOM before Boot's BOM and explicitly manages all three embedded Tomcat artifacts at the same patch version. Setting a property alone cannot override versions inside an imported Boot BOM. Keep these patches service-local; the core stays independent of Spring, Jackson, and Tomcat. Remove the overrides once an adopted Boot BOM supplies compatible fixed or newer versions.

## Verification

- Before the change, an OSV batch query of all 68 resolved public Maven dependencies matched affected versions in three artifacts.
- After the change, Maven resolves Jackson Core/Databind 3.1.8 and all three embedded Tomcat artifacts at 11.0.27. A fresh query of all 68 coordinates returns no matching affected dependencies.
- Full `mvn -Pquality clean verify` passes 115 tests, formatting, Checkstyle, SpotBugs and coverage; the startup test invokes the actual application main method.
- Java 17/21 CI, Sonar and current-head native review must pass again before merge.

This is a dated dependency-version check, not proof that the service has no vulnerabilities. Advisory databases and reachable application paths differ from Sonar's proprietary dependency-risk analysis; the original Sonar count was 11. No paid security product was enabled to perform this check.

To refresh the resolved graph from the repository root after installing the core, use `mvn -pl session-manager-service dependency:list`. Public OSV queries identify each package by ecosystem `Maven`, name `groupId:artifactId`, and resolved version. Recheck the complete graph after changing either BOM.

## Remaining Sonar finding: CVE-2026-104721

Authenticated Sonar CLI readback identified the remaining LOW-severity risk as Logback Classic 1.5.38, CVE-2026-104721 / CWE-22. The upstream [Logback 1.6.5 announcement](https://logback.qos.ch/news.html#1.6.5) identifies the fix for unsafe MDC values used in log filename segments. The default service logging does not configure a sifting file appender; the regression tests exercise that dependency API directly to prevent reintroducing the affected version.

Five traversal/variable-substitution input cases failed against the previous dependency; two safe-name cases passed. After aligning Logback Classic/Core to 1.6.5 and its SLF4J dependencies to 2.0.19, all seven cases and the full 122-test bootstrap quality build pass. Logback's published POM targets Java 11, compatible with the service's Java 17 runtime. Fresh Java 17 CI, Sonar risk readback, and native review are still required after this change.

This finding was not present as a package-version match in the public OSV result. Sonar and public advisory sources must both be checked; a zero result from one database cannot stand in for the other.

## Authenticated terminal access

The official SonarQube CLI (`sonar`) is installed locally and authenticated to the EU organization `auricless` using the OS keychain. No token is stored in this repository. Use `sonar auth status` to verify access and `sonar api get '/sca/issues-releases?projectKey=auricless_divided-session-manager&pullRequestKey=11&newlyIntroduced=true&pageSize=100'` to retrieve PR dependency-risk details. Substitute the actual PR number. The CLI routes Cloud v2 API requests and supplies credentials without printing them. Account authentication still needs a one-time sign-in when a session is first connected or expires.
