package com.lumbylabs.routes;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.Locale;
import net.runelite.client.ui.PluginPanel;

final class GuidePanel extends PluginPanel {
    final JLabel detail=new JLabel();
    private final LeadTheWayPlugin plugin;
    private final JPanel body=new JPanel();
    GuidePanel(LeadTheWayPlugin plugin) {
        this.plugin=plugin;
        setLayout(new BorderLayout()); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        add(body,BorderLayout.NORTH); catalog();
    }
    private void header() {
        body.removeAll(); JLabel title=new JLabel("Lead the Way"); title.setForeground(new Color(246,189,76));
        title.setFont(title.getFont().deriveFont(Font.BOLD,18f)); body.add(title);
        body.add(new JLabel("by Lumby Labs")); body.add(Box.createVerticalStrut(16));
    }
    private void catalog() {
        plugin.guiding=false; plugin.selected=null; plugin.step=null; header();
        body.add(new JLabel("Find a destination")); JTextField search=new JTextField(); body.add(search);
        JPanel results=new JPanel(); results.setLayout(new BoxLayout(results,BoxLayout.Y_AXIS)); body.add(results);
        Runnable filter=()->{
            results.removeAll(); String q=search.getText().strip().toLowerCase(Locale.ROOT);
            for(Route r:Route.CATALOG) if((r.name.toLowerCase(Locale.ROOT)+" "+r.aliases).contains(q)){
                JButton b=new JButton(r.name); b.addActionListener(e->preview(r)); results.add(b);
            }
            if(results.getComponentCount()==0) results.add(new JLabel("No matching destinations"));
            results.revalidate(); results.repaint();
        };
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filter.run();}
            public void removeUpdate(DocumentEvent e){filter.run();}
            public void changedUpdate(DocumentEvent e){filter.run();}
        }); filter.run(); revalidate(); repaint();
    }
    private void preview(Route r) {
        plugin.selected=r; header(); body.add(new JLabel(r.name)); body.add(Box.createVerticalStrut(8));
        JLabel caption=new JLabel(r.items.isEmpty()?"Route preview":"What to bring"); body.add(caption);
        detail.setText("Checking supplies…"); body.add(detail); JButton start=new JButton("Guide me"); body.add(start);
        start.addActionListener(e->{plugin.guiding=true; caption.setText("Next step"); detail.setText("Finding your stage…"); start.setVisible(false);});
        JButton back=new JButton("Close route"); back.addActionListener(e->catalog()); body.add(back); revalidate(); repaint();
    }
}
