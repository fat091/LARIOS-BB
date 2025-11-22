package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.awt.Component;
public class ProyectoPCyPoto2025 extends JFrame{
    private final PanelProblemas izq=new PanelProblemas(); private final GrafoPanel derG=new GrafoPanel(); private final MetricasPanelJFree derM=new MetricasPanelJFree();
    public ProyectoPCyPoto2025(){
        super("Proyecto PCyP Otoño 2025 - Animaciones");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setMinimumSize(new Dimension(1280,800)); setLocationByPlatform(true); setJMenuBar(menu());
        JSplitPane right=new JSplitPane(JSplitPane.VERTICAL_SPLIT, wrap("Grafo de Recursos", derG), wrap("Métricas (JFreeChart)", derM)); right.setResizeWeight(0.6);
        JSplitPane root=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrap("Animaciones de Problemas", izq), right); root.setResizeWeight(0.5);
        getContentPane().add(root, BorderLayout.CENTER); derM.demo();
    }
    private JMenuBar menu(){
        JMenuBar mb=new JMenuBar();
        JMenu archivo=new JMenu("Archivo"); JMenuItem salir=new JMenuItem("Salir"); salir.addActionListener(e->System.exit(0)); archivo.add(salir); mb.add(archivo);
        JMenu synch=new JMenu("Synch");
        synch.add(itemSync("Semáforos", SyncMode.SEMAFOROS));
        synch.add(itemSync("Variables de condición", SyncMode.VAR_CONDICION));
        synch.add(itemSync("Monitores", SyncMode.MONITORES));
        synch.add(itemSync("Mutex", SyncMode.MUTEX));
        synch.add(itemSync("Barreras", SyncMode.BARRERAS));
        mb.add(synch);
        JMenu graf=new JMenu("Gráfica"); JMenuItem scroll=new JMenuItem("Scroll"); scroll.addActionListener(e->derM.setMode(MetricasPanelJFree.Mode.SCROLL)); JMenuItem carr=new JMenuItem("Carrusel"); carr.addActionListener(e->derM.setMode(MetricasPanelJFree.Mode.CARRUSEL)); JMenuItem acor=new JMenuItem("Acordeón"); acor.addActionListener(e->derM.setMode(MetricasPanelJFree.Mode.ACORDEON)); graf.add(scroll); graf.add(carr); graf.add(acor); mb.add(graf);
        JMenu prob=new JMenu("Problemas"); 
        prob.add(itemProb("Productor-Consumidor")); 
        prob.add(itemProb("Cena de Filósofos")); 
        prob.add(itemProb("Barbero Dormilón")); 
        prob.add(itemProb("Fumadores")); 
        prob.add(itemProb("Lectores-Escritores"));
        prob.add(itemProb("Clúster GPU")); 
        mb.add(prob);
        JMenu dead=new JMenu("Deadlock");
        JMenuItem eje=new JMenuItem("Ejecutar");
        eje.addActionListener(e->{
            derG.deadlockEjecutar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())){
                for (Component c: izq.getComponents()){
                    if (c instanceof CenaFilosofosPanel cf){ cf.setDeadlock(true); break; }
                }
                derM.setPaused(true);
            }
        });
        JMenuItem ev=new JMenuItem("Evitar");
        ev.addActionListener(e->{
            derG.deadlockEvitar();
            if ("Cena de Filósofos".equals(izq.getCurrentKey())){
                for (Component c: izq.getComponents()){
                    if (c instanceof CenaFilosofosPanel cf){ cf.setDeadlock(false); break; }
                }
                derM.setPaused(false);
            }
        });
        dead.add(eje); dead.add(ev); mb.add(dead);
        return mb;
    }
    private JMenuItem itemSync(String n, SyncMode m){ JMenuItem it=new JMenuItem(n); 

        it.addActionListener(e->izq.setSyncMode(m)); // Solo actualiza el modo en el panel activo

        return it; 
    }
    private JMenuItem itemProb(String n){ JMenuItem it=new JMenuItem(n); 
        it.addActionListener(e->{

            izq.mostrar(n); 
            derG.mostrarProblema(n); 
            SwingUtilities.getWindowAncestor(this).revalidate(); 
            SwingUtilities.getWindowAncestor(this).repaint();

            izq.mostrar(n); // Muestra el panel de animación (izquierda)
            derG.mostrarProblema(n); // Muestra el grafo de recursos (derecha)

        }); 
        return it; 
    }
    private static JComponent wrap(String title, JComponent inner){ JPanel p=new JPanel(new BorderLayout()); JLabel lbl=new JLabel(title); lbl.setBorder(BorderFactory.createEmptyBorder(2,8,2,8)); lbl.setFont(lbl.getFont().deriveFont(Font.BOLD)); p.add(lbl,BorderLayout.NORTH); p.add(inner,BorderLayout.CENTER); return p; }
    public static void main(String[] args){ SwingUtilities.invokeLater(()->{ try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }catch(Exception ignored){} new ProyectoPCyPoto2025().setVisible(true); }); }
}