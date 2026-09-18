package net.ian.trainingdummy.client.window

enum class WindowAlignment(val xRatio: Float, val yRatio: Float) {
    TOP_LEFT(0.0f, 0.0f),       // Cresce para a DIREITA e para BAIXO (Fixo no canto superior esquerdo)
    TOP_CENTER(0.5f, 0.0f),     // Cresce para os dois lados horizontalmente e para BAIXO
    TOP_RIGHT(1.0f, 0.0f),      // Cresce para a ESQUERDA e para BAIXO
    CENTER_LEFT(0.0f, 0.5f),    // Cresce para a DIREITA e para CIMA/BAIXO
    CENTER(0.5f, 0.5f),         // Centralizado (Comportamento antigo)
    CENTER_RIGHT(1.0f, 0.5f),   
    BOTTOM_LEFT(0.0f, 1.0f),    // Cresce para a DIREITA e para CIMA
    BOTTOM_CENTER(0.5f, 1.0f),  // Cresce para CIMA
    BOTTOM_RIGHT(1.0f, 1.0f)
}