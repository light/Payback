import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import payback.Environment;
import payback.opticalflow.FlowAlgorithm.Label;

@SuppressWarnings( "serial" )
public class vvUI extends AppletUI {
    private Collection<JPanel> panels = new HashSet<JPanel>();
    private Environment environment;
    private Rectangle mmRect;

    public vvUI( vNES applet ) {
        super( applet );
        applet.setSize( 256, 240 );
        applet.sound = false;

        try {
            environment = new Environment();
        } catch( Exception e ) {
            e.printStackTrace();
            System.exit( 666 );
        }
        addFrame( 300, 0, new SpritesPanel() );
        // addFrame(300, 300, new FlowPanel());
        addFrame( 600, 0, new FlowPanel2( environment ) );

    }

    // private long frameLimiter = 0;
    @Override
    public void onEndFrame() {
        // if( frameLimiter++ % 10 == 0 ) {
        // long start = System.currentTimeMillis();
        environment.update( (BufferedImage) getScreenView().getImage() );

        for( JPanel panel : panels ) {
            panel.repaint();
        }
        // System.out.println(System.currentTimeMillis() - start);
        // }
    }

    private void addFrame( int x, int y, JPanel panel ) {
        JFrame jFrame = new JFrame( panel.getClass().getSimpleName() );
        jFrame.add( panel );
        jFrame.pack();
        jFrame.setLocation( x, y );
        jFrame.setVisible( true );
        panels.add( panel );
    }


    /**
     * Draws bounding rectangles around groups of lit pixels. Two pixels are considered part of the same group if they
     * are within 2 pixels of each other (Manhattan distance).
     * @param image
     */
    private static void doGrouping( BufferedImage image ) {
        int N = 3; // Max neighborhood distance
        double L = 100; // Min brightness to be considered lit
        List<Rectangle> groups = new ArrayList<Rectangle>();

        int w = image.getWidth();
        int h = image.getHeight();

        for( int x = 0; x < w; x++ ) {
            for( int y = 0; y < h; y++ ) {
                int rgb = image.getRGB( x, y );
                int rr = (rgb & 0xFF0000) >> 16;
                int gg = (rgb & 0xFF00) >> 8;
                int bb = (rgb & 0xFF);
                int max = (rr > gg) ? rr : gg;
                if (bb > max) max = bb;
                boolean lit = max > L;

                out: if( lit ) {
                     for( Rectangle r: groups ) {
                         if(x >= r.x-N && x <= r.x+r.width+N && y >= r.y-N && y <= r.y+r.height+N) {
                             int nx1 = Math.min( x, r.x );
                             int ny1 = Math.min( y, r.y );
                             int nx2 = Math.max( r.x+r.width, x );
                             int ny2 = Math.max( r.y+r.height, y );
                             r.setBounds( nx1, ny1, nx2-nx1, ny2-ny1 );
                             break out;
                         }
                    }
                    groups.add( new Rectangle( x, y, 0, 0 ) );
                }

            }
        }

        // Fuse overlapping rectangles
        if( groups.size()>1 ) {
            for(int i=0; i < groups.size()-1; i++) {
                for(int j=i+1; j < groups.size(); j++) {
                    Rectangle r1 = groups.get( i );
                    Rectangle r2 = groups.get( j );
                    if(r1.intersects( r2 )) {
                        r1.setBounds( r1.union( r2 ) );
                        groups.remove( j );
                        j--;
                    }
                }
            }
        }

        Graphics g = image.getGraphics();
        g.setColor( Color.green );
        for( Rectangle r: groups ) {
            g.drawRect( r.x, r.y, r.width, r.height );
        }

    }

    private class SpritesPanel extends JPanel {


        public SpritesPanel() {
            setPreferredSize( new Dimension( 256, 240 ) );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            g.setFont( new Font( "sans serif", Font.PLAIN, 10 ) );

            BufferedImage image = new BufferedImage( 256, 240, BufferedImage.TYPE_INT_RGB );

            drawSilhouette( image.getGraphics(), true, false );
            doGrouping( image );

            g.drawImage( image, 0, 0, null );

            if( mmRect != null ) {
                g.setColor( Color.RED );
                g.drawRect( mmRect.x, mmRect.y, mmRect.width, mmRect.height );
            }

        }


        private void drawSilhouette( Graphics g, boolean renderSprites, boolean renderBackground ) {
            PPU ppu = nes.getPpu();
            if( renderBackground ) {
                // int currentNametable = ppu.ntable1[0];
                NameTable nameTable = ppu.nameTable[0];
                int baseTile = (ppu.regS == 0 ? 0 : 256);
                for( int i = 0; i < 32; i++ ) {
                    for( int j = 0; j < 30; j++ ) {
                        short tile = nameTable.getTileIndex( i, j );
                        int x = i * 8;
                        int y = j * 8;
                        nes.ppu.ptTile[baseTile + tile].renderSimple( x, y, g, false, false );
                    }
                }
            }

            if( renderSprites ) {
                for( int i = 0; i < 64; i++ ) {
                    int x = ppu.sprX[i];
                    int y = ppu.sprY[i];
                    int tile = ppu.sprTile[i];
                    boolean mirrorH = nes.ppu.horiFlip[i];
                    boolean mirrorV = nes.ppu.vertFlip[i];

                    if( x >= 0 && x < 256 && y >= 0 && y < 240 ) {
                        nes.ppu.ptTile[tile].renderSimple( x, y, g, mirrorV, mirrorH );
                    }
                }
            }
        }
    }

