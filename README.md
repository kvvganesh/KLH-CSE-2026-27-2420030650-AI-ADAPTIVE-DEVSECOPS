# Adaptive DevSecOps Pipeline for Intelligent Vulnerability Assessment and Secure Deployment

A multi-page demonstration platform built with **Java 21 + Spring Boot 4 + Thymeleaf**. A project moves through a security pipeline (build, JUnit, SonarQube, Gitleaks, OWASP Dependency-Check). An explainable risk engine combines the results and the deployment decision adapts to the risk.

| Risk | Score | Decision |
|------|-------|----------|
| LOW | 0-29 | DEPLOYMENT APPROVED |
| MEDIUM | 30-59 | SECURITY REVIEW REQUIRED |
| HIGH | 60+ | DEPLOYMENT BLOCKED |

> **Two scan modes.** *Live local scan* clones a real repository and scans it with real tools (see below). *Safe / Medium / Critical* are simulated demo scenarios.

> **Simulated vs real.** Pipeline runs, scan results and deployments inside the web app are **simulated** (and labelled so) using three fixed scenarios. JUnit, SonarQube, Gitleaks and OWASP Dependency-Check run **for real** through Maven and GitHub Actions. The risk engine is **rule-based and explainable**, not machine learning. No real credentials or CVEs appear anywhere.

## Problem statement
Security tools produce separate reports that are read late or inconsistently, so pipelines either ignore findings or fail on every warning. This project converts the signals into one explainable score and an adaptive deployment decision.

## Features
- 12 separate pages: Login, Dashboard, Projects, Project Details, Pipeline, Security Scans, Vulnerability Assessment, Risk Analysis, Deployment Decision, Reports, Settings, Documentation
- Create/view projects (validated), run a simulated pipeline with animated stages
- Per-tool results: JUnit, SonarQube, Gitleaks, OWASP
- Vulnerability table with severity filter
- Risk score with per-reason points and recommended action
- Simulated deployment (approved / review / blocked)
- Downloadable HTML security reports
- One-click scenario switch (Safe / Medium / Critical) in the page header
- REST API, JUnit tests, and a GitHub Actions DevSecOps workflow

## Architecture
```
Browser (Thymeleaf pages + app.js)
   -> Controllers (PageController + REST controllers under /api)
   -> Services (Project, Pipeline, SecurityScan, RiskAssessment, Deployment, Report)
   -> Repositories (in-memory)
   -> Models (Project, PipelineRun, SecurityScan, Vulnerability, RiskAssessment, DeploymentDecision)
```
Data lives in memory, so **no database is needed** (it resets on restart and is re-seeded with three demo projects).

## Technology stack
Java 21, Spring Boot 4.0, Maven, Thymeleaf, HTML/CSS/JavaScript, JUnit, JaCoCo, SonarQube, Gitleaks, OWASP Dependency-Check, Git, GitHub, GitHub Actions.

## Project structure
```
adaptive-devsecops/
├── .github/workflows/devsecops.yml
├── .gitleaks.toml
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/example/devsecops/
    │   ├── controller/  PageController + Project/Pipeline/SecurityScan/RiskAssessment/Deployment/Report REST controllers
    │   ├── service/     business logic and the risk engine
    │   ├── repository/  in-memory repositories
    │   ├── model/       domain records and enums
    │   ├── dto/         request/response objects
    │   ├── config/      RiskProperties, DataInitializer (demo data)
    │   └── exception/   NotFound/Conflict + API and page error handlers
    ├── main/resources/
    │   ├── templates/   12 pages + fragments + error
    │   ├── static/      css, js, images
    │   └── application.properties
    └── test/            JUnit tests
```

## Prerequisites
- JDK 21
- Maven 3.9+
- Optional: Docker (for a local SonarQube), Gitleaks CLI, a free NVD API key

## Installation and configuration
```bash
git clone <your-repo-url>
cd adaptive-devsecops
```
Risk thresholds are configured in `src/main/resources/application.properties`:
```
devsecops.risk.medium-threshold=30
devsecops.risk.high-threshold=60
```

## How to run
```bash
mvn spring-boot:run
```
Open http://localhost:8080 and press **Enter platform**.

## How to run tests
```bash
mvn clean verify
```
Reports: `target/surefire-reports`, coverage: `target/site/jacoco/index.html`.

## How to run SonarQube
1. Start a server: `docker run -d --name sonarqube -p 9000:9000 sonarqube:community`
2. Open http://localhost:9000 (admin / admin, change the password), create a token under *My Account > Security*.
3. Run:
```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<your-token>
```
Settings (`sonar.projectKey`, coverage path) are in `pom.xml`. For SonarCloud also add `-Dsonar.organization=<org>` and use `https://sonarcloud.io` as host.

## How to run Gitleaks
```bash
gitleaks detect --source . --config .gitleaks.toml --verbose
```
Install: `brew install gitleaks` or download from https://github.com/gitleaks/gitleaks/releases. The config extends the default rules. Exit code 0 means no secrets found.

