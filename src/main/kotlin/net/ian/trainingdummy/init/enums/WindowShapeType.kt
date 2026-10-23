package net.ian.trainingdummy.init.enums

/**
 * Formatos geométricos disponíveis para a janela flutuante do Dummy.
 *
 * Este enum é "puro" (sem referências ao WorldPanel) para poder ser usado com segurança no código comum
 * (DummyEntity / rede). A conversão para [dev.ian.worldpanel.shape.PanelShape] fica no cliente
 * em `WindowShapes`.
 */
enum class WindowShapeType(val translationKey: String) {
    ROUNDED("gui.trainingdummy.window_shape.rounded"),
    ROUNDED_RECTANGLE("gui.trainingdummy.window_shape.rounded_rectangle"),
    RECTANGLE("gui.trainingdummy.window_shape.rectangle"),
    CIRCLE("gui.trainingdummy.window_shape.circle"),
    ELLIPSE("gui.trainingdummy.window_shape.ellipse"),
    TRIANGLE("gui.trainingdummy.window_shape.triangle"),
    RHOMBUS("gui.trainingdummy.window_shape.rhombus"),
    TRAPEZOID("gui.trainingdummy.window_shape.trapezoid"),
    PARALLELOGRAM("gui.trainingdummy.window_shape.parallelogram"),
    PENTAGON("gui.trainingdummy.window_shape.pentagon"),
    HEXAGON("gui.trainingdummy.window_shape.hexagon"),
    OCTAGON("gui.trainingdummy.window_shape.octagon");

    fun next(): WindowShapeType = entries[(ordinal + 1) % entries.size]

    fun previous(): WindowShapeType = entries[(ordinal - 1 + entries.size) % entries.size]

    companion object {
        val DEFAULT = ROUNDED

        fun byId(id: Int): WindowShapeType = entries.getOrElse(id) { DEFAULT }
    }
}
