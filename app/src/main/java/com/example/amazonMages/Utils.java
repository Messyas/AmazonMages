package com.example.amazonMages;

/**
 * A classe Utils fornece métodos utilitários para o cálculo de distâncias entre pontos em um espaço 2D.
 */
public class Utils {

    /**
     * O método `getDistanceBetweenPoints` retorna a distância entre dois pontos 2D, p1 e p2.
     *
     * @param p1x A coordenada X do primeiro ponto (p1).
     * @param p1y A coordenada Y do primeiro ponto (p1).
     * @param p2x A coordenada X do segundo ponto (p2).
     * @param p2y A coordenada Y do segundo ponto (p2).
     * @return A distância entre os dois pontos calculada com a fórmula da distância Euclidiana.
     */
    public static double getDistanceBetweenPoints(double p1x, double p1y, double p2x, double p2y) {
        // Calcula a diferença entre as coordenadas X e Y dos dois pontos
        double deltaX = p1x - p2x;
        double deltaY = p1y - p2y;

        // Aplica o Teorema de Pitágoras para encontrar a distância entre os pontos
        // distância = raiz quadrada de ((deltaX)^2 + (deltaY)^2)
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }
}
