# BookingRoom

Application Spring Boot pour la **réservation de salles** avec authentification sécurisée, espace administrateur, gestion des réservations et recherche avancée. Stack : Java 17, Spring Security, Thymeleaf, PostgreSQL, Docker et tests automatisés.

***

## Fonctionnalités

- Authentification et gestion des utilisateurs avec rôles (utilisateur / administrateur).
- Gestion des salles : création, modification, suppression, capacité et équipements.
- Gestion des réservations : création, consultation, annulation par l’utilisateur et validation par l’admin.
- Recherche avancée des salles par date, créneau horaire, capacité et caractéristiques.
- Interface web côté serveur rendue avec **Thymeleaf** (formulaires, listes, vues de détail).
- Persistance des données avec **PostgreSQL**, configuration possible via Docker.

### Fonctionnalités prévues

- [ ] Système de confirmation par email après réservation (Spring Mail + template Thymeleaf).
- [ ] Calendrier visuel des réservations (FullCalendar ou équivalent) pour vue mensuelle / hebdo.
- [ ] API REST pour application mobile (endpoints sécurisés pour login, listes de salles et réservations).
- [ ] Export des réservations en PDF (iText / Flying Saucer, etc.).
- [ ] Notifications push (Web Push API ou intégration mobile).
- [ ] Système d’évaluation des salles (notes, commentaires, moyenne par salle).

***

## Architecture du projet

### Organisation générale

Le projet suit une architecture en couches classique Spring Boot :

- **config** : configuration Spring (sécurité, CORS, mail, etc.).
- **controller** : contrôleurs MVC (endpoints web / REST).
- **service** : logique métier (réservations, salles, utilisateurs).
- **repository** : interfaces JPA pour l’accès aux données.
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

- **Frontend** :
    - Thymeleaf, HTML5, CSS.

- **Base de données** :
    - PostgreSQL (local ou via image Docker `postgres`).

- **Conteneurisation** :
    - Docker / Docker Compose pour lancer l’appli et la base de données.

- **Tests** :
    - JUnit, Spring Boot Test, Mockito pour la couche service / contrôleur.

***

## Installation et exécution

### Prérequis

- Java 17.
- Maven.
- Docker & Docker Compose (optionnel mais conseillé).

### Lancer avec Docker (exemple)

1. Configurer `docker-compose.yml` avec un service `postgres` et l’application Spring Boot.
2. Lancer :
   ```bash
   docker-compose up --build
   ```  
3. Accéder à l’application sur `http://localhost:8080`.

### Lancer en local sans Docker

1. Cloner le projet :
   ```bash
   git clone https://github.com/UBONGO2000/BookingRoom.git
   cd BookingRoom
   ```  
2. Créer une base PostgreSQL :
    - Nom : `bookingroom`
    - Utilisateur / mot de passe à reporter dans `application.properties` :
        - username : 'user'
        - password : 'user123'
    - Administrateur :
      - username : 'admin'
      - password :'admin123'
3. Lancer :
   ```bash
   mvn spring-boot:run
   # ou
   ./gradlew bootRun
   ```  
4. Ouvrir `http://localhost:8080`.

***

## Routes principales (exemples)
- `GET /login` : page de connexion.
- `GET /rooms` : liste des salles + recherche.
- `GET /reservations` : liste des réservations de l’utilisateur connecté.
- `POST /reservations` : création d’une nouvelle réservation.
- `GET /admin/rooms` : gestion des salles (admin).
- `GET /admin/reservations` : gestion et validation des réservations (admin).

### Endpoints REST (prévu pour l’API mobile)

- `POST /api/auth/login` : authentification et récupération d’un token.
- `GET /api/rooms` : liste paginée / filtrée des salles.
- `GET /api/rooms/{id}` : détail d’une salle avec évaluations.
- `GET /api/reservations/me` : réservations de l’utilisateur connecté.
- `POST /api/reservations` : création d’une réservation mobile.

***

## Backlog technique (idées d’implémentation)

- **Emails** : configuration de Spring Mail + templates Thymeleaf pour confirmation / annulation.
- **Calendrier** : intégration de FullCalendar (JS) sur une page Thymeleaf consommant un endpoint JSON `/api/calendar`.
- **PDF** : service dédié générant un récapitulatif de réservation et endpoint `/reservations/{id}/pdf`.
- **Push** : enregistrement des tokens (Web Push ou mobile) côté backend et envoi lors des changements de statut.
- **Notes / avis** : formulaire d’évaluation sur la page de détail d’une salle, affichage de la note moyenne.