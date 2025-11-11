
package com.mycompany.proyectopcypoto2025;
import javax.swing.*; import java.awt.*; import java.awt.event.*; import java.awt.geom.*; import java.util.*;
public class DibujaGrafo extends JPanel {
    private final java.util.List<Nodo> nodos=new ArrayList<>(); private final java.util.List<Conexion> aristas=new ArrayList<>(); private final Map<String,Nodo> by=new HashMap<>();
    private double z=1.0; private Point2D.Double pan=new Point2D.Double(0,0); private Nodo drag; private Point off=new Point(); private boolean deadlock=false;
    public DibujaGrafo(){ setBackground(Color.white); setPreferredSize(new Dimension(900,480)); evitar(); MouseAdapter ma=new MouseAdapter(){ Point last;
        @Override public void mousePressed(MouseEvent e){ last=e.getPoint(); Point2D w=toWorld(e.getPoint()); drag=find(w); if(drag!=null){ off.x=(int)(w.getX()-drag.x); off.y=(int)(w.getY()-drag.y);} }
        @Override public void mouseDragged(MouseEvent e){ if(drag!=null){ Point2D w=toWorld(e.getPoint()); drag.x=(int)w.getX()-off.x; drag.y=(int)w.getY()-off.y; repaint(); } else { pan.x+=(e.getX()-last.x)/z; pan.y+=(e.getY()-last.y)/z; repaint(); } last=e.getPoint(); }
        @Override public void mouseWheelMoved(MouseWheelEvent e){ double old=z; z*=(e.getWheelRotation()>0?0.9:1.1); z=Math.max(0.3,Math.min(z,3)); Point2D b=toWorld(e.getPoint(),old), a=toWorld(e.getPoint(),z); pan.x+=(b.getX()-a.getX()); pan.y+=(b.getY()-a.getY()); repaint(); }
    }; addMouseListener(ma); addMouseMotionListener(ma); addMouseWheelListener(ma); }
    public void ejecutar(){ base(); conectar("P0","R1"); conectar("R1","P1"); conectar("P1","R2"); conectar("R2","P2"); conectar("P2","R3"); conectar("R3","P0"); deadlock=true; repaint(); }
    public void evitar(){ base(); conectar("P0","R1"); conectar("R1","P1"); conectar("P1","R2"); conectar("R2","P0"); deadlock=false; repaint(); }
    private void base(){ nodos.clear(); aristas.clear(); by.clear(); addN("P0",520,60,Nodo.Tipo.PROCESO,new Color(255,200,200)); addN("P1",660,200,Nodo.Tipo.PROCESO,new Color(255,240,160)); addN("P2",520,340,Nodo.Tipo.PROCESO,new Color(200,240,200)); addN("R1",420,140,Nodo.Tipo.RECURSO,new Color(160,210,255)); addN("R2",620,280,Nodo.Tipo.RECURSO,new Color(160,200,255)); addN("R3",420,280,Nodo.Tipo.RECURSO,new Color(160,190,255)); }
    private void addN(String n,int x,int y,Nodo.Tipo t,Color f){ Nodo nd=new Nodo(n,x,y,t,f); nodos.add(nd); by.put(n,nd); }
    private void conectar(String a,String b){ Nodo na=by.get(a), nb=by.get(b); if(na!=null&&nb!=null) aristas.add(new Conexion(na,nb)); }
    private Point2D toWorld(Point p){ return new Point2D.Double(p.x/z - pan.x, p.y/z - pan.y); }
    private Point2D toWorld(Point p,double zz){ return new Point2D.Double(p.x/zz - pan.x, p.y/zz - pan.y); }
    private Nodo find(Point2D p){ for(int i=nodos.size()-1;i>=0;i--){ Nodo n=nodos.get(i); double dx=p.getX()-n.x, dy=p.getY()-n.y; if(dx*dx+dy*dy<=28*28) return n; } return null; }
    @Override protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.scale(z,z); g2.translate(pan.x,pan.y);
        g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g2.setColor(deadlock?new Color(200,60,60):new Color(60,90,200));
        for(Conexion c:aristas){ g2.draw(new java.awt.geom.Line2D.Double(c.a.x,c.a.y,c.b.x,c.b.y)); arrow(g2,c.a.x,c.a.y,c.b.x,c.b.y); }
        for(Nodo n:nodos){ Shape s=(n.tipo==Nodo.Tipo.PROCESO)?new Ellipse2D.Double(n.x-26,n.y-26,52,52):new RoundRectangle2D.Double(n.x-26,n.y-26,52,52,14,14); g2.setColor(n.fill); g2.fill(s); g2.setColor(Color.DARK_GRAY); g2.setStroke(new BasicStroke(1.8f)); g2.draw(s); g2.setColor(Color.BLACK); String t=n.nombre; FontMetrics fm=g2.getFontMetrics(); g2.drawString(t,(int)(n.x-fm.stringWidth(t)/2.0),(int)(n.y+fm.getAscent()/3.0)); }
        g2.dispose(); }
    private void arrow(Graphics2D g2,double x1,double y1,double x2,double y2){ double a=Math.atan2(y2-y1,x2-x1); int L=10; double a1=a+Math.toRadians(160), a2=a-Math.toRadians(160); Polygon p=new Polygon(new int[]{(int)x2,(int)(x2+L*Math.cos(a1)),(int)(x2+L*Math.cos(a2))}, new int[]{(int)y2,(int)(y2+L*Math.sin(a1)),(int)(y2+L*Math.sin(a2))},3); g2.fill(p); }
}
