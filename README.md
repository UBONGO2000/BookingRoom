# BookingRoom

Un projet Spring Boot que j'ai monté pour progresser sur l'écosystème Spring (Security, Data JPA, Thymeleaf...) en construisant quelque chose d'un peu plus consistant qu'un CRUD de démo : un outil de réservation de salles de réunion, avec comptes utilisateurs, espace admin, et une vraie couche de sécurité derrière.

Stack : Java 21, Spring Boot 3.5, Spring Security, Thymeleaf, PostgreSQL, Docker, tests automatisés.

***

## Ce que ça fait

- Connexion / inscription, avec deux rôles : utilisateur (`USER`) et administrateur (`ADMIN`).
- Gestion des salles côté admin : création, modification, suppression, activation/désactivation, équipements (projecteur, tableau blanc, visioconférence).
- Réservation côté utilisateur : recherche par nom, capacité et équipements, réservation d'un créneau, annulation logique (la réservation reste dans l'historique avec le statut `CANCELLED`).
- Une réservation est confirmée immédiatement si le créneau est libre : il n'y a pas de workflow de validation par un administrateur. La recherche de salles ne filtre pas par date ou par créneau ; la disponibilité est vérifiée au moment de la réservation.
- L'admin voit et peut annuler n'importe quelle réservation, avec un filtre par salle ou par utilisateur.
- Rendu entièrement côté serveur avec Thymeleaf — pas de framework JS, tout est généré par le backend.
- Politique de mot de passe assez stricte à l'inscription (majuscule, minuscule, chiffre, caractère spécial).
- Une API REST basée sur la session (pas de JWT) pour les salles, les réservations, les statistiques et le profil.

### Sur ma liste, pas encore fait

- [x] Une petite API REST (`/api/**`) pour les salles, les réservations, les stats et les données utilisateur — déjà en place.
- [ ] Confirmation par email après une réservation.
- [ ] Un vrai calendrier visuel (FullCalendar ou équivalent) plutôt qu'une simple liste.
- [ ] Export PDF d'une réservation.
- [ ] Notifications push — franchement pas prioritaire pour un outil de cette échelle, je le laisse en bas de la pile.
- [ ] Notes/avis sur les salles. Il n'y a pas d'entité `Rating`/`Evaluation` dans le modèle : le champ `evaluations` de `GET /api/rooms/{id}` est un tableau vide explicitement laissé comme placeholder.

***

## Comment c'est organisé

Architecture en couches assez classique pour du Spring Boot :

- `controller` — les endpoints, web (Thymeleaf) et REST.
- `service` — la logique métier (réservations, salles, utilisateurs).
- `repository` — les interfaces Spring Data JPA.
- `model` — les entités JPA (`User`, `Room`, `Booking`...).
- `dto` — les objets utilisés pour les formulaires et les réponses JSON.
- `security` — la partie anti brute-force sur le login (filtre + service de verrouillage).

### Le modèle en gros

