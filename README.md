# BookingRoom

Application Spring Boot pour la **réservation de salles** avec authentification sécurisée, espace administrateur, gestion des réservations et recherche avancée. Stack : Java 17, Spring Security, Thymeleaf, PostgreSQL, Docker et tests automatisés.

***

## Fonctionnalités

- Authentification et gestion des utilisateurs avec rôles (utilisateur / administrateur).
- Gestion des salles : création, modification, suppression, capacité et équipements.
- Gestion des réservations : création, consultation, annulation par l'utilisateur et validation par l'admin.
- Recherche avancée des salles par date, créneau horaire, capacité et caractéristiques.
- Interface web côté serveur rendue avec **Thymeleaf** (formulaires, listes, vues de détail).
- Persistance des données avec **PostgreSQL**, configuration possible via Docker.
- Validation des formulaires avec Spring Validation.
- Politique de mot de passe renforcée (majuscule, minuscule, chiffre, caractère spécial).

### Fonctionnalités prévues

- [x] API REST initiale pour statistiques et données utilisateur (endpoints `/api/**`).
- [ ] Système de confirmation par email après réservation (Spring Mail + template Thymeleaf).
- [ ] Calendrier visuel des réservations (FullCalendar ou équivalent) pour vue mensuelle / hebdo.
- [ ] Export des réservations en PDF (iText / Flying Saucer, etc.).
- [ ] Notifications push (Web Push API ou intégration mobile).
- [ ] Système d'évaluation des salles (notes, commentaires, moyenne par salle).

***

## Architecture du projet

### Organisation générale

Le projet suit une architecture en couches classique Spring Boot :

- **config** : configuration Spring (sécurité, CORS, mail, etc.).
- **controller** : contrôleurs MVC (endpoints web / REST).
- **service** : logique métier (réservations, salles, utilisateurs).
- **repository** : interfaces JPA pour l'accès aux données.
- **model/entity** : entités JPA (User, Room, Reservation, Rating, …).
- **dto** : objets de transfert pour les réponses JSON / formulaires.

### Modèle de données (exemple)

- **User** : id, nom, email, mot de passe, rôle, statut.
- **Room** : id, nom, capacité, équipements, description, disponibilité.
- **Reservation** : id, utilisateur, salle, date, créneau horaire, statut (en attente, confirmée, annulée).
- **Rating** (prévu) : id, salle, utilisateur, note, commentaire, date.

***

## Stack technique détaillée

- **Backend** :
    - Spring Boot (Java 17)
    - Spring MVC (contrôleurs REST / Thymeleaf)
    - Spring Data JPA (accès à PostgreSQL)
    - Spring Security (authentification / autorisation)
    - Spring Validation (validation des formulaires)

- **Frontend** :
    - Thymeleaf, HTML5, CSS.

- **Base de données** :
    - PostgreSQL (local ou via image Docker `postgres`).

- **Conteneurisation** :
    - Docker / Docker Compose pour lancer l'appli et la base de données.

- **Tests** :
    - JUnit, Spring Boot Test, Mockito pour la couche service / contrôleur.

***

## Installation et exécution

### Prérequis

- Java 17.
- Maven.
- Docker & Docker Compose (optionnel mais conseillé).

### Lancer avec Docker (exemple)

1. Configurer `docker-compose.yml` avec un service `postgres` et l'application Spring Boot.
2. Lancer :
   ```bash
   docker-compose up --build
   ```  
3. Accéder à l'application sur `http://localhost:8080`.

### Lancer en local sans Docker

1. Cloner le projet :
   ```bash
   git clone https://github.com/UBONGO2000/BookingRoom.git
   cd BookingRoom
   ```  
2. Créer une base PostgreSQL :
   - Nom : `bookingroom`
   - Utilisateur / mot de passe à reporter dans `application.properties`
3. Lancer :
   ```bash
   mvn spring-boot:run
   # ou
   ./mvnw spring-boot:run
   ```  
4. Ouvrir `http://localhost:8080`.

***

## Comptes par défaut

Les comptes suivants sont créés automatiquement au premier lancement :

