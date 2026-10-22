# Service dependency security verification

Checked 2026-10-22 while preparing SM-20 / PR #11. Sonar's passing code quality gate also reported dependency risks, so dependency versions were checked separately through the public [OSV API](https://google.github.io/osv.dev/api/).

## Findings and correction

| Component | Boot 4.1.1 managed version | Compatible patched version |
| --- | --- | --- |
| Jackson Core / Databind | 3.1.5 | 3.1.8 |
| Tomcat Embed Core / EL / WebSocket | 11.0.24 | 11.0.27 |

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
