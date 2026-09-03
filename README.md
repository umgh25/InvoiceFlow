# InvoiceFlow

InvoiceFlow est un mini SaaS de facturation construit avec Spring Boot et PostgreSQL.

## Gestion des clients

Le backend expose un CRUD authentifié sous `/api/customers` :

- `POST /api/customers` crée un client pour l'utilisateur connecté ;
- `GET /api/customers` liste uniquement ses clients ;
- `GET /api/customers/{id}` retourne l'un de ses clients ;
- `PUT /api/customers/{id}` met à jour l'un de ses clients ;
- `DELETE /api/customers/{id}` supprime l'un de ses clients.

Toutes les routes nécessitent un JWT dans l'en-tête `Authorization: Bearer <token>`. Un client appartenant à un autre utilisateur est traité comme inexistant. Les erreurs utilisent le format standard `application/problem+json` : validation en `400`, ressource absente en `404`, conflit d'adresse e-mail en `409`.

L'adresse e-mail d'un client est normalisée en minuscules et doit être unique pour son propriétaire. Deux utilisateurs différents peuvent enregistrer la même adresse client.

## Démarrage local

Prérequis : Java 17 et Docker.

```powershell
docker compose up -d postgres
cd backend
.\mvnw.cmd spring-boot:run
```

L'API démarre sur `http://localhost:8080` avec les paramètres PostgreSQL définis dans `docker-compose.yml`.

## Tests

Docker doit être démarré. La suite utilise Testcontainers et lance automatiquement un PostgreSQL 16 éphémère :

```powershell
cd backend
.\mvnw.cmd verify
```

Les tests couvrent le contexte Spring, les migrations Flyway, le CRUD HTTP, l'authentification JWT, le cloisonnement entre utilisateurs, les erreurs et la validation.

## Migration des clients existants

La migration `V3__scope_customers_to_owner.sql` ajoute le propriétaire et remplace l'unicité globale de l'adresse e-mail par une unicité par utilisateur.

- S'il n'existe aucun client, la migration s'applique directement.
- S'il existe des clients et exactement un utilisateur, ils lui sont rattachés automatiquement.
- S'il existe des clients et plusieurs utilisateurs, la migration s'arrête sans supprimer de données afin que leur propriété soit résolue explicitement.
