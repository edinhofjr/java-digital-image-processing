package dev.edinho.pdi.entities;

/**
 * Características de uma região (componente conexo) de uma imagem binária,
 * organizadas como na taxonomia da aula:
 * <ul>
 *   <li><b>Dimensionais</b>: área, perímetro, excentricidade, compacidade, raio máximo e mínimo;</li>
 *   <li><b>Inerciais</b>: centro geométrico, momentos, orientação, retângulo envolvente, elipse ajustada;</li>
 *   <li><b>Topológicas</b>: número de furos, número de Euler.</li>
 * </ul>
 *
 * @param label     rótulo do componente (1..n)
 * @param area      número de pixels (momento m00)
 * @param perimeter número de pixels de contorno
 * @param centroidX centro geométrico X (m10 / m00)
 * @param centroidY centro geométrico Y (m01 / m00)
 * @param minX      limite esquerdo do retângulo envolvente
 * @param minY      limite superior do retângulo envolvente
 * @param maxX      limite direito do retângulo envolvente
 * @param maxY      limite inferior do retângulo envolvente
 * @param mu20      momento central de 2ª ordem em X, normalizado pela área
 * @param mu02      momento central de 2ª ordem em Y, normalizado pela área
 * @param mu11      momento central misto, normalizado pela área
 * @param maxRadius maior distância do centro ao contorno
 * @param minRadius menor distância do centro ao contorno
 * @param holes     número de furos (componentes de fundo cercados pelo objeto)
 */
public record RegionFeatures(
        int label,
        int area,
        int perimeter,
        double centroidX,
        double centroidY,
        int minX,
        int minY,
        int maxX,
        int maxY,
        double mu20,
        double mu02,
        double mu11,
        double maxRadius,
        double minRadius,
        int holes
) {
    // ---------- retângulo envolvente ----------

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    /** Razão entre a área do objeto e a área do retângulo envolvente. */
    public double extent() {
        return (double) area / (width() * height());
    }

    /** Razão largura / altura do retângulo envolvente. */
    public double aspectRatio() {
        return (double) width() / height();
    }

    // ---------- dimensionais ----------

    /** Compacidade P² / A — mínima (4π ≈ 12,57) para o círculo, cresce para formas irregulares. */
    public double compactness() {
        return (double) perimeter * perimeter / area;
    }

    /** Circularidade 4πA / P² — 1 para um círculo, menor para formas alongadas/irregulares. */
    public double circularity() {
        if (perimeter == 0) return 0;
        return Math.min(1.0, 4 * Math.PI * area / ((double) perimeter * perimeter));
    }

    /** Raio de um círculo com a mesma área. */
    public double equivalentRadius() {
        return Math.sqrt(area / Math.PI);
    }

    // ---------- inerciais (elipse ajustada pelos momentos de 2ª ordem) ----------

    private double lambda(int sign) {
        double mean = (mu20 + mu02) / 2;
        double diff = Math.sqrt(Math.pow((mu20 - mu02) / 2, 2) + mu11 * mu11);
        return Math.max(0, mean + sign * diff);
    }

    /** Comprimento do eixo maior da elipse com os mesmos momentos de 2ª ordem. */
    public double majorAxis() {
        return 4 * Math.sqrt(lambda(+1));
    }

    /** Comprimento do eixo menor da elipse com os mesmos momentos de 2ª ordem. */
    public double minorAxis() {
        return 4 * Math.sqrt(lambda(-1));
    }

    /** Excentricidade da elipse ajustada: 0 = círculo, tende a 1 para formas alongadas. */
    public double eccentricity() {
        double l1 = lambda(+1);
        if (l1 == 0) return 0;
        return Math.sqrt(1 - lambda(-1) / l1);
    }

    /**
     * Orientação do eixo maior em graus, medida a partir do eixo X no sentido anti-horário
     * (como aparece na tela, já que o Y da imagem cresce para baixo). Faixa (-90, 90].
     */
    public double orientationDegrees() {
        double theta = 0.5 * Math.atan2(2 * mu11, mu20 - mu02);
        return -Math.toDegrees(theta);
    }

    // ---------- topológicas ----------

    /** Número de Euler de um único componente: 1 − número de furos. */
    public int eulerNumber() {
        return 1 - holes;
    }
}
