
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.util.Random;
public class FumadoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Mesa mesa=new Mesa(); private volatile boolean running=false; private Thread agente, ciclo; private SyncMode modo=SyncMode.SEMAFOROS;
    public FumadoresPanel(){ setLayout(new BorderLayout()); add(mesa,BorderLayout.CENTER); start(); }
    private void start(){ if(running) return; running=true; Random rnd=new Random();
        agente = new Thread(() -> { while(running){ int a=rnd.nextInt(3); int b=(a+1+rnd.nextInt(2))%3; mesa.setRecursos(a,b); dormir(220);} },"Agente");
        ciclo = new Thread(() -> { while(running){ int fum=mesa.smokerWith(mesa.a, mesa.b); mesa.fumando=fum; mesa.t++; dormir(260); mesa.fumando=-1; } },"Fumadores");
        agente.start(); ciclo.start();
    }
    @Override public void setSyncMode(SyncMode m){ this.modo=m; }
    @Override public void reset(){ running=false; mesa.reset(); start(); }
    @Override public void demo(){}
    private void dormir(long ms){ try{ Thread.sleep(ms);}catch(Exception ignored){} }

    static class Mesa extends JPanel{
        int a=-1,b=-1; int fumando=-1; int t=0;
        public Mesa(){ setPreferredSize(new Dimension(560,260)); setBackground(Color.white); new Timer(33,e->{t++; repaint();}).start(); }
        public synchronized void setRecursos(int a,int b){ this.a=a; this.b=b; }
        public synchronized void reset(){ a=b=-1; fumando=-1; }
        public int smokerWith(int a,int b){ if(a==-1||b==-1) return -1; int falta=3 - a - b; return falta; }
        @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(240,240,240)); g2.fillRoundRect(200, 60, 160, 80, 16,16); g2.setColor(Color.GRAY); g2.drawRoundRect(200, 60, 160, 80, 16,16);
            String[] names={"Papel","Tabaco","Cerillos"};
            for(int i=0;i<3;i++){ int rx=220, ry=80+i*18; boolean presente = (i==a || i==b); g2.setColor(presente? new Color(120,220,160): new Color(230,230,230)); g2.fillRoundRect(rx, ry, 120, 14, 8,8); g2.setColor(Color.DARK_GRAY); g2.drawRoundRect(rx, ry, 120, 14, 8,8); g2.setColor(Color.BLACK); g2.drawString(names[i], rx+6, ry+11); }
            for(int i=0;i<3;i++){ int x=140+i*140, y=190; Color c = (i==fumando)? new Color(120,220,160): new Color(200,210,255);
                DrawUtil.person(g2, x, y, c); g2.setColor(Color.BLACK); g2.drawString("F"+i, x-6, y+50);
                if(i==fumando){ g2.setColor(new Color(100,100,100,160)); DrawUtil.smoke(g2, x+16, y-18, t); } }
            g2.dispose();
        }
    }
}