    private class FlowPanel extends JPanel {
        private static final double SCALE_FACTOR = 3;

        public FlowPanel() {
            setPreferredSize( new Dimension( (int) (2 * 256 * SCALE_FACTOR), (int) (240 * SCALE_FACTOR) ) );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );

            float[][] u = environment.getFlow().getU();
            float[][] v = environment.getFlow().getV();
            float[][] n = environment.getFlow().getNorms();
            Label[][] l = environment.getFlow().getLabels();

            if( u == null || v == null || n == null || l == null ) {
                return; // avoid NPE
            }

            int w = u.length;
            int h = u[0].length;

            float maxNorm = 0;
            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    maxNorm = Math.max( maxNorm, n[i][j] );
                }
            }

            // just for fun (may help define a threshold ?)
            // showVelocityHistogram(g, u, v, maxNorm);

            int[] colors = new int[] { 0xFF, 0x00FF, 0x0000FF };

            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    float uij = u[i][j];
                    float vij = v[i][j];
                    float norm = n[i][j];
                    double x = i * SCALE_FACTOR;
                    double y = j * SCALE_FACTOR;

                    // show non-null labels
                    double x2 = (w + i) * SCALE_FACTOR;
                    double y2 = j * SCALE_FACTOR;
                    if( l[i][j] != null ) {
                        g.setColor( new Color( l[i][j].value * 3498238 ) );
                        g.fillRect( (int) (x2 - SCALE_FACTOR / 2), (int) (y2 - SCALE_FACTOR / 2), (int) (SCALE_FACTOR), (int) (SCALE_FACTOR) );
                    }

                    // color based on the velocity (gradient)
                    // g.setColor(new Color(Color.HSBtoRGB(0, 0, norm * 256 /
                    // maxNorm)));

                    // color based on the velocity (fractions)
                    if( norm > 0.5 * maxNorm ) {
                        g.setColor( Color.red );
                    } else if( norm < .25 * maxNorm ) {
                        g.setColor( Color.black );
                    } else {
                        g.setColor( Color.blue );
                    }

                    g.drawLine( (int) x, (int) y, (int) (x + uij * SCALE_FACTOR * .7 / maxNorm), (int) (y + vij * SCALE_FACTOR * .7 / maxNorm) );
                }
            }

        }

        /**
         * Shows a velocity histogram based on a 0 (slowest) - 250 (fastest) scale.
         */
        private void showVelocityHistogram( Graphics g, float[][] u, float[][] v, float maxNorm ) {
            int[] histo = new int[256];
            float histoStep = maxNorm / 250;
            for( int i = 0; i < u.length; i++ ) {
                for( int j = 0; j < u[0].length; j++ ) {
                    float uij = u[i][j];
                    float vij = v[i][j];
                    int k = (int) (Math.sqrt( uij * uij + vij * vij ) / histoStep);
                    histo[k]++;
                }
            }
            g.setColor( Color.GREEN );
            for( int i = 0; i < histo.length; i++ ) {
                g.drawLine( i, 0, i, histo[i] );
            }
        }
    }

    private static class FlowPanel2 extends JPanel {
        private final Environment environment;
        private static float maxNorm = 0;
        private int w = 256;
        private int h = 240;
        int[] pixels;
        private BufferedImage image;

        public FlowPanel2( Environment environment ) {
            this.environment = environment;
            setPreferredSize( new Dimension( w, h ) );
            createImage();
        }

        /*
         * Creates a bufferedImage backed by a pixel array.
         */
        private void createImage() {
            pixels = new int[w * h];
            ColorModel colorModel = new DirectColorModel( 24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0 );
            SampleModel sampleModel = colorModel.createCompatibleSampleModel(w, h);
            DataBuffer buffer = new DataBufferInt(pixels, w * h);
            WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, buffer, new Point(0,0));
            image = new BufferedImage(colorModel, raster, false, null);
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );

            float[][] u = environment.getFlow().getU();
            float[][] v = environment.getFlow().getV();
            float[][] n = environment.getFlow().getNorms();
            Label[][] l = environment.getFlow().getLabels();

            if( u == null || v == null || n == null || l == null ) {
                return; // avoid NPE
            }

            // maxNorm = 0;
            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    float norm = n[i][j];
                    norm = Math.min( norm, 2 ); // Complètement arbitraire
                    maxNorm = Math.max( maxNorm, norm );
                }
            }

            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    float uij = u[i][j];
                    float vij = v[i][j];
                    float norm = n[i][j];

                    double angle;
                    if( uij == 0 ) {
                        angle = vij > 0 ? Math.PI / 2 : -Math.PI / 2;
                    } else {
                        angle = Math.atan( vij / uij ) - (uij > 0 ? 0 : Math.PI);
                    }
                    float intensity = norm / maxNorm;
                    pixels[i + j * w] = Color.HSBtoRGB( (float) ((angle % (2 * Math.PI)) / (2 * Math.PI)), intensity, intensity );
                }
            }

            doGrouping( image );

            g.drawImage( image, 0, 0, null );
        }

    }

}
