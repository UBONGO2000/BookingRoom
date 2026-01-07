# Guide : Dockerisation et Déploiement Gratuit de RoomBooker

Ce guide explique comment transformer votre application Spring Boot en conteneur Docker et comment la mettre en ligne gratuitement.

---

## 1. Dockerisation du projet (Étape par étape)

### Étape 1 : Créer le fichier `Dockerfile`
A la racine de votre projet, créez un fichier nommé `Dockerfile` (sans extension) :

```dockerfile
# Étape 1 : Construction (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Copier le fichier pom.xml et télécharger les dépendances (cache)
COPY pom.xml .
RUN mvn dependency:go-offline
# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Runtime)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copier le jar généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar
# Exposer le port de l'application
EXPOSE 8080
# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Étape 2 : Créer le fichier `.dockerignore`
Pour éviter de copier des fichiers inutiles dans l'image Docker, créez un fichier `.dockerignore` :

```text
target/
mvnw
mvnw.cmd
.mvn/
.git/
.idea/
*.iml
Dockerfile
```

### Étape 3 : Construire et tester l'image localement
Ouvrez un terminal à la racine du projet et lancez :

1. **Construction de l'image** :
   ```bash
   docker build -t roombooker:1.0 .
   ```

2. **Lancement du conteneur** :
   ```bash
   docker run -p 8080:8080 roombooker:1.0
   ```
Accédez à `http://localhost:8080` pour vérifier que tout fonctionne.

---

## 2. Déploiement Gratuit (Option Render.com)

Render est l'une des plateformes les plus simples pour déployer gratuitement une application Spring Boot dockerisée.

### Étape 1 : Préparer votre dépôt GitHub
1. Créez un nouveau dépôt sur GitHub.
2. Envoyez votre code (avec le `Dockerfile`) :
   ```bash
   git init
   git add .
   git commit -m "Add Dockerfile for deployment"
   git branch -M main
   git remote add origin https://github.com/VOTRE_USER/VOTRE_PROJET.git
   git push -u origin main
   ```

### Étape 2 : Configurer Render
1. Créez un compte sur [Render.com](https://render.com/).
2. Cliquez sur **"New +"** puis **"Web Service"**.
3. Connectez votre compte GitHub et sélectionnez votre dépôt.
4. Dans la configuration :
   - **Name** : `roombooker-app`
   - **Region** : `Frankfurt` (plus proche de la France)
   - **Branch** : `main`
   - **Runtime** : `Docker` (Render détectera automatiquement votre Dockerfile)
   - **Instance Type** : `Free`
5. Cliquez sur **"Create Web Service"**.

### Étape 3 : Gérer la base de données
Puisque vous utilisez **H2 en mémoire** (`jdbc:h2:mem:bookingdb`), vos données seront effacées à chaque redémarrage du service (comportement normal du mode gratuit). 
Pour un déploiement réel avec persistance, vous pourriez créer une base de données **PostgreSQL gratuite** sur Render et mettre à jour votre `application.properties` avec les variables d'environnement.

---

## 3. Astuces pour le mode gratuit

1. **Mise en veille** : Sur Render (Plan Free), l'application s'endort après 15 minutes d'inactivité. Le premier chargement après une pause peut prendre 30 à 60 secondes.
2. **Variables d'environnement** : Ne mettez jamais vos mots de passe en clair dans le code. Utilisez l'onglet "Environment" sur Render pour définir vos secrets.
3. **Logs** : Vous pouvez suivre le déploiement et les erreurs en direct via l'onglet "Logs" de votre tableau de bord Render.

Félicitations ! Votre application est maintenant prête pour le monde entier. 🚀
