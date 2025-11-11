
package com.mycompany.proyectopcypoto2025;
import java.awt.*;
public class DrawUtil {
    public static void person(Graphics2D g2, int x,int y, Color c){
        g2.setColor(c); g2.fillOval(x-12,y-28,24,24); // head
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y-4, x, y+20); // body
        g2.drawLine(x, y+20, x-10, y+36); // leg L
        g2.drawLine(x, y+20, x+10, y+36); // leg R
        g2.drawLine(x, y+6, x-12, y+14); // arm L
        g2.drawLine(x, y+6, x+12, y+14); // arm R
    }
    public static void smoke(Graphics2D g2, int x,int y, int t){
        for(int i=0;i<5;i++){ int r=6+i*2; int ox=(int)(Math.sin((t+i)*0.2)*4); int oy=i*10; g2.drawOval(x+ox, y-oy, r, r); }
    }
}
