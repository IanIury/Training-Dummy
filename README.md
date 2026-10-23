# Training Dummy (NeoForge 26.2)

**Training Dummy** é um mod que adiciona um boneco de treino interativo para testar armas, encantamentos, poções, armaduras e analisar o seu dano em tempo real com painéis flutuantes.

> **Nota sobre o projeto:** Este repositório é uma recriação e adaptação completa do mod original (feito para a 1.21.1) para funcionar na arquitetura do **Minecraft 26.2 (NeoForge)**, contando com **(AI-Assisted Development)** durante o processo de migração, refatoração e adaptação para as novas APIs de renderização.

---

## Principais Recursos e Como Usar

### 1. Boneco de Treino (`Training Dummy`)
- **Posicionar:** Use o item **Training Dummy** no chão para invocá-lo.
- **Equipar:** Clique com o botão direito diretamente no corpo ou nos braços do boneco segurando armaduras, cabeças ou itens, ou clique na **base** dele para abrir o **inventário completo**.
- **Recolher:** Segure `Shift + Clique Direito` na base do boneco (ou com o item de invocação na mão) para guardá-lo no inventário mantendo todos os equipamentos, configurações e nome.
- **Skins e Efeitos Especiais:** Renomeie o boneco com uma **Etiqueta (Name Tag)** usando o nickname de um jogador para aplicar o rosto da skin dele ao boneco. Nomes especiais também ativam efeitos visuais:
  - `jeb_` — Efeito arco-íris cintilante.
  - `-endGateway-`, `-entityGlint-`, `-translucent-`, `-lightning-` — Shaders especiais no corpo do boneco.

### 2. Módulo de Treino (`Training Module`) e Modos de Análise
Ao equipar um **Módulo de Treino** no slot especial do boneco (ou clicando com o módulo diretamente nele), você libera os modos avançados na GUI do boneco:
- **Padrão (`Default`):** Mostra o dano causado por golpe e as reduções aplicadas (armadura, encantamentos, efeitos de poção, absorção, etc.).
- **DPS:** Mede o seu Dano Por Segundo durante uma janela de 5 segundos, exibindo média por golpe, maior acerto e registrando recordes no **Ranking** do mundo.
- **Acumulado (`Accumulated`):** Soma continuamente todo o dano causado nos golpes.
- **Comparação (`Comparison`):** Compara lado a lado o golpe anterior com o golpe atual, indicando a diferença e qual arma teve o melhor desempenho.

Na tela do boneco (clicando na base), você também pode:
- Alternar o **formato geométrico da janela flutuante** (`Rectangle`, `Rounded`, `Hexagon`, `Octagon`, etc.).
- Ativar/desativar as **informações detalhadas de dano**.
- Abrir a tela de **Ranking** global do servidor/mundo.

### 3. Display do Boneco (`Display Dummy`)
Permite espelhar o painel de informações de um boneco à distância:
1. Com o **Display Dummy** na mão, use `Shift + Clique Direito` em um Boneco de Treino para vinculá-lo (uma miniatura holográfica aparecerá na sua mão e o boneco ficará destacado).
2. Posicione o display em qualquer bloco (chão, parede ou teto) para projetar a janela daquele boneco remotamente.

### 4. Mesa de Gerenciamento (`Dummy Management Block`)
Bloco utilitário para gerenciar itens e configurações relacionadas aos bonecos de treino em um só lugar.

---

## Requisitos
- **Minecraft:** `26.2`
- **NeoForge:** `26.2.0.93+`
- **Java:** `25`
- **Dependência incluída:** `WorldPanel` (renderização das janelas flutuantes 3D)