| Rôle | Nom d'utilisateur | Mot de passe |
|------|-------------------|--------------|
| Utilisateur | `user` | `User@123` |
| Administrateur | `admin` | `Admin@123` |

**Politique de mot de passe** (inscription) :
- Minimum 8 caractères
- Au moins 1 lettre majuscule
- Au moins 1 lettre minuscule
- Au moins 1 chiffre
- Au moins 1 caractère spécial (@$!%*?&)

***

## Routes principales

- `GET /login` : page de connexion.
- `POST /login` : traitement de la connexion.
- `GET /logout` : déconnexion.
- `GET /register` : page d'inscription.
- `POST /register` : traitement de l'inscription.
- `GET /dashboard` : tableau de bord utilisateur.
- `GET /booking` : liste des salles + recherche.
- `GET /booking/{id}` : détail d'une salle.
- `POST /booking/confirm` : confirmer une réservation.
- `POST /booking/cancel/{id}` : annuler une réservation.
- `GET /admin` : dashboard administrateur.
- `GET /admin/rooms/new` : formulaire création salle.
- `POST /admin/rooms/save` : sauvegarder salle.
- `GET /admin/rooms/edit/{id}` : formulaire modification salle.
- `GET /admin/rooms/delete/{id}` : supprimer salle.

### Documentation API REST

Les endpoints API sont sécurisés. L'authentification par session est utilisée, mais le CSRF est désactivé pour les routes `/api/**` afin de faciliter l'intégration mobile.

- `POST /api/auth/login` : Authentification (JSON: `username`, `password`). Retourne un message de succès et le nom d'utilisateur.
- `GET /api/rooms` : Liste des salles. Paramètres optionnels : `name`, `capacity`, `projector`, `whiteboard`, `videoconferencing`, `page`, `size`.
- `GET /api/rooms/{id}` : Détail d'une salle avec placeholder pour les évaluations.
- `GET /api/reservations/me` (ou `/api/my-bookings`) : Liste des réservations de l'utilisateur connecté.
- `POST /api/reservations` : Créer une réservation. JSON: `title`, `startTime`, `endTime`, `roomId`. Les dates doivent être au format ISO (ex: `2026-04-18T14:00:00`).
- `GET /api/stats/overview` : Vue d'ensemble des statistiques de l'application.
- `GET /api/rooms/available-now` : Salles disponibles immédiatement pour 1h.
- `GET /api/me` : Informations du profil connecté.

***

## Sécurité

### Mesures implémentées

- **Protection CSRF** : Réactivée pour sécuriser les formulaires contre les attaques de type Cross-Site Request Forgery.
- **Gestion Globale des Erreurs** : Utilisation de `@ControllerAdvice` pour capturer les exceptions et afficher des pages d'erreur personnalisées.
- **Chiffrement des mots de passe** : BCrypt avec salt automatique.
- **Gestion des rôles** : USER et ADMIN avec séparation stricte des routes.
- **Validation des formulaires** : Spring Validation sur User, Room et Booking.
- **Politique de mot de passe** : complexité minimale requise.
- **Logout sécurisé** : invalidation de session et suppression des cookies.

### Recommandations pour la production

- Activer HTTPS.
- Implémenter une protection contre les attaques Brute Force (rate limiting).
- Configurer des headers de sécurité supplémentaires (HSTS, CSP, etc.).
- Utiliser une base de données PostgreSQL en production (pas H2).

***

## Backlog technique (idées d'implémentation)

- **Emails** : configuration de Spring Mail + templates Thymeleaf pour confirmation / annulation.
- **Calendrier** : intégration de FullCalendar (JS) sur une page Thymeleaf consommant un endpoint JSON `/api/calendar`.
- **PDF** : service dédié générant un récapitulatif de réservation et endpoint `/reservations/{id}/pdf`.
- **Push** : enregistrement des tokens (Web Push ou mobile) côté backend et envoi lors des changements de statut.
- **Notes / avis** : formulaire d'évaluation sur la page de détail d'une salle, affichage de la note moyenne.
