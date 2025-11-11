
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*;
public class LectoresEscritoresPanel extends JPanel implements Reseteable, Demoable, SyncAware {
    private final Pizarra piz=new Pizarra(); private volatile boolean running=false; private Thread writer, reader; private SyncMode modo=SyncMode.MONITORES;
    public LectoresEscritoresPanel(){ setLayout(new BorderLayout()); add(piz,BorderLayout.CENTER); start(); }
    private void start(){ if(running) return; running=true;
        writer=new Thread(() -> { while(running){ piz.modo="ESCRITURA"; piz.escritor=true; piz.valor++; dormir(280); piz.escritor=false; piz.modo="LECTURA"; dormir(140);} },"W");
        reader=new Thread(() -> { while(running){ if(!piz.escritor){ piz.lector=true; dormir(160); piz.lector=false; } dormir(100);} },"R");
        writer.start(); reader.start();
    }
    @Override public void setSyncMode(SyncMode m){ this.modo=m; }
    @Override public void reset(){ running=false; piz.reset(); start(); }
    @Override public void demo(){}
    private void dormir(long ms){ try{ Thread.sleep(ms);}catch(Exception ignored){} }

    static class Pizarra extends JPanel{
        String modo="LECTURA"; boolean escritor=false, lector=false; int valor=0; int t=0;
        public Pizarra(){ setPreferredSize(new Dimension(560,260)); setBackground(Color.white); new Timer(33,e->{t++; repaint();}).start(); }
        public void reset(){ modo="LECTURA"; escritor=false; lector=false; valor=0; }
        @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(30,120,60)); g2.fillRoundRect(120, 40, 320, 80, 12,12); g2.setColor(new Color(220,220,220)); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(120, 40, 320, 80, 12,12);
            g2.setColor(Color.WHITE); g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f)); String texto = modo+" (valor="+valor+")"; FontMetrics fm=g2.getFontMetrics(); g2.drawString(texto, 120+(320-fm.stringWidth(texto))/2, 40+45);
            DrawUtil.person(g2, 220, 170, lector? new Color(120,220,160): new Color(200,210,255)); g2.setColor(Color.BLACK); g2.drawString("Lector", 202, 204);
            DrawUtil.person(g2, 340, 170, escritor? new Color(120,220,160): new Color(200,210,255)); g2.setColor(Color.BLACK); g2.drawString("Escritor", 318, 204);
            g2.dispose();
        }
    }
}
