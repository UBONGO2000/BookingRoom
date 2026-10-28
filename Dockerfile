# syntax=docker/dockerfile:1

# Image de build epinglee par digest (reproductibilite).
FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
# Les tests tournent dans l'image (pas de -DskipTests) ; la CI les rejoue.
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package

# Image d'execution epinglee par digest.
FROM eclipse-temurin:21-jre-jammy@sha256:affcab5356e1270ac35d2361ee83135af2f03e8d580eca78a6d5d6fdbf048db0
WORKDIR /app

# curl pour le HEALTHCHECK, tzdata pour Europe/Paris.
RUN groupadd --system app \
    && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && ln -fs /usr/share/zoneinfo/Europe/Paris /etc/localtime \
    && dpkg-reconfigure --frontend noninteractive tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/Paris
# Profil obligatoire : pas de compte de demo, schema valide, secrets via variables d'environnement.
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build --chown=app:app /app/target/*.jar app.jar

# L'application ne tourne jamais en root.
USER app
EXPOSE 8080
# Render (et la plupart des PaaS) imposent le port via PORT : le healthcheck
# doit interroger le port reellement ecoute, pas 8080 en dur.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD sh -c 'curl -fsS "http://localhost:${PORT:-8080}/actuator/health" || exit 1'

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