- **User** : identifiant, infos perso, mot de passe haché, rôle (`USER` ou `ADMIN`).
- **Room** : nom, capacité, équipements, disponibilité, éventuelle plage `availableFrom`/`availableUntil`, description.
- **Booking** : utilisateur, salle, titre, `startTime`, `endTime`, `createdAt`, statut et `cancelledAt`.
- **BookingStatus** : `CONFIRMED` (la réservation bloque le créneau) ou `CANCELLED` (annulation logique : la ligne reste en base pour l'historique, mais ne bloque plus le créneau).
- Il n'y a pas d'entité `Rating`/`Evaluation` : les notes/avis ne sont pas implémentés.

***

## Stack technique

- **Backend** : Spring Boot 3.5 (Java 21), Spring MVC, Spring Data JPA, Spring Security, Spring Validation.
- **Frontend** : Thymeleaf + CSS fait main (pas de Bootstrap ni Tailwind).
- **Base de données** : PostgreSQL (H2 en mémoire pour les tests).
- **Conteneurisation** : Docker.
- **Tests** : JUnit 5, Spring Boot Test, Mockito, MockMvc, JaCoCo pour la couverture — sur les services et sur les contrôleurs (rôles, autorisations, protection contre l'IDOR sur les réservations).

***

## Lancer le projet

### Avant de commencer

- Java 21
- Maven (ou le wrapper `./mvnw` déjà dans le repo, pas besoin d'installer Maven à part)
- Docker si tu veux éviter d'installer PostgreSQL à la main

### Avec Docker

Crée le fichier `.env` à partir du modèle, puis renseigne `DB_PASSWORD` (obligatoire) :

```bash
cp .env.example .env
docker compose up --build
```

Le fichier `docker-compose.yml` démarre PostgreSQL et l'app en profil `prod` (aucun compte de démo), avec PostgreSQL dans un volume dédié. Le healthcheck est exposé sur `http://localhost:8080/actuator/health` ; `docker compose ps` doit afficher les deux services `healthy`. Le port publié peut être changé avec `APP_PORT` (par défaut `8080`).

### En local, sans Docker

```bash
git clone https://github.com/UBONGO2000/BookingRoom.git
cd BookingRoom
```

Crée une base PostgreSQL nommée `bookingroom`, puis renseigne les identifiants :

```bash
cp .env.example .env   # fichier ignoré par Git, chargé automatiquement par Spring Boot
```

(ou exporte `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` dans le shell)

Lance ensuite avec le profil `dev` (nécessaire pour créer les données de démonstration) :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Ouvre `http://localhost:8080`.

Sans profil `dev`, l'application démarre avec la configuration commune mais ne crée ni comptes de démo ni salles.

### Profils

| Profil | Usage | Comportement |
|---|---|---|
| `dev` | développement local | Seed des comptes de démo et de 15 salles, SQL loggé, cookies non `Secure`. À ne pas utiliser comme profil de production. |
| `prod` | Docker / production | `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` obligatoires sans valeur de repli, aucun seed, SQL non loggé, `flyway clean` interdit, cookies `Secure`, prise en compte de `X-Forwarded-Proto`. |
| `test` | tests automatisés | Base H2 en mémoire, migrations Flyway, pas de seed, SQL non loggé. |
| *(aucun)* | démarrage local minimal | Configuration commune, pas de seed, PostgreSQL local par défaut, SQL loggé. |

Le profil est choisi avec `SPRING_PROFILES_ACTIVE` (ou `-Dspring-boot.run.profiles=dev` avec Maven). Le `Dockerfile` force `prod`.

### Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | *(aucun)* | `dev`, `prod` ou `test`. |
| `APP_NAME` | `BookingRoom` | Nom Spring de l'application. |
| `PORT` | `8080` | Port HTTP interne de l'application. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bookingroom` en dev | URL JDBC. Obligatoire en `prod`. |
| `DB_USERNAME` | `postgres` en dev | Utilisateur PostgreSQL. Obligatoire en `prod`. |
| `DB_PASSWORD` | `password` en dev | Mot de passe PostgreSQL. Obligatoire en `prod` et avec Compose. |
| `SEED_DEFAULT_USERS` | `true` en `dev` | Active le seed des comptes de démo. Forcé à `false` en `prod`. |
| `SEED_ADMIN_USERNAME` | `admin` | Nom du compte admin de démo. |
| `SEED_ADMIN_EMAIL` | `admin@gmail.com` | Email du compte admin de démo. |
| `SEED_ADMIN_PASSWORD` | `Admin@123` | Mot de passe du compte admin de démo. |
| `SEED_USER_USERNAME` | `user` | Nom du compte utilisateur de démo. |
| `SEED_USER_EMAIL` | `user@gmail.com` | Email du compte utilisateur de démo. |
| `SEED_USER_PASSWORD` | `User@123` | Mot de passe du compte utilisateur de démo. |
| `LOGIN_MAX_ATTEMPTS` | `5` | Échecs avant verrouillage d'un compte. |
| `LOGIN_LOCK_DURATION_MINUTES` | `15` | Durée du verrouillage d'un compte. |
| `LOGIN_RATE_LIMIT_MAX` | `20` | Nombre de tentatives de login par IP et par fenêtre. |
| `LOGIN_RATE_LIMIT_WINDOW` | `60` | Durée de la fenêtre de rate limiting, en secondes. |
| `COOKIE_SECURE` | `false` en dev, `true` en `prod` | Attribut `Secure` des cookies de session et CSRF. |
| `APP_PORT` | `8080` | Port publié par `docker compose` (variable Compose uniquement). |

Le fichier `.env` est chargé automatiquement par Spring Boot via `spring.config.import` ; Docker Compose le lit aussi pour l'interpolation. Les variables d'environnement du système restent prioritaires.

### Migrations de base

Le schéma est versionné avec Flyway dans `src/main/resources/db/migration` : les scripts communs (`common/`) et une variante par moteur (`h2/` pour les tests, `postgresql/` pour la prod). `ddl-auto=validate` vérifie que les entités correspondent aux migrations, sans jamais modifier le schéma. Une base existante sans historique Flyway est baselinée en V1, puis les migrations suivantes sont appliquées au démarrage.

### Comptes de démo

Créés automatiquement au premier démarrage **en profil `dev` uniquement** (aucun compte en `prod`) :

| Rôle | Utilisateur | Mot de passe |
|------|-------------|---------------|
| Utilisateur | `user` | `User@123` |
| Administrateur | `admin` | `Admin@123` |

Ce seed peut être désactivé (`SEED_DEFAULT_USERS=false`) ou personnalisé via variables d'environnement (`SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`, etc.) — utile le jour où ça sort de mon poste local. Le seed ne remplace pas le mot de passe d'un compte déjà existant ; il ne met à jour que son rôle si nécessaire.

**Règles pour un nouveau mot de passe** (à l'inscription) : 8 caractères minimum, une majuscule, une minuscule, un chiffre, un caractère spécial (`@$!%*?&`). Le nom d'utilisateur, lui, n'accepte que lettres, chiffres et underscores.

***

## Les routes

Les pages HTML protégées redirigent vers `/login` (302) quand la session est absente. Les formulaires utilisent le pattern POST-Redirect-GET : une action réussie renvoie une redirection 302, une erreur de validation réaffiche la page en 200. Un utilisateur authentifié qui n'a pas le rôle requis reçoit une 403. Toute requête `POST` sans jeton CSRF valide reçoit également une 403.

| Route | Méthode | Accès | Réponse / codes |
|---|---|---|---|
| `/` | GET | public | 200 |
| `/login` | GET | public | 200 |
| `/login` | POST | public | 302 vers `/dashboard` si succès ; 302 vers `/login?error`, `?locked` ou `?ratelimited` en cas d'échec |
| `/logout` | POST | authentifié | 302 vers `/login?logout`, session invalidée |
| `/register` | GET | public | 200 |
| `/register` | POST | public | 302 vers `/login?registered` si succès ; 200 avec le formulaire si erreurs |
| `/dashboard` | GET | USER / ADMIN | 200 ; 302 vers `/login` si anonyme |
| `/booking` | GET | USER / ADMIN | 200 (liste + recherche paginée) ; 302 vers `/login` si anonyme |
| `/booking/{id}` | GET | USER / ADMIN | 200 ; 404 si la salle n'existe pas |
| `/booking/confirm` | POST | USER / ADMIN | 302 vers `/dashboard` si succès ; 302 vers `/booking/{id}` si erreur métier ; 200 avec le formulaire si validation ; 404 si la salle n'existe pas |
| `/booking/cancel/{id}` | POST | USER / ADMIN, propriétaire ou ADMIN | 302 vers `/dashboard` ; 403 si ni propriétaire ni admin ; 404 si la réservation n'existe pas |
| `/admin` | GET | ADMIN | 200 ; 403 si USER authentifié ; 302 vers `/login` si anonyme |
| `/admin/rooms/new` | GET | ADMIN | 200 |
| `/admin/rooms/save` | POST | ADMIN | 302 vers `/admin` si succès ; 200 avec le formulaire si erreurs |
| `/admin/rooms/edit/{id}` | GET | ADMIN | 200 ; 404 si la salle n'existe pas |
| `/admin/rooms/delete/{id}` | POST | ADMIN | 302 vers `/admin` ; message flash si la suppression est refusée |
| `/admin/rooms/toggle-availability/{id}` | POST | ADMIN | 302 vers `/admin` |
| `/actuator/health` | GET | public | 200 si `UP`, 503 si `DOWN` |

Les suppressions et changements d'état sont volontairement en `POST`, jamais en `GET` — un lien qui modifie des données au simple survol ou preload, c'est une porte ouverte que je préfère fermer.

### API REST

Les routes `/api/**` s'appuient sur la même session que le site (pas de JWT pour l'instant). Toutes exigent une session authentifiée, sauf `POST /api/auth/login` et `GET /api/csrf`. Les URLs historiques utilisent `reservations` alors que l'entité interne s'appelle `Booking`.

Le CSRF est actif partout, y compris sur `/api/**` : seul `POST /api/auth/login` est exempté. Pour les autres écritures, le client récupère le token via `GET /api/csrf`, puis l'envoie dans l'en-tête `X-XSRF-TOKEN` (le cookie `XSRF-TOKEN` est lisible par le JS). Les formulaires Thymeleaf incluent automatiquement le champ caché `_csrf`.

| Endpoint | Méthode | Accès | Réponse / codes |
|---|---|---|---|
| `/api/csrf` | GET | public | 200 `{"token":"..."}` ; pose aussi le cookie `XSRF-TOKEN` |
| `/api/auth/login` | POST | public, exempté de CSRF | 200 `{"username":"..."}` ; 400 si champs manquants ; 401 si identifiants invalides ; 429 si compte verrouillé ou IP limitée (`Retry-After`) |
| `/api/rooms` | GET | authentifié | 200, page JSON des salles disponibles ; filtres `name`, `capacity`, `projector`, `whiteboard`, `videoconferencing`, `page` (0) et `size` (10) ; 401 si anonyme |
| `/api/rooms/{id}` | GET | authentifié | 200 `{"room":{...},"evaluations":[]}` ; 404 si inconnue ; 401 si anonyme |
| `/api/reservations/me` | GET | authentifié | 200, liste de mes réservations ; alias `/api/my-bookings` ; 401 si anonyme |
| `/api/reservations` | POST | authentifié + CSRF | 200 `{"id":...,"message":"..."}` ; 400 pour une validation, une salle inconnue ou une règle métier ; 409 en cas de conflit de créneau ; 401 si anonyme ; 403 si CSRF invalide |
| `/api/stats/overview` | GET | ADMIN | 200 ; 403 si USER ; 401 si anonyme |
| `/api/rooms/available-now` | GET | authentifié | 200, salles disponibles dans l'heure qui vient ; 401 si anonyme |
| `/api/me` | GET | authentifié | 200, profil courant ; 404 si l'utilisateur n'existe plus ; 401 si anonyme |

Les erreurs applicatives sont renvoyées en JSON sous la forme `{"error":"..."}`. Un 401 produit par le filtre de sécurité peut avoir un corps vide ; les 400/403/409 applicatifs sont, eux, JSON. À ce stade, `POST /api/reservations` répond 200 avec l'identifiant créé ; le 201 avec en-tête `Location` n'est pas encore implémenté.

***

## Sécurité

Ce qui est en place aujourd'hui :

- **CSRF** activé partout, y compris sur `/api/**` : seul `POST /api/auth/login` est exempté. Le client récupère le token via `GET /api/csrf` (cookie `XSRF-TOKEN`) et l'envoie dans l'en-tête `X-XSRF-TOKEN`. Les formulaires HTML utilisent le champ `_csrf` généré par Thymeleaf.
- **Mots de passe** hachés en BCrypt.
- **Rôles** USER/ADMIN avec séparation stricte des routes — vérifiée par des tests, pas juste "ça marche chez moi".
- **Anti brute-force** : un compte se verrouille 15 minutes après 5 échecs de connexion (formulaire et API), seuils réglables via `LOGIN_MAX_ATTEMPTS` et `LOGIN_LOCK_DURATION_MINUTES`. Les tentatives sont aussi limitées par IP (`LOGIN_RATE_LIMIT_MAX`, `LOGIN_RATE_LIMIT_WINDOW`).
- **En-têtes de sécurité** : HSTS (sur HTTPS), CSP stricte sans `unsafe-inline` (styles et scripts externalisés), `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` (caméra/micro/géolocalisation désactivés), `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`.
- **Cookies** : session `HttpOnly` + `SameSite=Lax` + `Secure` en prod ; cookie CSRF `SameSite=Lax` et lisible par le JS (nécessaire pour l'API), `Secure` en prod.
- **Gestion d'erreurs séparée** : les pages web reçoivent une page d'erreur HTML, l'API reçoit du JSON — pas de page HTML renvoyée à un client mobile qui attendait une réponse structurée.
- **Profil `prod`** (`SPRING_PROFILES_ACTIVE=prod`, déjà positionné dans le `Dockerfile`) : identifiants **sans valeur de repli** (l'application refuse de démarrer si `DB_URL`, `DB_USERNAME` ou `DB_PASSWORD` manquent), SQL non loggé, `ddl-auto=validate` partout et `flyway clean` interdit. Les défauts du fichier commun restent orientés dev pour ne pas se prendre les pieds dans le tapis en local.
- Déconnexion propre : session invalidée, cookies supprimés.

Ce qu'il resterait à faire avant un vrai déploiement en prod :

- Activer HTTPS derrière un reverse proxy (HSTS et cookies `Secure` ne s'appliquent qu'en HTTPS).
- Vérifier que le proxy transmet bien `X-Forwarded-Proto` (déjà traité par `server.forward-headers-strategy=framework`).
- Lancer un scan ZAP complet sur l'environnement cible.

***

## Tests

```bash
./mvnw test
# ou, pour rejouer les tests et régénérer la couverture :
./mvnw clean verify
```

115 tests répartis sur 21 classes. Couverture de code (JaCoCo, générée automatiquement dans `target/site/jacoco/index.html`) :

| Périmètre | Instructions | Lignes |
|---|---|---|
| Ensemble du projet | 87,9 % | 87,5 % |
| Couche service | 96,4 % | 97,9 % |
| Couche contrôleur | 79,8 % | 82,7 % |

La couche service est bien couverte (logique métier : recherche de salles, création de réservation, règles de disponibilité, annulation logique). Les contrôleurs le sont moins pour l'instant — j'ai ciblé volontairement les points les plus faciles à casser sans s'en rendre compte : que les routes admin refusent bien un utilisateur non-admin, qu'un utilisateur ne peut pas annuler la réservation de quelqu'un d'autre juste en devinant son id (protection IDOR), que deux créations simultanées sur le même créneau n'en laissent passer qu'une, et que l'inscription valide bien la confirmation du mot de passe. Les recherches paginées sont aussi testées : filtres conservés dans les liens, tri déterministe, et intégration avec plus de 100 salles pour vérifier que le filtrage reste en base. `HomeController` n'a pas encore de tests dédiés — prochaine étape logique si je continue à faire monter ce chiffre.

***

## Backlog technique

Des pistes plus concrètes pour les points encore non cochés plus haut :

- **Emails** : Spring Mail + template Thymeleaf pour la confirmation/annulation.
- **Calendrier** : FullCalendar côté JS, consommant un endpoint `/api/calendar` à créer.
- **PDF** : un service dédié + route `/reservations/{id}/pdf`.
- **Notes/avis** : un formulaire sur la page de détail d'une salle, plus le calcul de la moyenne. Il faudra d'abord créer l'entité `Rating`/`Evaluation` correspondante ; aujourd'hui le champ `evaluations` de l'API reste vide.
- **Push** : à voir si ça vaut vraiment le coup pour un outil de cette taille avant de s'attaquer à la complexité que ça implique (service worker, gestion des tokens...).
