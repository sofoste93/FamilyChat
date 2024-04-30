# FamilyChat Version 1.0

FamilyChat est une application de chat simple en mode console utilisant Java pour permettre la communication en temps réel au sein d'un réseau domestique.

## Structure du Projet

Le projet est divisé en deux parties principales : `client` et `server`.

### Server

Le serveur gère toutes les connexions entrantes, traite les messages reçus et les redistribue à tous les clients connectés.

- **Fichiers principaux:**
  - `ChatServer.java`: Lance le serveur et accepte les connexions des clients.
  - `UserThread.java`: Gère la communication avec les clients connectés.

### Client

Le client se connecte au serveur et permet à l'utilisateur d'envoyer et de recevoir des messages.

- **Fichiers principaux:**
  - `ChatClient.java`: Initialise la connexion au serveur et lance les threads de lecture et d'écriture.
  - `ReadThread.java`: Lit les messages du serveur.
  - `WriteThread.java`: Envoie les messages au serveur et gère l'entrée utilisateur.

## Fonctionnalités

- Connexion multiple de clients au serveur de chat.
- Envoi et réception de messages en temps réel.
- Affichage des utilisateurs connectés.
- Gestion simple des utilisateurs se connectant et se déconnectant.

## Comment utiliser

### Serveur

1. Naviguez dans le dossier du projet.
2. Compilez et exécutez `ChatServer.java` en utilisant la commande:
   ```bash
   java tech.sofoste.server.ChatServer
