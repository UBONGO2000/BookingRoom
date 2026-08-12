# BookingRoom

Un projet Spring Boot que j'ai monté pour progresser sur l'écosystème Spring (Security, Data JPA, Thymeleaf...) en construisant quelque chose d'un peu plus consistant qu'un CRUD de démo : un outil de réservation de salles de réunion, avec comptes utilisateurs, espace admin, et une vraie couche de sécurité derrière.

Stack : Java 17, Spring Boot, Spring Security, Thymeleaf, PostgreSQL, Docker, tests automatisés.

***

## Ce que ça fait

- Connexion / inscription, avec deux rôles : utilisateur et administrateur.
- Gestion des salles côté admin : création, modification, suppression, équipements (projecteur, tableau blanc, visioconférence).
- Réservation côté utilisateur : recherche par capacité/équipement, réservation d'un créneau, annulation.
- L'admin voit et peut annuler n'importe quelle réservation, avec un filtre par salle ou par utilisateur.
- Rendu entièrement côté serveur avec Thymeleaf — pas de framework JS, tout est généré par le backend.
- Politique de mot de passe assez stricte à l'inscription (majuscule, minuscule, chiffre, caractère spécial).

### Sur ma liste, pas encore fait

- [x] Une petite API REST (`/api/**`) pour les stats et les données utilisateur — déjà en place.
- [ ] Confirmation par email après une réservation.
- [ ] Un vrai calendrier visuel (FullCalendar ou équivalent) plutôt qu'une simple liste.
- [ ] Export PDF d'une réservation.
- [ ] Notifications push — franchement pas prioritaire pour un outil de cette échelle, je le laisse en bas de la pile.
- [ ] Notes/avis sur les salles (l'API a déjà un placeholder qui attend cette fonctionnalité).

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

- **User** : identifiant, infos perso, mot de passe haché, rôle.
- **Room** : nom, capacité, équipements, disponibilité, description.
- **Booking** : utilisateur, salle, créneau, titre.

***

## Stack technique

- **Backend** : Spring Boot (Java 17), Spring MVC, Spring Data JPA, Spring Security, Spring Validation.
- **Frontend** : Thymeleaf + CSS fait main (pas de Bootstrap ni Tailwind).
- **Base de données** : PostgreSQL (H2 en mémoire pour les tests).
- **Conteneurisation** : Docker.
- **Tests** : JUnit 5, Spring Boot Test, Mockito, MockMvc, JaCoCo pour la couverture — sur les services et sur les contrôleurs (rôles, autorisations, protection contre l'IDOR sur les réservations).

***

## Lancer le projet

### Avant de commencer

- Java 17
- Maven (ou le wrapper `./mvnw` déjà dans le repo, pas besoin d'installer Maven à part)
- Docker si tu veux éviter d'installer PostgreSQL à la main

### Avec Docker

```bash
docker-compose up --build
```
Puis direction `http://localhost:8080`.

### En local, sans Docker

```bash
git clone https://github.com/UBONGO2000/BookingRoom.git
cd BookingRoom
```

Crée une base PostgreSQL nommée `bookingroom`, renseigne l'utilisateur/mot de passe dans `application.properties` (ou via les variables d'environnement `DB_USERNAME`/`DB_PASSWORD`), puis :

```bash
./mvnw spring-boot:run
```

Ouvre `http://localhost:8080`.

### Comptes de démo

Créés automatiquement au premier démarrage :

| Rôle | Utilisateur | Mot de passe |
|------|-------------|---------------|
| Utilisateur | `user` | `User@123` |
| Administrateur | `admin` | `Admin@123` |

Ce seed peut être désactivé (`SEED_DEFAULT_USERS=false`) ou personnalisé via variables d'environnement (`SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`, etc.) — utile le jour où ça sort de mon poste local.

**Règles pour un nouveau mot de passe** (à l'inscription) : 8 caractères minimum, une majuscule, une minuscule, un chiffre, un caractère spécial (`@$!%*?&`). Le nom d'utilisateur, lui, n'accepte que lettres, chiffres et underscores.

***

## Les routes

| Route | Méthode | Ce qu'elle fait |
|---|---|---|
| `/` | GET | Page d'accueil |
| `/login` | GET / POST | Page de connexion / traitement |
| `/logout` | POST | Déconnexion |
| `/register` | GET / POST | Inscription |
| `/dashboard` | GET | Tableau de bord utilisateur |
| `/booking` | GET / POST | Liste des salles + recherche |
| `/booking/{id}` | GET | Détail d'une salle |
| `/booking/confirm` | POST | Confirmer une réservation |
| `/booking/cancel/{id}` | POST | Annuler une réservation (propriétaire ou admin uniquement) |
| `/admin` | GET | Tableau de bord admin |
| `/admin/rooms/new` | GET | Formulaire de création de salle |
| `/admin/rooms/save` | POST | Créer / modifier une salle |
| `/admin/rooms/edit/{id}` | GET | Formulaire de modification |
| `/admin/rooms/delete/{id}` | POST | Supprimer une salle |
| `/admin/rooms/toggle-availability/{id}` | POST | Activer / désactiver une salle |

Les suppressions et changements d'état sont volontairement en `POST`, jamais en `GET` — un lien qui modifie des données au simple survol ou preload, c'est une porte ouverte que je préfère fermer.

### API REST

Les routes `/api/**` s'appuient sur la même session que le site (pas de JWT pour l'instant), et le CSRF y est désactivé pour simplifier une future intégration mobile.

- `POST /api/auth/login` — authentification JSON (`username`, `password`).
- `GET /api/rooms` — liste des salles, filtrable (`name`, `capacity`, `projector`, `whiteboard`, `videoconferencing`, `page`, `size`).
- `GET /api/rooms/{id}` — détail d'une salle (le champ `evaluations` est un placeholder, la fonctionnalité n'existe pas encore côté modèle).
- `GET /api/reservations/me` (alias `/api/my-bookings`) — mes réservations.
- `POST /api/reservations` — créer une réservation (`title`, `startTime`, `endTime`, `roomId`, dates au format ISO).
- `GET /api/stats/overview` — quelques statistiques globales.
- `GET /api/rooms/available-now` — salles libres dans l'heure qui vient.
- `GET /api/me` — mon profil.

***

## Sécurité

Ce qui est en place aujourd'hui :

- **CSRF** activé partout, sauf sur `/api/**`.
- **Mots de passe** hachés en BCrypt.
- **Rôles** USER/ADMIN avec séparation stricte des routes — vérifiée par des tests, pas juste "ça marche chez moi".
- **Anti brute-force** : un compte se verrouille 15 minutes après 5 échecs de connexion (formulaire et API), seuils réglables via `LOGIN_MAX_ATTEMPTS` et `LOGIN_LOCK_DURATION_MINUTES`.
- **Gestion d'erreurs séparée** : les pages web reçoivent une page d'erreur HTML, l'API reçoit du JSON — pas de page HTML renvoyée à un client mobile qui attendait une réponse structurée.
- **Profil `prod`** (`SPRING_PROFILES_ACTIVE=prod`) qui coupe le logging SQL et passe Hibernate en `validate` au lieu de `update`. Les valeurs par défaut restent orientées dev pour ne pas se prendre les pieds dans le tapis en local.
- Déconnexion propre : session invalidée, cookies supprimés.

Ce qu'il resterait à faire avant un vrai déploiement en prod :

- Activer HTTPS (question d'infra, pas de code ici).
- Ajouter des headers de sécurité supplémentaires (HSTS, CSP...).
- S'assurer que la base cible est bien PostgreSQL, pas H2.

***

## Tests

```bash
./mvnw test
```

57 tests répartis sur 10 classes. Couverture de code (JaCoCo, générée automatiquement dans `target/site/jacoco/index.html`) :

| Périmètre | Instructions | Lignes |
|---|---|---|
| Ensemble du projet | 61,5 % | 59,0 % |
| Couche service | 84,9 % | 96,6 % |
| Couche contrôleur | 25,8 % | 30,0 % |

La couche service est bien couverte (logique métier : recherche de salles, création de réservation, règles de disponibilité). Les contrôleurs le sont beaucoup moins pour l'instant — j'ai ciblé volontairement les deux points les plus faciles à casser sans s'en rendre compte : que les routes admin refusent bien un utilisateur non-admin, et qu'un utilisateur ne peut pas annuler la réservation de quelqu'un d'autre juste en devinant son id (protection IDOR). `AuthController` et `HomeController` n'ont pas encore de tests dédiés — prochaine étape logique si je continue à faire monter ce chiffre.

***

## Backlog technique

Des pistes plus concrètes pour les points encore non cochés plus haut :

- **Emails** : Spring Mail + template Thymeleaf pour la confirmation/annulation.
- **Calendrier** : FullCalendar côté JS, consommant un endpoint `/api/calendar` à créer.
- **PDF** : un service dédié + route `/reservations/{id}/pdf`.
- **Notes/avis** : un formulaire sur la page de détail d'une salle, plus le calcul de la moyenne. C'est la pièce qui manque pour que le champ `evaluations` de l'API arrête d'être un mensonge poli.
- **Push** : à voir si ça vaut vraiment le coup pour un outil de cette taille avant de s'attaquer à la complexité que ça implique (service worker, gestion des tokens...).
