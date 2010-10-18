package payback.opticalflow;

import java.awt.image.BufferedImage;

public class FlowAlgorithm {
	/** The previous and current frames. */
	private BufferedImage[] images = new BufferedImage[2];
	/** The frame's dimensions. */
	private int h, w;

	/** The pixel brightness values, for the previous and current frames. */
	private float[][][] e;
	/** Computed values for each pixel. */
	private float[][] u, v, ex, ey, et, vbar, ubar, norms;
	/** Labels. */
	private Label[][] labels;

	/** A magic (constant) value. */
	private float alpha = .1f;

	/** Number of loops in the velocity computation. */
	private final static int NB_ITERATIONS = 8;

	/**
	 * Compute a color's brightness value.
	 * 
	 * @param rgb
	 *            the color's RGB values as an integer (0xRRGGBB)
	 */
	private static float getBrightness(int rgb) {
		int red = (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = (rgb) & 0xff;
		float brightness = (float) ((.2126 * red + .7152 * green + .0722 * blue) / 255);
		return brightness;
	}

	/**
	 * Computes the scalar product of vectors at (i,j) and (k,l).
	 */
	private static float scalar(float[][] u, float[][] v, int i, int j, int k, int l) {
		if (k < 0) {
			k = 0;
		}
		if (l < 0) {
			l = 0;
		}
		if (k > u.length - 1) {
			k = u.length - 1;
		}
		if (l > u[0].length - 1) {
			l = u[0].length - 1;
		}
		return u[i][j] * u[k][l] + v[i][j] * v[k][l];
	}

	/**
	 * Update all computed values.
	 * 
	 * @param image
	 *            the current frame
	 */
	public void update(BufferedImage image) {
		images[0] = images[1];
		h = image.getHeight();
		w = image.getWidth();
		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		newImage.getGraphics().drawImage(image, 0, 0, null);
		images[1] = newImage;

		if (images[0] == null) {
			return;
		}

		if (u == null) {
			e = new float[2][w][h];
			ex = new float[w][h];
			ey = new float[w][h];
			et = new float[w][h];
			ubar = new float[w][h];
			vbar = new float[w][h];
			u = new float[w][h];
			v = new float[w][h];
			norms = new float[w][h];
		}

		// compute the E values
		e[0] = e[1];
		e[1] = new float[w][h];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int rgb = images[1].getRGB(x, y);
				// int r = (rgb & 0xFF0000) >> 16;
				// int g = (rgb & 0xFF00) >> 8;
				// int b = (rgb & 0xFF);
				// float[] hsbvals = Color.RGBtoHSB(r, g, b, null);
				// e[1][x][y] = hsbvals[2];
				e[1][x][y] = getBrightness(rgb);
			}
		}

		// compute the Ex, Ey, Et values
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				int x = i;
				int y = j;
				if (x == 0) {
					x = 1;
				}
				if (x == w - 1) {
					x = w - 2;
				}
				if (y == 0) {
					y = 1;
				}
				if (y == h - 1) {
					y = h - 2;
				}
				ex[x][y] = 0.25f * (e[0][x][(y + 1)] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][(x + 1)][y]
						+ e[1][x][(y + 1)] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][(x + 1)][y]);
				ey[x][y] = 0.25f * (e[0][(x + 1)][y] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][x][(y + 1)]
						+ e[1][(x + 1)][y] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][x][(y + 1)]);
				et[x][y] = 0.25f * (e[1][x][y] - e[0][x][y] + e[1][(x + 1)][y] - e[0][(x + 1)][y] + e[1][x][(y + 1)]
						- e[0][x][(y + 1)] + e[1][(x + 1)][(y + 1)] - e[0][(x + 1)][(y + 1)]);
			}
		}

		for (int iter = 0; iter < NB_ITERATIONS; iter++) {
			float[][] un = new float[w][h];
			float[][] vn = new float[w][h];

			// compute mean velocity values
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					int x = i;
					int y = j;
					if (x == 0) {
						x = 1;
					}
					if (x == w - 1) {
						x = w - 2;
					}
					if (y == 0) {
						y = 1;
					}
					if (y == h - 1) {
						y = h - 2;
					}
					ubar[x][y] = 1f / 6 * (u[x - 1][y] + u[x][y + 1] + u[x + 1][y] + u[x][y - 1]) + 1f / 12
							* (u[x - 1][y - 1] + u[x - 1][y + 1] + u[x + 1][y + 1] + u[x + 1][y - 1]);
					vbar[x][y] = 1f / 6 * (v[x - 1][y] + v[x][y + 1] + v[x + 1][y] + v[x][y - 1]) + 1f / 12
							* (v[x - 1][y - 1] + v[x - 1][y + 1] + v[x + 1][y + 1] + v[x + 1][y - 1]);
				}
			}

			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					float exx = ex[x][y];
					float ubarr = ubar[x][y];
					float eyy = ey[x][y];
					float vbarr = vbar[x][y];
					float ett = et[x][y];
					un[x][y] = ubarr - exx * (exx * ubarr + eyy * vbarr + ett)
							/ (alpha * alpha + exx * exx + eyy * eyy);
					vn[x][y] = vbarr - eyy * (exx * ubarr + eyy * vbarr + ett)
							/ (alpha * alpha + exx * exx + eyy * eyy);
				}
			}

			u = un;
			v = vn;
		}

		// compute norms
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				int x = i;
				int y = j;
				if (x == 0) {
					x = 1;
				}
				if (x == w - 1) {
					x = w - 2;
				}
				if (y == 0) {
					y = 1;
				}
				if (y == h - 1) {
					y = h - 2;
				}
				float uxy = u[x][y];
				float vxy = v[x][y];
				norms[x][y] = (float) Math.sqrt(uxy * uxy + vxy * vxy);
			}
		}

		// compute labels
		double threshold = .01; // how to guess it right ??
		labels = new Label[w][h]; // reset labels to null
		int nextLabel = 1;
		for (int i = 1; i < w - 1; i++) {
			for (int j = 1; j < h - 1; j++) {
				if (labels[i][j] == null)
					for (int p = -1; p < 2; p++) {
						for (int q = -1; q < 2; q++) {
							// float likeliness = Math
							// .abs(1 - scalar(u, v, i, j, i + p, j + q) /
							// scalar(u, v, i, j, i, j));
							float likeliness = Math.abs(u[i][j] * v[i + p][j + q] - u[i + p][j + q] * v[i][j]);
							if ((p != 0 || q != 0) && likeliness < threshold) {
								if (labels[i + p][j + q] != null) {
									if (labels[i][j] == null) {
										labels[i][j] = labels[i + p][j + q];
									} else {
										labels[i][j].value = labels[i + p][j + q].value;
									}
								} else {
									if (labels[i][j] == null) {
										labels[i][j] = labels[i + p][j + q] = new Label(nextLabel++);
									} else {
										labels[i + p][j + q] = labels[i][j];
									}
								}
							}
						}
					} // neighborhood
			}
		}
		// System.out.println("labels: " + nextLabel + " -- " + w * h +
		// " pixels -- " + (int) (100 * nextLabel / (w * h))
		// + "% isolation");

	}

	public static class Label {
		public int value;

		public Label(int value) {
			this.value = value;
		}
	}

	/** Expose the computed X velocities. */
	public float[][] getU() {
		return u;
	}

	/** Expose the computed Y velocities. */
	public float[][] getV() {
		return v;
	}

	/** Expose the computed velocity norms. */
	public float[][] getNorms() {
		return norms;
	}

	/** Expose the computed labels. */
	public Label[][] getLabels() {
		return labels;
	}

}
