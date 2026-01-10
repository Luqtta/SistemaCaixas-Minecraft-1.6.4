# 🧰 SistemaCaixas (Avelar)

Sistema de **Caixas / Crates avançado** para **Minecraft 1.6.4 (Bukkit / Cauldron / CraftBukkit modded)**, totalmente configurável via **GUI**, **chat** e **config.yml**, com suporte a **itens de mods (Crafting Dead)** e **economia (Vault / iConomy)**.

---

## 📦 Sobre o projeto

O **SistemaCaixas** é um plugin desenvolvido para servidores **Minecraft 1.6.4**, especialmente **modpacks como Crafting Dead**, que permite criar caixas personalizadas com prêmios aleatórios, roleta animada, sistema de compra integrado à economia e ferramentas administrativas completas.

O foco do projeto é:
- 🔥 Estabilidade
- 🎯 Facilidade de uso
- 🧠 Controle total via GUI
- 🧩 Compatibilidade com itens de mods

---

## ✨ Funcionalidades

### 🎁 Sistema de Caixas
- Criação ilimitada de caixas
- Cada caixa possui:
  - Nome customizado
  - Descrição personalizada
  - Ícone configurável
  - Preço individual
  - Lista de prêmios com **chance manual**
- Caixa comprada vira **item físico** no inventário do jogador

---

### 🎰 Roleta Animada
- Roleta estilo **linha do meio girando**
- Item final aparece no **centro**
- **Nether Star fixa** em cima e embaixo do item ganho
- Animação suave com sons progressivos
- Proteção contra abrir várias caixas ao mesmo tempo

---

### 🎆 Itens Raros
- Itens com **chance abaixo de 25%** são considerados raros
- Ao ganhar item raro:
  - 🎇 Fogos de artifício
  - 📢 Broadcast automático no servidor
  - 🔊 Som especial

---

### 💰 Economia
- Integração com **Vault**
- Compatível com **iConomy**
- Compra de caixas via menu
- Lore do menu mostra:
  - Preço da caixa
  - Saldo atual do jogador
  - Status em tempo real (verde/vermelho)
- Lore atualiza automaticamente após cada compra

---

### 🧑‍💼 Sistema Administrativo
Apenas para **OPs ou jogadores com permissão `caixas.admin`**

- Criar caixas
- Editar caixas
- Deletar caixas (com limite mínimo)
- Dar caixas para jogadores
- Recarregar configurações sem reiniciar o servidor

---

### 🛠️ Editor de Itens via GUI
- GUI exclusiva para editar prêmios da caixa
- Ações:
  - **Clique direito** → editar chance pelo chat
  - **Clique esquerdo** → remover item (com confirmação no chat)
- Adicionar item:
  - Colocar item no cursor
  - Clicar em slot vazio
  - Definir chance pelo chat
- Suporte total a **itens vanilla e de mods**
- Sistema seguro usando **Reward ID (rid)** para evitar bugs

---

### 💬 Sistema de Chat Inteligente
- Definição de chance pelo chat
- Confirmação de remoção (SIM / CANCELAR)
- Mensagens padronizadas com prefixo:

