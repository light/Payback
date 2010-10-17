package payback.opticalflow;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Regroups scattered pixels. SLOW.
 * @see http://scien.stanford.edu/class/psych221/projects/02/hrd/de-scatter.htm
 */
public class DeScatterFilter  {
    private int maxIterations;

    public DeScatterFilter( int maxIterations ) {
        this.maxIterations = maxIterations;
    }

    public static void main( String[] args ) throws Exception {
        BufferedImage image = ImageIO.read( new File( "D:\\Dev\\workspaces\\workspace\\Payback\\payback\\opticalflow\\mega_screenshot.png" ) );
        DeScatterFilter filter = new DeScatterFilter( 1 );

        long start=System.currentTimeMillis();
        BufferedImage result = filter.filter( image, null );
        System.out.println((System.currentTimeMillis()-start)+" ms");
        JFrame jFrame = new JFrame();
        jFrame.add( new JLabel( new ImageIcon( result ) ) );
        jFrame.pack();
        jFrame.setVisible( true );
    }

    public BufferedImage filter( BufferedImage src, BufferedImage dst ) {
        int width = src.getWidth();
        int height = src.getHeight();
        if( dst == null )
            dst = createCompatibleDestImage( src, null );
        int[] neighbors = new int[9*2];
        for( int n = 0; n < maxIterations; n++ ) {
            long start = System.currentTimeMillis();
            int replacements = 0;
            BufferedImage tmp = createCompatibleDestImage( src, null );
            for( int y = 0; y < height; y++ ) {
                for( int x = 0; x < width; x++ ) {
                    Arrays.fill( neighbors, 0 ); // TODO special case color = 0x00000000;
                    int max = 0;
                    int nbNeighbors = 0;
                    int rgb = src.getRGB( x, y );
                    neighbors[0] = rgb;
                    neighbors[1] = 1;
                    for( int j = Math.max( y - 1, 0 ); j <= Math.min( y + 1, height - 1 ); j++ )
                        for( int i = Math.max( x - 1, 0 ); i <= Math.min( x + 1, width - 1 ); i++ )
                            if( j != y || i != x ) {
                                nbNeighbors++;
                                int neighbor = src.getRGB( i, j );
                                int neighborIndex = 0;
                                for(; neighborIndex < 9; neighborIndex++) {
                                    if(neighbor == neighbors[neighborIndex*2] ) {
                                        neighbors[neighborIndex*2+1]++;
                                        break;
                                    } else if (neighbors[neighborIndex*2] == 0){
                                        neighbors[neighborIndex*2] = neighbor;
                                        neighbors[neighborIndex*2+1]++;
                                        break;
                                    }
                                }
                                if(neighbors[max*2+1] < neighbors[neighborIndex*2+1])
                                    max = neighborIndex;
                            }
                    if( rgb != neighbors[max*2] && neighbors[max*2+1]>= nbNeighbors / 2 ) {
                        replacements++;
                        tmp.setRGB( x, y, neighbors[max*2] );
                    } else {
                        tmp.setRGB( x, y, rgb );
                    }
                }
            }
            System.out.println( n + " "+replacements+ ", t="+ (System.currentTimeMillis()-start));
            src = tmp;
            if( replacements == 0 ) // If no pixel was regrouped this time, no need to continue
                break;
        }

        dst.getGraphics().drawImage( src, 0, 0, null );
        return dst;

    }

    public BufferedImage createCompatibleDestImage( BufferedImage src, ColorModel destCM ) {
        if( destCM == null )
            destCM = src.getColorModel();
        return new BufferedImage( destCM, destCM.createCompatibleWritableRaster( src.getWidth(), src.getHeight() ), destCM.isAlphaPremultiplied(), null );
    }

}
