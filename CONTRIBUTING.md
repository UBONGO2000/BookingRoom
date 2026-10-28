# Contribuer à BookingRoom

Merci de l'intérêt porté au projet. Ce dépôt est avant tout un projet
d'apprentissage, mais les contributions propres et testées sont bienvenues.

## Prérequis

- Java 21
- Docker (pour PostgreSQL et l'image applicative), ou un PostgreSQL local
- Le wrapper Maven fourni (`./mvnw` / `mvnw.cmd`)

## Lancer le projet en local

```bash
cp .env.example .env          # puis ajuster les valeurs
docker compose up -d db       # PostgreSQL seul
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` crée les comptes et les salles de démonstration.
Les identifiants sont documentés dans le README.

## Avant d'ouvrir une pull request

```bash
./mvnw clean verify
```

Cette commande compile, exécute les tests et vérifie les seuils JaCoCo
(80 % instructions, 80 % lignes, 65 % branches).

## Conventions

- Une branche par sujet (`feat/...`, `fix/...`, `docs/...`) et des commits
  au format `type(scope): message`.
- Respecter l'architecture en couches existante (`controller`, `service`,
  `repository`, `model`, `dto`, `security`, `config`).
- Ne jamais commiter `.env`, `doc.md`, un mot de passe ou une clé.
- Toute nouvelle règle métier doit être couverte par un test.
- Mettre à jour le README si le comportement ou la configuration change.

## Signaler un problème

Ouvrir une issue en précisant les étapes de reproduction, le comportement
attendu et les logs utiles (sans secret ni donnée personnelle).
