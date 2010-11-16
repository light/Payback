package payback.fft;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import payback.fft.FFT.ComplexRGB;

/**
 * Let's test the {@link FFT} with a visually understandable example.
 */
public class FFTTest {

    public static void main(String[] args) throws Exception {
        File baseDir = new File("src", FFT.class.getPackage().getName().replace('.', File.separatorChar));
        System.out.println("dir: " + baseDir.getAbsolutePath());

        String imageFile = "mega_256.png";
        // String imageFile = "txt2.gif";
        String maskFile = "mm_256.png";
        // String maskFile = "txt2msk1.gif";

        // playWithMasks(baseDir, imageFile, maskFile);

        playWithColors(baseDir, imageFile, maskFile);

    }

    private static void playWithColors(File baseDir, String imageFile, String maskFile) throws Exception {
        final BufferedImage image = ImageIO.read(new File(baseDir, imageFile));
        int w = image.getWidth();
        int h = image.getHeight();
        int imageType = image.getType();
        System.out.println("size: " + w + ", " + h);

        ComplexRGB blueComplexRGB = new ComplexRGB() {
            @Override
            public ComplexNumber getComplexNumber(int rgb) {
                return super.getComplexNumber(rgb & 0xff);
            }
        };
        ComplexRGB greenComplexRGB = new ComplexRGB() {
            @Override
            public ComplexNumber getComplexNumber(int rgb) {
                return super.getComplexNumber((rgb & 0xff00) >> 8);
            }
        };
        ComplexRGB redComplexRGB = new ComplexRGB() {
            @Override
            public ComplexNumber getComplexNumber(int rgb) {
                return super.getComplexNumber((rgb & 0xff0000) >> 16);
            }
        };

        final BufferedImage mask = ImageIO.read(new File(baseDir, maskFile));
        final BufferedImage allConvols = new BufferedImage(w, h, imageType);

        FFT blueFFT = newColoredFFT(image, blueComplexRGB);
        FFT blueMaskFFT = newColoredFFT(mask, blueComplexRGB);
        FFT blueConvolveFFT = new FFT(FFT.convolve(blueFFT.output, blueMaskFFT.output));

        FFT redFFT = newColoredFFT(image, redComplexRGB);
        FFT redMaskFFT = newColoredFFT(mask, redComplexRGB);
        FFT redConvolveFFT = new FFT(FFT.convolve(redFFT.output, redMaskFFT.output));

        FFT greenFFT = newColoredFFT(image, greenComplexRGB);
        FFT greenMaskFFT = newColoredFFT(mask, greenComplexRGB);
        FFT greenConvolveFFT = new FFT(FFT.convolve(greenFFT.output, greenMaskFFT.output));

        final BufferedImage blueConvol = new BufferedImage(w, h, imageType);
        displayFFT(blueConvolveFFT, blueConvol, false, null);
        final BufferedImage redConvol = new BufferedImage(w, h, imageType);
        displayFFT(redConvolveFFT, redConvol, false, null);
        final BufferedImage greenConvol = new BufferedImage(w, h, imageType);
        displayFFT(greenConvolveFFT, greenConvol, false, null);

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                allConvols.setRGB(i, j, blueConvol.getRGB(i, j) & redConvol.getRGB(i, j) & greenConvol.getRGB(i, j));
            }
        }

        JFrame frame = new JFrame("test FFT");
        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                g.drawImage(image, 0, 0, null);
                g.drawImage(mask, 0, 300, null);
                g.drawImage(allConvols, 0, 600, null);

                g.drawImage(redConvol, 300, 000, null);
                g.drawImage(greenConvol, 300, 300, null);
                g.drawImage(blueConvol, 300, 600, null);
            }
        });
        frame.setPreferredSize(new Dimension(600, 900));
        frame.setVisible(true);
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private static FFT newColoredFFT(final BufferedImage image, ComplexRGB complexRGB) {
        FFT coloredFFT = new FFT();
        coloredFFT.complexRGB = complexRGB;
        coloredFFT.setInput(image);
        coloredFFT.compute(false);
        return coloredFFT;
    }

    private static void playWithMasks(File baseDir, String imageFile, String maskFile) throws Exception {
        final BufferedImage image = ImageIO.read(new File(baseDir, imageFile));
        int w = image.getWidth();
        int h = image.getHeight();
        System.out.println("size: " + w + ", " + h);

        FFT fft = new FFT(image);

        final BufferedImage fftImg = new BufferedImage(w, h, image.getType());
        final BufferedImage imgCentered = new BufferedImage(w, h, image.getType());

        displayFFT(fft, fftImg, true, null);

        imgCentered.getGraphics().drawImage(fftImg, 0, 0, w / 2, h / 2, w / 2, h / 2, w, h, null);
        imgCentered.getGraphics().drawImage(fftImg, w / 2, h / 2, w, h, 0, 0, w / 2, h / 2, null);
        imgCentered.getGraphics().drawImage(fftImg, w / 2, 0, w, h / 2, 0, h / 2, w / 2, h, null);
        imgCentered.getGraphics().drawImage(fftImg, 0, h / 2, w / 2, h, w / 2, 0, w, h / 2, null);

        final BufferedImage rebuiltImage = new BufferedImage(w, h, image.getType());
        final BufferedImage convol = new BufferedImage(w, h, image.getType());
        final BufferedImage mask = ImageIO.read(new File(baseDir, maskFile));

        FFT ifft = new FFT(fft.output);
        displayFFT(ifft, rebuiltImage, false, null);

        FFT convolveFFT = new FFT(FFT.convolve(fft.output, new FFT(mask).output));
        displayFFT(convolveFFT, convol, false, .8f);

        JFrame frame = new JFrame("test FFT");
        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                g.drawImage(image, 0, 0, null);
                g.drawImage(rebuiltImage, 0, 300, null);
                g.drawImage(imgCentered, 300, 300, null);
                g.drawImage(fftImg, 300, 0, null);
                g.drawImage(mask, 0, 600, null);
                g.drawImage(convol, 300, 600, null);
            }
        });
        frame.setPreferredSize(new Dimension(600, 900));
        frame.setVisible(true);
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private static void displayFFT(FFT fft, final BufferedImage img, boolean log, Float threshold) {
        int w = img.getWidth();
        int h = img.getHeight();
        double maxValue = 0.;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                ComplexNumber c = fft.output[i][j];
                if (c == null) {
                    continue;
                }
                maxValue = Math.max(maxValue, fft.complexRGB.getIntensity(c));
            }
        }
        // System.out.println("Max=" + maxValue);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                ComplexNumber c = fft.output[i][j];
                if (c == null) {
                    // System.out.println("oops! @" + i + "," + j);
                    c = new ComplexNumber();
                } else {
                    // System.out.println("@" + i + "," + j + " : c=" + c);
                }
                double value = fft.complexRGB.getIntensity(c);
                double intensity = log ? Math.log(value) / Math.log(maxValue) : value / maxValue;
                if (threshold != null && value < threshold * maxValue) {
                    intensity = 0.;
                }
                int rgb = Color.HSBtoRGB(0f, 0f, (float) intensity);
                img.setRGB(i, j, rgb);
            }
        }
    }
}
