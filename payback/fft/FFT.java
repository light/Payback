package payback.fft;

import java.awt.Color;
import java.awt.image.BufferedImage;

import payback.algo.FlowAlgorithm;

public class FFT {

    /**
     * Data structure to hold the input to the algorithm.
     */
    public ComplexNumber[][] input;

    /**
     * Data structure to hold the ouput of the algorithm.
     */
    public ComplexNumber[][] output;

    public static class ComplexRGB {
        public ComplexNumber getComplexNumber(int rgb) {
            return new ComplexNumber(FlowAlgorithm.getBrightness(rgb), 0.);
        }

        public int getRGB(ComplexNumber c) {
            return Color.HSBtoRGB(0f, 0f, (float) getIntensity(c));
        }

        public double getIntensity(ComplexNumber c) {
            return c.getMagnitude();
        }
    }

    public ComplexRGB complexRGB = new ComplexRGB();

    /**
     * Default no argument constructor.
     */
    public FFT() {
    }

    private ComplexNumber[][] build2DArray(int[][] pixels) {
        int w = pixels.length;
        int h = pixels[0].length;
        ComplexNumber[][] result = new ComplexNumber[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                result[i][j] = complexRGB.getComplexNumber(pixels[i][j]);
            }
        }
        return result;
    }

    private ComplexNumber[][] build2DArray(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        ComplexNumber[][] result = new ComplexNumber[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                result[i][j] = complexRGB.getComplexNumber(image.getRGB(i, j));
            }
        }
        return result;
    }

    private void setRow(ComplexNumber[][] matrix, int row, ComplexNumber[] data) {
        for (int i = 0; i < matrix[0].length; i++) {
            matrix[i][row] = data[i];
        }
    }

    private ComplexNumber[] getRow(ComplexNumber[][] matrix, int row) {
        ComplexNumber[] result = new ComplexNumber[matrix[0].length];
        for (int i = 0; i < result.length; i++) {
            result[i] = matrix[i][row];
        }
        return result;
    }

    /**
     * Inverse FFT.
     */
    public FFT(ComplexNumber[][] input) {
        this.input = input;
        compute(true);
    }

    /**
     * FFT of the given image (as RGB pixels).
     */
    public FFT(int[][] pixels) {
        input = build2DArray(pixels);

        compute(false);
    }

    public void setInput(ComplexNumber[][] input) {
        this.input = input;
    }

    public void setInput(BufferedImage image) {
        input = build2DArray(image);
    }

    /**
     * FFT of the given image.
     */
    public FFT(BufferedImage image) {
        input = build2DArray(image);

        compute(false);
    }

    private static int[][] getPixels(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] pixels = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                pixels[i][j] = image.getRGB(i, j);
            }
        }
        return pixels;
    }

    public void compute(boolean inverse) {
        long start = System.currentTimeMillis();

        int w = input.length;
        int h = input[0].length;
        ComplexNumber[][] intermediate = new ComplexNumber[w][h];

        output = new ComplexNumber[w][h];

        for (int i = 0; i < input.length; i++) {
            intermediate[i] = recursiveFFT(input[i], inverse);
        }
        for (int i = 0; i < intermediate[0].length; i++) {
            setRow(output, i, recursiveFFT(getRow(intermediate, i), inverse));
        }

        System.out.println((System.currentTimeMillis() - start) + " ms");
    }

    public static ComplexNumber[][] convolve(ComplexNumber[][] a, ComplexNumber[][] b) {
        ComplexNumber[][] result = new ComplexNumber[a.length][a[0].length];
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = a[i][j].mul(b[i][j]);
            }
        }
        return result;
    }

    private static ComplexNumber[] recursiveFFT(ComplexNumber[] x, boolean inverse) {
        int n = x.length;
        ComplexNumber[] result = new ComplexNumber[n];
        ComplexNumber[] even = new ComplexNumber[n / 2];
        ComplexNumber[] odd = new ComplexNumber[n / 2];

        if (n == 1) {
            result[0] = x[0];
        } else {

            even = recursiveFFT(evenValues(x), inverse);
            odd = recursiveFFT(oddValues(x), inverse);

            for (int k = 0; k < n / 2; k++) {
                ComplexNumber factor =
                        inverse ? ComplexNumber.fromPolar(1., 2. * Math.PI * k / n) : ComplexNumber.fromPolar(1., -2.
                                * Math.PI * k / n);
                result[k] = even[k].add(odd[k].mul(factor));
                result[k + n / 2] = even[k].sub(odd[k].mul(factor));
            }

        }
        return result;
    }

    private static ComplexNumber[] evenValues(ComplexNumber[] x) {
        ComplexNumber[] result = new ComplexNumber[x.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = x[2 * i];
        }
        return result;
    }

    private static ComplexNumber[] oddValues(ComplexNumber[] x) {
        ComplexNumber[] result = new ComplexNumber[x.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = x[2 * i + 1];
        }
        return result;
    }
}
