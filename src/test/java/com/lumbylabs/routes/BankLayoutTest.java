package com.lumbylabs.routes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankLayoutTest {
    private static Component find(Container parent, String text) {
        for (Component c : parent.getComponents()) {
            if (c instanceof AbstractButton && text.equals(((AbstractButton)c).getActionCommand())) return c;
            if (c instanceof JLabel && (text.equals(((JLabel)c).getText()) || text.equals(((JLabel)c).getAccessibleContext().getAccessibleName()))) return c;
            if (c instanceof Container) {
                Component match = find((Container)c, text);
                if (match != null) return match;
            }
        }
        return null;
    }
    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) if (child instanceof Container) layout((Container)child);
    }
    @Test public void currentStepMovesBetweenSeparateCards() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GuidePanel panel = new GuidePanel(new LeadTheWayPlugin());
            ((JButton)find(panel,"Hill Giants")).doClick(0);
            panel.showProgress("Trapdoor", "Open the trapdoor.", java.util.List.of("North passage", "Chamber"));
            Container previous = find(panel,"Current · Step 1 of 3").getParent();
            assertNotSame(previous, find(panel,"Step 2 of 3").getParent());
            panel.showProgress("North passage", "Head north.", java.util.List.of("Chamber"));
            assertNotNull(find(panel,"Done · Step 1 of 3"));
            assertNotNull(find(panel,"Current · Step 2 of 3"));
            assertNotNull(find(panel,"Step 3 of 3"));
        });
    }
    @Test public void routeGuideFitsOnNarrowSidebar() throws Exception {
        GuidePanel[] holder = new GuidePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            GuidePanel p = new GuidePanel(new LeadTheWayPlugin());
            holder[0] = p;
            ((JButton)find(p,"Hill Giants")).doClick(0);
            ((JButton)find(p,"Guide me")).doClick(0);
            p.showProgress("Climb down the trapdoor", "Open the trapdoor in the ruins south of Edgeville bank.",
                java.util.List.of("Follow the north passage", "Follow the east corridor", "Round the eastern bend"));
            p.setSize(225,540);
            layout(p);
        });
        SwingUtilities.invokeAndWait(() -> {
            GuidePanel p = holder[0];
            layout(p);
            assertNotNull(find(p,"Current · Step 1 of 4"));
            if (Boolean.getBoolean("routes.preview")) {
                BufferedImage img = new BufferedImage(225,540,BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics(); p.printAll(g); g.dispose();
                try { ImageIO.write(img,"png",new File("build/hill-guide.png")); }
                catch(java.io.IOException e) { throw new AssertionError(e); }
            }
        });
    }
    @Test public void ferryChecklistFitsAtTwoSidebarWidths() throws Exception {
        GuidePanel[] holder = new GuidePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            GuidePanel p = new GuidePanel(new LeadTheWayPlugin());
            holder[0] = p;
            ((JButton)find(p,"Dagannoth Kings")).doClick(0);
            ((JButton)find(p,"Guide me")).doClick(0);
            LinkedHashMap<String,String> rows = new LinkedHashMap<>();
            rows.put("Ferry coins","Withdraw 1,000");
            rows.put("Pet rock","Missing"); rows.put("Rune thrownaxe","Missing");
            p.showPacking("Rellekka ferry",rows);
        });
        for (int width : new int[]{225,170}) {
            SwingUtilities.invokeAndWait(() -> { holder[0].setSize(width,420); layout(holder[0]); });
            SwingUtilities.invokeAndWait(() -> {
                GuidePanel p = holder[0]; layout(p);
                for(String value : new String[]{"Bank checklist","Rune thrownaxe","Withdraw 1,000","End route"}) {
                    Component c = find(p,value);
                    assertNotNull(value,c);
                    assertTrue(value,c.getWidth() >= c.getPreferredSize().width || c instanceof JButton || value.equals("Bank checklist"));
                    Rectangle bounds = SwingUtilities.convertRectangle(c.getParent(),c.getBounds(),p);
                    assertTrue(value,bounds.x >= 0 && bounds.x + bounds.width <= width);
                }
                if (Boolean.getBoolean("routes.preview")) {
                    BufferedImage img = new BufferedImage(width,420,BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics(); p.printAll(g); g.dispose();
                    try { ImageIO.write(img,"png",new File("build/bank-"+width+".png")); }
                    catch(java.io.IOException e) {throw new AssertionError(e);}
                }
            });
        }
    }
}
