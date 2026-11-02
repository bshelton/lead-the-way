package com.lumbylabs.routes;

import java.awt.*;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class GuidePanel extends PluginPanel {
    private static final Color GOLD = new Color(242,190,101), MUTED = new Color(184,184,180);
    private static final Color LINE = new Color(68,68,64), CONTROL = new Color(32,32,32);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    final JLabel detail = label("", 12, false, Color.WHITE);
    private final LeadTheWayPlugin plugin;
    private final JPanel body = column();
    private JLabel caption;
    private JPanel information;
    private String packingTitle;
    private java.util.Map<String,String> packingRows;
    private int packingWidth;

    void showDetail(String text, boolean active) {
        if (information != null) information.getParent().setVisible(true);
        if (packingRows != null) {
            packingRows = null;
            information.removeAll();
            information.add(detail);
        }
        if (caption != null) caption.setText(active ? "Next step" : "What to bring");
        detail.setText(text);
        revalidate();
        repaint();
    }

    void showPacking(String title, java.util.Map<String,String> rows) {
        if (information == null) return;
        information.getParent().setVisible(true);
        caption.setText("Bank checklist");
        if (title.equals(packingTitle) && rows.equals(packingRows)) return;
        packingTitle = title;
        packingRows = rows;
        renderPacking();
    }

    private void renderPacking() {
        if (packingRows == null) return;
        Insets padding = information.getInsets();
        packingWidth = information.getWidth();
        int width = packingWidth - padding.left - padding.right;
        if (width <= 0) return;
        information.removeAll();
        JTextArea title = new JTextArea(packingTitle);
        title.setFont(BODY.deriveFont(Font.BOLD));
        title.setForeground(Color.WHITE);
        title.setOpaque(false);
        title.setEditable(false);
        title.setFocusable(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setBorder(null);
        title.setSize(width, Short.MAX_VALUE);
        row(information, title, title.getPreferredSize().height);
        for (var entry : packingRows.entrySet()) {
            information.add(Box.createVerticalStrut(6));
            JPanel line = new JPanel(); line.setOpaque(false);
            JLabel name = label(entry.getKey(), 12, false, Color.WHITE);
            String action = entry.getValue();
            JLabel status = label(action, 11, false, action.equals("Packed") ? new Color(169,219,131) : GOLD);
            boolean fits = name.getPreferredSize().width + 8 + status.getPreferredSize().width <= width;
            line.setLayout(new BorderLayout(8,2));
            line.add(name, BorderLayout.WEST);
            line.add(status, fits ? BorderLayout.EAST : BorderLayout.SOUTH);
            row(information, line, line.getPreferredSize().height);
        }
        for (Container c = information; c != null && c != this; c = c.getParent()) c.invalidate();
        information.revalidate(); information.repaint();
        revalidate();
    }

    GuidePanel(LeadTheWayPlugin plugin) {
        this.plugin = plugin;
        detail.addPropertyChangeListener("text", e -> {
            for (Container c = detail.getParent(); c != null && c != this; c = c.getParent()) c.invalidate();
            revalidate();
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                if (stepWidth != getWidth() && !displayedSteps.isEmpty())
                    renderSteps(displayedSteps, displayedIndex, displayedInstruction);
            }
        });
        setLayout(new BorderLayout());
        body.setBorder(new EmptyBorder(12, 10, 10, 10));
        add(body, BorderLayout.NORTH);
        catalog();
    }
    private static JPanel column() {
        JPanel c = new JPanel() {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }
    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(BODY.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float)size));
        l.setForeground(color);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private static ImageIcon icon(String name) {
        return new ImageIcon(GuidePanel.class.getResource("/brand/" + name + ".png"));
    }
    private static void row(JPanel parent, JComponent c, int height) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        c.setPreferredSize(new Dimension(160, height));
        parent.add(c);
    }
    private static JButton button(String text, String mark, boolean primary) {
        JButton b = new JButton(text, icon(mark));
        b.setFont(BODY.deriveFont(primary ? Font.BOLD : Font.PLAIN));
        b.setIconTextGap(7);
        b.setForeground(primary ? GOLD : Color.WHITE);
        b.setBorder(BorderFactory.createLineBorder(primary ? new Color(126,101,57) : LINE));
        Color base = primary ? new Color(57,50,35) : CONTROL;
        b.setOpaque(true);
        b.setBackground(base);
        b.setRolloverEnabled(true);
        b.getModel().addChangeListener(e -> b.setBackground(b.getModel().isRollover() ? base.brighter() : base));
        return b;
    }
    private JPanel section(JLabel heading) {
        JPanel section = column();
        section.setOpaque(true);
        section.setBackground(CONTROL);
        heading.setOpaque(true);
        heading.setBackground(new Color(48, 48, 46));
        heading.setBorder(new EmptyBorder(0, 9, 0, 9));
        row(section, heading, 27);
        JPanel content = column();
        content.setBorder(new EmptyBorder(8, 9, 9, 9));
        section.add(content);
        body.add(section);
        return content;
    }
    private void gap(int n) { body.add(Box.createVerticalStrut(n)); }
    private final java.util.Map<Route, ImageIcon> portraits = new java.util.HashMap<>();

    void loadPortraits(net.runelite.client.game.SpriteManager sprites) {
        for (Route route : Route.CATALOG) {
            if (route.portraits.length == 0) continue;
            ImageIcon badge = destinationIcon(route);
            for (int i = 0; i < route.portraits.length; i++) {
                final int x = i * 15;
                sprites.getSpriteAsync(route.portraits[i], 0, image -> SwingUtilities.invokeLater(() -> {
                    Graphics2D g = (Graphics2D) badge.getImage().getGraphics();
                    g.drawImage(image, x, 0, 19, 19, null);
                    g.dispose();
                    repaint();
                }));
            }
        }
    }

    private ImageIcon destinationIcon(Route route) {
        if (route == Route.HILL_GIANTS) return icon("giant-head");
        if (route.portraits.length == 0) return icon("sign-16");
        return portraits.computeIfAbsent(route, r -> new ImageIcon(new java.awt.image.BufferedImage(
            19 + 15 * (r.portraits.length - 1), 19, java.awt.image.BufferedImage.TYPE_INT_ARGB)));
    }

    private void header() {
        body.removeAll();
        JPanel brand = new JPanel(new BorderLayout(9,0));
        brand.setOpaque(false);
        JLabel mark = new JLabel(icon("sign-32"));
        brand.add(mark, BorderLayout.WEST);
        JPanel titles = column();
        titles.add(label("Lead the Way", 16, true, GOLD));
        titles.add(label("by Lumby Labs", 11, false, MUTED));
        brand.add(titles);
        ImageIcon gear = new ImageIcon(net.runelite.client.util.ImageUtil.resizeImage(
            net.runelite.client.util.ImageUtil.loadImageResource(net.runelite.client.plugins.config.ConfigPlugin.class, "config_icon.png"), 14, 14));
        JButton settings = new JButton("Settings");
        settings.getAccessibleContext().setAccessibleName("Lead the Way settings");
        settings.setFont(BODY.deriveFont(11f));
        settings.setForeground(Color.WHITE);
        settings.setBackground(CONTROL);
        settings.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(3,4,3,4)));
        settings.setToolTipText("Lead the Way settings");
        settings.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JCheckBoxMenuItem banner = new JCheckBoxMenuItem("Show canvas helper", plugin.canvasHelperEnabled());
            banner.addActionListener(change -> plugin.setCanvasHelperEnabled(banner.isSelected()));
            menu.add(banner);
            menu.show(settings, settings.getWidth() - menu.getPreferredSize().width, settings.getHeight());
        });
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        controls.add(settings);
        brand.add(controls, BorderLayout.EAST);
        brand.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                boolean compact = brand.getWidth() < 215;
                settings.setText(compact ? "" : "Settings");
                settings.setIcon(compact ? gear : null);
                titles.getComponent(0).setFont(BODY.deriveFont(Font.BOLD, brand.getWidth() < 150 ? 13f : 16f));
                mark.setVisible(brand.getWidth() >= titles.getPreferredSize().width + controls.getPreferredSize().width + 50);
            }
        });
        row(body, brand, 38);
        gap(12);
        JPanel line = new JPanel(); line.setBackground(LINE); row(body,line,1);
        gap(16);
    }
    private void catalog() {
        packingRows = null;
        plugin.select(null, false);
        header();
        body.add(label("Find a destination", 14, true, Color.WHITE));
        gap(7);
        JPanel field = new JPanel(new BorderLayout(7,0));
        field.setBackground(CONTROL);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(0,8,0,8)));
        JLabel glass = new JLabel(icon("search"));
        JTextField search = new JTextField();
        search.setFont(BODY); search.setOpaque(false); search.setBorder(null);
        search.getAccessibleContext().setAccessibleName("Find a destination");
        field.add(glass, BorderLayout.WEST); field.add(search);
        row(body, field, 32); gap(10);
        JPanel results = section(label("Destinations", 12, true, MUTED));
        Runnable filter = () -> {
            results.removeAll();
            String query = search.getText().strip().toLowerCase(Locale.ROOT);
            for (Route route : Route.CATALOG) {
                if (!(route.name.toLowerCase(Locale.ROOT) + " " + route.aliases).contains(query)) continue;
                JButton result = button("", "forward", false);
                result.setIcon(null);
                result.setActionCommand(route.name);
                result.addActionListener(e -> preview(route));
                result.setLayout(new BorderLayout(4, 0));
                result.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(5, 1, 5, 1)));
                ImageIcon portrait = destinationIcon(route);
                JLabel art = new JLabel();
                // Draw from the live sprite buffer as RuneLite fills the boss portraits asynchronously.
                art.setIcon(new Icon() {
                    public int getIconWidth() { return 30; }
                    public int getIconHeight() { return 30; }
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        int height = 30 * portrait.getIconHeight() / portrait.getIconWidth();
                        g.drawImage(portrait.getImage(), x, y + (30-height)/2, 30, height, c);
                    }
                });
                result.add(art, BorderLayout.WEST);
                JPanel words = column();
                JLabel name = label(route.name, 12, true, Color.WHITE);
                words.add(name);
                words.add(Box.createVerticalStrut(3));
                JLabel length = label(route.length.label, 11, false, MUTED);
                length.setIcon(new Icon() {
                    public int getIconWidth() { return 31; }
                    public int getIconHeight() { return 6; }
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        for (int bar = 0; bar < 4; bar++) {
                            g.setColor(bar <= route.length.ordinal() ? GOLD : LINE);
                            g.fillRect(x + 8*bar, y+2, 6, 3);
                        }
                    }
                });
                words.add(length);
                result.add(words, BorderLayout.CENTER);
                result.add(new JLabel(icon("forward")), BorderLayout.EAST);
                result.getAccessibleContext().setAccessibleName(route.name + ", " + route.length.label + " route");
                row(results, result, 46);
            }
            if (results.getComponentCount() == 0) results.add(label("No matching destinations",12,false,MUTED));
            results.revalidate(); results.repaint();
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter.run(); }
            public void removeUpdate(DocumentEvent e) { filter.run(); }
            public void changedUpdate(DocumentEvent e) { filter.run(); }
        });
        search.addActionListener(e -> {
            if (results.getComponentCount()==1 && results.getComponent(0) instanceof JButton)
                ((JButton)results.getComponent(0)).doClick(0);
        });
        filter.run(); revalidate(); repaint();
    }
    private JPanel upcomingBox;
    private final JPanel upcomingRows = column();
    private final java.util.List<String> traversed = new java.util.ArrayList<>();

    void setUpcoming(java.util.List<String> steps) {
        renderSteps(steps, -1, null);
    }

    void showProgress(String milestone, String instruction, java.util.List<String> remaining) {
        if (upcomingBox == null) return;
        int earlier = traversed.indexOf(milestone);
        if (earlier >= 0) traversed.subList(earlier, traversed.size()).clear();
        traversed.add(milestone);
        java.util.List<String> steps = new java.util.ArrayList<>(traversed);
        for (String future : remaining) {
            if (!future.equals(steps.get(steps.size() - 1))) steps.add(future);
        }
        information.getParent().setVisible(false);
        renderSteps(steps, traversed.size() - 1, instruction);
    }

    private java.util.List<String> displayedSteps = java.util.List.of();
    private int displayedIndex = -1, stepWidth;
    private String displayedInstruction;

    private void renderSteps(java.util.List<String> steps, int current, String instruction) {
        if (upcomingBox == null) return;
        displayedSteps = java.util.List.copyOf(steps);
        displayedIndex = current;
        displayedInstruction = instruction;
        stepWidth = getWidth();
        upcomingRows.removeAll();
        for (int i = 0; i < steps.size(); i++) {
            boolean here = i == current;
            JPanel card = column();
            card.setOpaque(true);
            boolean completed = i < current;
            card.setBackground(CONTROL);
            JPanel bar = new JPanel(new BorderLayout(6, 0));
            bar.setAlignmentX(LEFT_ALIGNMENT);
            bar.setBorder(new EmptyBorder(8, 8, 8, 8));
            bar.setBackground(here ? new Color(231, 159, 41) : new Color(23, 23, 23));
            String title = steps.get(i);
            JTextArea name = stepText(title, true, here ? Color.BLACK : completed ? MUTED : Color.WHITE);
            sizeStepText(name, 88);
            bar.add(name, BorderLayout.CENTER);
            String status = here ? "Current · " : completed ? "Done · " : "";
            String description = status + "Step " + (i + 1) + " of " + steps.size();
            JLabel indicator = label(completed ? "✓" : here ? "▶" : "□", 12, true, here ? Color.BLACK : MUTED);
            indicator.getAccessibleContext().setAccessibleName(description);
            indicator.setToolTipText(description);
            bar.add(indicator, BorderLayout.EAST);
            card.add(bar);
            if (here && instruction != null && !instruction.equals(title)) {
                JTextArea action = stepText(instruction, false, GOLD);
                action.setBorder(new EmptyBorder(7, 8, 8, 8));
                sizeStepText(action, 55);
                card.add(action);
            }
            if (i > 0) upcomingRows.add(Box.createVerticalStrut(5));
            upcomingRows.add(card);
            if (here) SwingUtilities.invokeLater(() -> {
                if (card.isShowing()) card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
            });
        }
        upcomingBox.setVisible(!steps.isEmpty());
        for (Container c = upcomingRows; c != null && c != this; c = c.getParent()) c.invalidate();
        revalidate(); repaint();
    }

    private void sizeStepText(JTextArea text, int gutters) {
        int width = Math.max(48, (stepWidth > 0 ? stepWidth : 225) - gutters);
        text.setSize(width, Short.MAX_VALUE);
        text.setPreferredSize(new Dimension(width, text.getPreferredSize().height));
    }

    private static JTextArea stepText(String text, boolean bold, Color color) {
        JTextArea area = new JTextArea(text);
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setFont(bold ? BODY.deriveFont(Font.BOLD) : BODY);
        area.setForeground(color);
        return area;
    }

    private void preview(Route route) {
        traversed.clear();
        plugin.select(route, false);
        detail.setPreferredSize(null);
        detail.setMinimumSize(null);
        header();
        JLabel heading = label(route.name, 15, true, Color.WHITE);
        heading.setIcon(destinationIcon(route));
        heading.setIconTextGap(7);
        row(body, heading, 25);
        heading.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                Icon portraits = destinationIcon(route);
                int needed = portraits.getIconWidth() + heading.getIconTextGap()
                    + heading.getFontMetrics(heading.getFont()).stringWidth(route.name);
                heading.setIcon(heading.getWidth() >= needed ? portraits : null);
            }
        });
        gap(3);
        body.add(label((route.members ? "Members" : "Free-to-play") + " · " + route.length.label, 11, false, MUTED));
        body.add(label(route.minimumAgility == 0 ? "No Agility required" : "Agility level " + route.minimumAgility, 11, false, MUTED));
        gap(12);
        packingRows = null;
        caption = label(route.items.isEmpty() ? "Route preview" : "What to bring", 12, true, MUTED);
        information = section(caption);
        information.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                if (e.getComponent() == information && information.getWidth() != packingWidth) renderPacking();
            }
        });
        detail.setText("Waiting for inventory…");
        information.add(detail);
        // Reserve space for a wrapped instruction when the checklist changes to guidance.
        gap(12);
        JButton start = button("Guide me", "go", true);
        row(body, start, 34); gap(6);
        JButton back = button("Destinations", "back", false);
        row(body, back, 30);
        start.addActionListener(e -> {
            plugin.select(route, true);
            detail.setMinimumSize(null);
            detail.setPreferredSize(null);
            caption.setForeground(GOLD); caption.setBackground(new Color(58, 50, 35));
            caption.setText("Next step"); detail.setText("Finding your stage…");
            start.setVisible(false); back.setText("End route"); back.setIcon(icon("end"));
        });
        gap(12);
        JPanel contents = section(label("Route steps", 12, true, MUTED));
        upcomingBox = (JPanel) contents.getParent();
        contents.add(upcomingRows);
        setUpcoming(java.util.List.of());
        back.addActionListener(e -> catalog());
        revalidate(); repaint();
    }
}