## How to run OWASP Dependency-Check
```bash
export NVD_API_KEY=<your-free-nvd-key>   # strongly recommended, otherwise the first run is very slow
mvn org.owasp:dependency-check-maven:check
```
The report is written to `target/dependency-check-report.html`. The build fails on CVSS 9.0 or higher (`failBuildOnCVSS` in `pom.xml`).

## How GitHub Actions works
`.github/workflows/devsecops.yml` runs on push to `main`, pull requests and manual dispatch. Jobs run in sequence:

1. **Build + JUnit**: Java 21, `mvn clean verify`, uploads test and coverage reports
2. **SonarQube**: runs analysis and waits for the quality gate
3. **Gitleaks**: scans the full git history
4. **OWASP Dependency-Check**: scans dependencies, uploads the report

Repository secrets: `SONAR_TOKEN` and `SONAR_HOST_URL` (SonarQube is skipped with a warning if missing; the server must be reachable from GitHub), `NVD_API_KEY` (recommended). Gitleaks needs no setup for personal repositories (organisation repositories need a `GITLEAKS_LICENSE` secret).

The web app does not run the workflow; it demonstrates the same stages with simulated data.


## Live local scan (real results)
Add a project with **Scan mode = Live local scan** (the default for new projects), using an `https://` repository URL or an absolute local folder path (for example `C:\\dev\\my-app`). Press **RUN PIPELINE**. The backend then:

1. clones the branch into `~/.adaptive-devsecops/workspaces/<projectId>/repo` (fresh each run)
2. runs `mvn -B test` and reads `target/surefire-reports` (JUnit)
3. runs `gitleaks detect --redact` (secrets; the secret value is never stored)
4. runs SonarQube analysis if `SONAR_HOST_URL` and `SONAR_TOKEN` are set, then reads the quality gate through the SonarQube API
5. runs OWASP Dependency-Check if `devsecops.scan.dependency-check-enabled=true`
6. feeds everything into the same risk engine and deployment decision

Requirements: Git and Maven on PATH (the target project must be Maven with a `pom.xml` in its root). Gitleaks is optional.

- A tool that is missing or disabled is shown as **Not scanned** and adds +10 to the risk score (unverified is not the same as safe).
- Logs for every tool are kept in `~/.adaptive-devsecops/workspaces/<projectId>/logs`.
- Scans can take minutes. Dependency-Check's first run downloads the NVD database and is very slow without a free `NVD_API_KEY`.
- **Security warning:** `mvn test` executes the scanned project's code on your machine. Only scan repositories you trust.
- Settings (`devsecops.scan.*`) are in `application.properties`.

Windows example to enable all tools:
```
set SONAR_HOST_URL=http://localhost:9000
set SONAR_TOKEN=<token>
set NVD_API_KEY=<key>
mvn spring-boot:run -Dspring-boot.run.arguments=--devsecops.scan.dependency-check-enabled=true
```

## Demo scenarios
Use the **Demo scenario** buttons in the header of the Pipeline, Scans, Vulnerabilities, Risk and Deployments pages. Each button sets the scenario for the selected project and runs a new pipeline.

| Scenario | Signals | Score | Result |
|----------|---------|-------|--------|
| Safe project | all tests pass, gate passes, 0 secrets, no critical/high/medium dependencies | 0 | LOW, approved |
| Medium risk | gate fails (+15), medium dependency (+15) | 30 | MEDIUM, review required |
| Critical risk | critical secret (+50), high dependency (+25), gate fail (+15), failed tests (+10) | 100 (capped) | HIGH, blocked |

Suggested 5-10 minute flow: Dashboard -> Projects -> Pipeline (RUN PIPELINE) -> Security Scans -> Vulnerability Assessment -> Risk Analysis -> Deployments (SIMULATE DEPLOYMENT) -> switch to Critical risk and repeat -> Reports (GENERATE REPORT).

## REST API
| Method | Path |
|--------|------|
| GET / POST | `/api/projects` |
| GET | `/api/projects/{id}` |
| PUT | `/api/projects/{id}/scenario` |
| POST | `/api/projects/{id}/pipeline` |
| GET | `/api/projects/{id}/pipelines`, `/api/pipelines` |
| GET | `/api/projects/{id}/security-scans` |
| GET | `/api/projects/{id}/vulnerabilities?severity=HIGH` |
| GET | `/api/projects/{id}/risk` |
| GET / POST | `/api/projects/{id}/deployment`, `/api/projects/{id}/deploy` |
| GET | `/api/reports`, `/api/reports/{runId}/html`, `/api/projects/{id}/report` |

Errors return JSON (`status`, `error`, `message`, `fieldErrors`) without stack traces: 400 validation, 404 not found, 409 conflict (for example deploying before any pipeline run).

## Screenshots
- Dashboard: `docs/screenshots/dashboard.png` (placeholder)
- Pipeline: `docs/screenshots/pipeline.png` (placeholder)
- Risk analysis: `docs/screenshots/risk.png` (placeholder)
- Deployment decision: `docs/screenshots/deployment.png` (placeholder)

## Future improvements
- Persist data in PostgreSQL
- Import real SonarQube, Gitleaks and Dependency-Check reports
- Trigger and read real GitHub Actions runs through the GitHub API
- Trend charts of risk over time and configurable scoring weights
- Real authentication and roles
