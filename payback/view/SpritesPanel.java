package payback.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import payback.algo.FlowAlgorithm;
import vnes.NES;
import vnes.NameTable;
import vnes.PPU;
import vnes.vNES;

public class SpritesPanel extends JPanel {
    private Rectangle mmRect;
    private vNES applet;

    public SpritesPanel(vNES applet) {
        this.applet = applet;
        setPreferredSize(new Dimension(256, 240));
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        g.setFont( new Font( "sans serif", Font.PLAIN, 10 ) );

        BufferedImage image = new BufferedImage( 256, 240, BufferedImage.TYPE_INT_RGB );

        drawSilhouette( image.getGraphics(), true, false );
        doGrouping( image, null, null );

        g.drawImage( image, 0, 0, null );

        if( mmRect != null ) {
            g.setColor( Color.RED );
            g.drawRect( mmRect.x, mmRect.y, mmRect.width, mmRect.height );
        }

    }


    private void drawSilhouette( Graphics g, boolean renderSprites, boolean renderBackground ) {
        NES nes = applet.nes;
        if (nes == null){
            return; // avoid NPE
        }
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
    
    /**
     * Draws bounding rectangles around groups of lit pixels. Two pixels are considered part of the same group if they
     * are within 2 pixels of each other (Manhattan distance).
     * @param image
     * @param v 
     * @param u 
     */
    private static void doGrouping( BufferedImage image, float[][] u, float[][] v ) {
        int N = 2; // Max neighborhood distance
        int L = 50; // Min brightness to be considered lit
        List<Rectangle> groups = new ArrayList<Rectangle>();

        int w = image.getWidth();
        int h = image.getHeight();

        for( int x = 0; x < w; x++ ) {
            for( int y = 0; y < h; y++ ) {
                out: if( isPixelLit(image, L, x, y) ) {
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
                    if( r1.intersects( r2 ) ) {
                        r1.setBounds( r1.union( r2 ) );
                        groups.remove( j );
                        j--;
                    }
                }
            }
        }

        Graphics g = image.getGraphics();
        g.setColor(Color.green);
        for (Rectangle r : groups) {
            g.drawRect(r.x, r.y, r.width, r.height);
            if (u != null && v != null) {
                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                float uMean = 0f;
                float vMean = 0f;
                int nb = 0;
                for (int i = r.x; i < r.x + r.width; i++) {
                    for (int j = r.y; j < r.y + r.height; j++) {
                        if (!isPixelLit(image, L, i, j))
                            continue;
                        uMean += u[i][j];
                        vMean += v[i][j];
                        nb++;
                    }
                }
                uMean /= nb;
                vMean /= nb;
                double norm = Math.sqrt(uMean * uMean + vMean * vMean);
                g.drawLine(cx, cy, cx + (int) (10 * uMean / norm), cy + (int) (10 * vMean / norm));
                g.setColor(Color.cyan);
                g.drawLine(cx, cy, cx + (int) (10 * uMean), cy + (int) (10 * vMean));
                g.setColor(Color.green);
            }
        }

    }

    private static boolean isPixelLit(BufferedImage image, int L, int x, int y) {
        int rgb = image.getRGB( x, y );
        return L < 255 * FlowAlgorithm.getBrightness(rgb);
//        int rr = (rgb & 0xFF0000) >> 16;
//        int gg = (rgb & 0xFF00) >> 8;
//        int bb = (rgb & 0xFF);
//        int max = (rr > gg) ? rr : gg;
//        if (bb > max) max = bb;
//        boolean lit = max > L;
//        return lit;
    }
    
}

