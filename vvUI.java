import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import payback.Environment;

@SuppressWarnings("serial")
public class vvUI extends AppletUI {
	private JPanel spritePanel;
	private JPanel flowPanel;
	private InputHandler joy1 = new vvInputHandler();
	private Environment environment;

	public vvUI(vNES applet) {
		super(applet);

		try {
			environment = new Environment();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(666);
		}
		spritePanel = new DancingSugarPlumFairiesPanel();
		flowPanel = new FlowPanel();
		showFrame(300, 0, spritePanel);
		showFrame(300, 300, flowPanel);
		showFrame(800, 0, new MagicAddressDivinationWizardPanel());

	}

	private void showFrame(int x, int y, JComponent component) {
		JFrame jFrame = new JFrame();
		jFrame.add(component);
		jFrame.pack();
		jFrame.setLocation(x, y);
		jFrame.setVisible(true);
	}

	@Override
	public void imageReady(boolean skipFrame) {
		super.imageReady(skipFrame);
		spritePanel.repaint();
		flowPanel.repaint();
	}

	// @Override
	// public InputHandler getJoy1() {
	// return joy1;
	// }

	private class DancingSugarPlumFairiesPanel extends JPanel {
		private static final double SCALE_FACTOR = 1.5;

		public DancingSugarPlumFairiesPanel() {
			setPreferredSize(new Dimension((int) (256 * SCALE_FACTOR), (int) (240 * SCALE_FACTOR)));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setFont(new Font("sans serif", Font.PLAIN, 10));

			PPU ppu = nes.getPpu();

			// int currentNametable = ppu.ntable1[0];
			NameTable nameTable = ppu.nameTable[0];
			int baseTile = (ppu.regS == 0 ? 0 : 256);
			for (int i = 0; i < 32; i++) {
				for (int j = 0; j < 30; j++) {
					short tile = nameTable.getTileIndex(i, j);
					int x = i * 8;
					int y = j * 8;
					nes.ppu.ptTile[baseTile + tile].renderSimple(x, y, g, false, false);
				}
			}

			for (int i = 0; i < 64; i++) {
				int x = ppu.sprX[i];
				int y = ppu.sprY[i];
				int tile = ppu.sprTile[i];
				boolean mirrorH = nes.ppu.horiFlip[i];
				boolean mirrorV = nes.ppu.vertFlip[i];

				if (x >= 0 && x < 256 && y >= 0 && y < 240) {
					nes.ppu.ptTile[tile].renderSimple(x, y, g, mirrorV, mirrorH);
				}
			}

			if (mmRect != null) {
				g.setColor(Color.RED);
				g.drawRect(mmRect.x, mmRect.y, mmRect.width, mmRect.height);
			}

		}
	}

	private class FlowPanel extends JPanel {
		private static final double SCALE_FACTOR = 3;

		public FlowPanel() {
			setPreferredSize(new Dimension((int) (256 * SCALE_FACTOR), (int) (240 * SCALE_FACTOR)));
		}

		/**
		 * Computes the scalar product of velocities at (i,j) and (k,l).
		 */
		private float scalar(float[][] u, float[][] v, int i, int j, int k, int l) {
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

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			float[][] u = environment.getFlow().getU();
			float[][] v = environment.getFlow().getV();
			float[][] n = environment.getFlow().getNorm();

			if (u == null || v == null || n == null) {
				return; // avoid NPE
			}

			float maxNorm = 0;
			for (int i = 0; i < u.length; i++) {
				for (int j = 0; j < u[0].length; j++) {
					maxNorm = Math.max(maxNorm, n[i][j]);
				}
			}

			// just for fun (may help define a threshold ?)
			// showVelocityHistogram(g, u, v, maxNorm);

			for (int i = 0; i < u.length; i++) {
				for (int j = 0; j < u[0].length; j++) {
					float uij = u[i][j];
					float vij = v[i][j];
					float norm = n[i][j];
					double x = i * SCALE_FACTOR;
					double y = j * SCALE_FACTOR;

					g.setColor(new Color(Color.HSBtoRGB(0, 0, norm * 256 / maxNorm)));
					if (norm > 0.5 * maxNorm) {
						g.setColor(Color.red);
					} else if (norm < .25 * maxNorm) {
						g.setColor(Color.blue);
					} else {
						g.setColor(Color.black);
					}

					// if(norm > 0.00001f)
					g.drawLine((int) x, (int) y, (int) (x + uij * SCALE_FACTOR * .7 / maxNorm), (int) (y + vij
							* SCALE_FACTOR * .7 / maxNorm));

					// find area of same velocity
					g.setColor(Color.CYAN);
					double threshold = .3; // related to the histogram ?
					for (int p = -1; p < 2; p++) {
						for (int q = -1; q < 2; q++) {
							if ((p != 0 || q != 0) && Math.sqrt(scalar(u, v, i, j, i + p, j + q)) > threshold * maxNorm) {
								g.drawRect((int) x - 2, (int) y - 2, 5, 5);
							}
						}
					}

				}
			}

		}

