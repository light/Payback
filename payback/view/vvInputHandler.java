package payback.view;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vnes.InputHandler;

public class vvInputHandler implements InputHandler ,Runnable {
    private int pressedButton = -1;

    public vvInputHandler() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate( this, 0, 100, TimeUnit.MILLISECONDS );
    }

    public short getKeyState( int padKey ) {
        return (short) (pressedButton == padKey ? 0x41 : 0x40);
    }

    public void mapKey( int padKey, int deviceKey ) {}

    public void reset() {}

    public void update() {}

    public void run() {
       pressedButton = new Random().nextInt( NUM_KEYS );
    }

}
