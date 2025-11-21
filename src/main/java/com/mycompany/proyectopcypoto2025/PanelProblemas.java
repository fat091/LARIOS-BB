package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.util.LinkedHashMap; import java.util.Map;
public class PanelProblemas extends JPanel implements Reseteable, Demoable, SyncAware {
    private final CardLayout cards=new CardLayout(); private final Map<String,JPanel> map=new LinkedHashMap<>(); private String currentKey="Productor-Consumidor";
    public PanelProblemas(){ setLayout(cards);
        map.put("Productor-Consumidor", new ProductorConsumidorPanel());
        map.put("Cena de Filósofos", new CenaFilosofosPanel());
        map.put("Barbero Dormilón", new BarberoDormilonPanel());
        map.put("Fumadores", new FumadoresPanel());
        map.put("Lectores-Escritores", new LectoresEscritoresPanel());
        map.put("Clúster GPU", new GpuClusterPanel()); 
        for(Map.Entry<String,JPanel> e: map.entrySet()) add(e.getValue(), e.getKey());
        cards.show(this,"Productor-Consumidor");
    }
    public void mostrar(String key){ 
        currentKey=key; 
        cards.show(this,key); 
        revalidate(); 
        repaint();
    }
    public String getCurrentKey(){ return currentKey; }
    @Override public void reset(){ for(JPanel p: map.values()) if(p instanceof Reseteable r) r.reset(); }
    @Override public void demo(){ for(JPanel p: map.values()) if(p instanceof Demoable d) d.demo(); }
    @Override public void setSyncMode(SyncMode m){ 
<<<<<<< HEAD
=======
        // Solo establece el modo en el panel actualmente visible
>>>>>>> 23e934d68df5d3d253bd9fa0253db12338618c8b
        JPanel currentPanel = map.get(currentKey);
        if(currentPanel instanceof SyncAware s) s.setSyncMode(m);
    }
}