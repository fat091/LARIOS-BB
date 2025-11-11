
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*;
public class GrafoPanel extends JPanel implements Reseteable, Demoable {
    private final DibujaGrafo g = new DibujaGrafo();
    public GrafoPanel(){ setLayout(new BorderLayout()); add(new JScrollPane(g), BorderLayout.CENTER); }
    public void deadlockEjecutar(){ g.ejecutar(); }
    public void deadlockEvitar(){ g.evitar(); }
    @Override public void reset(){ g.evitar(); }
    @Override public void demo(){ g.ejecutar(); }
}