		/**
		 * Shows a velocity histogram based on a 0 (slowest) - 250 (fastest)
		 * scale.
		 */
		private void showVelocityHistogram(Graphics g, float[][] u, float[][] v, float maxNorm) {
			int[] histo = new int[256];
			float histoStep = maxNorm / 250;
			for (int i = 0; i < u.length; i++) {
				for (int j = 0; j < u[0].length; j++) {
					float uij = u[i][j];
					float vij = v[i][j];
					int k = (int) (Math.sqrt(uij * uij + vij * vij) / histoStep);
					histo[k]++;
				}
			}
			g.setColor(Color.GREEN);
			for (int i = 0; i < histo.length; i++) {
				g.drawLine(i, 0, i, histo[i]);
			}
		}
	}

	private class MagicAddressDivinationWizardPanel extends JPanel {
		private static final int RAM_SIZE = 0x800;
		private short[] snapshot = new short[RAM_SIZE];
		private boolean[] matches = new boolean[RAM_SIZE];
		private JTextArea textArea;

		public MagicAddressDivinationWizardPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			add(new JButton(new StartAnalysisAction()));
			add(new JButton(new FindChangesAction()));
			add(new JButton(new ExcludeChangesAction()));
			textArea = new JTextArea(20, 50);
			add(new JScrollPane(textArea));
		}

		private short[] getCPUMemory() {
			short[] mem = new short[0x800];
			System.arraycopy(nes.cpuMem.mem, 0, mem, 0, RAM_SIZE);
			return mem;
		}

		private void displayMatches() {
			int matchCount = 0;
			for (int i = 0; i < matches.length; i++) {
				if (matches[i])
					matchCount++;
			}
			textArea.append("Found " + matchCount + " potential matches.\n");
			if (matchCount <= 25) {
				for (int i = 0; i < RAM_SIZE; i++) {
					if (matches[i]) {
						textArea.append("Address: 0x" + Integer.toHexString(i) + ", value: 0x"
								+ Integer.toHexString(snapshot[i]) + "\n");
					}
				}
			}
			textArea.scrollRectToVisible(new Rectangle(0, textArea.getHeight() - 2, 1, 1));

		}

		private class StartAnalysisAction extends AbstractAction {

			public StartAnalysisAction() {
				super("(Re)Start analysis");
			}

			public void actionPerformed(ActionEvent e) {
				snapshot = getCPUMemory();
				Arrays.fill(matches, true);
				textArea.append("Analysis started.\n");
			}
		}

		private class FindChangesAction extends AbstractAction {

			public FindChangesAction() {
				super("Find addresses that have changed");
			}

			public void actionPerformed(ActionEvent e) {
				short[] mem = getCPUMemory();
				for (int i = 0; i < RAM_SIZE; i++) {
					if (snapshot[i] == mem[i])
						matches[i] = false;
				}
				snapshot = mem;
				displayMatches();
			}

		}

		private class ExcludeChangesAction extends AbstractAction {

			public ExcludeChangesAction() {
				super("Find addresses that do not have changed");
			}

			public void actionPerformed(ActionEvent e) {
				short[] mem = getCPUMemory();
				for (int i = 0; i < RAM_SIZE; i++) {
					if (snapshot[i] != mem[i])
						matches[i] = false;
				}
				snapshot = mem;
				displayMatches();
			}
		}

	}

	private Rectangle mmRect;

	// private long frameLimiter = 0;

	@Override
	public void onEndFrame() {
		// if( frameLimiter++ % 10 == 0 ) {
		long start = System.currentTimeMillis();
		environment.update((BufferedImage) getScreenView().getImage());
		System.out.println(System.currentTimeMillis() - start);
		// }
	}

}
