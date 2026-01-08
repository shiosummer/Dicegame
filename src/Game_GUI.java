import javax.swing.*;
import javax.swing.text.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

public class Game_GUI extends JFrame {
    private Game_Logic game;
    private Dice_Player engine;
    private AI_Logic aiFactory;
    private String playerName = "玩家";
    private int aiCount = 1;
    private int roundCounter = 0;

    private JTextPane logArea;
    private JComboBox<Integer> qtyCombo;
    private JComboBox<Integer> faceCombo;
    private JLabel statusLabel;
    private JLabel nowPlayingLabel;
    private JPanel dicePanel;
    private JButton bidFeiBtn, bidZhaiBtn, openBtn;
    private JButton ppBtn;

    private ImageIcon[] diceIcons = new ImageIcon[7];

    // --- 🎵 音乐播放系统变量 ---
    private ArrayList<File> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;
    private Clip musicClip;
    private boolean isPaused = false;
    private long clipTimePosition = 0;

    // --- 🎭 垃圾话系统数据库 ---
    private final String[] TALK_BID = {
            "就这？我闭着眼都能赢。", "我看你印堂发黑，这把必输。", "这就是你的实力吗？",
            "如果你这把能赢，我当场把骰子吃掉！", "你的骰子是在拼多多买的吧？", "我赌你的杯子里没有 6。"
    };
    private final String[] TALK_OPEN = {
            "看好了，这就叫绝杀！", "抓到你在偷鸡了！", "别挣扎了，乖乖认输吧。", "运气也是实力的一部分。", "这就是心理战的胜利！"
    };
    private final String[] TALK_PRESSURE = {
            "快点啊，等得我代码都生锈了。", "这么久不叫，你是在算卦吗？", "别磨蹭了，我都等困了。", "你的手在抖什么？"
    };

    private ArrayList<String> bidPool = new ArrayList<>();
    private ArrayList<String> openPool = new ArrayList<>();
    private ArrayList<String> pressurePool = new ArrayList<>();

    public Game_GUI() {
        engine = new Dice_Player();
        aiFactory = new AI_Logic();
        game = new Game_Logic();

        initSettings();
        loadResources();
        loadPlaylist();
        checkMusicEnvironment();

        game.addPlayer(aiFactory.new AIPlayer(engine, playerName, 0));
        for (int i = 1; i <= aiCount; i++) {
            game.addPlayer(aiFactory.new AIPlayer(engine, "AI-" + (i < 10 ? "0" + i : i), 2));
        }

        setupUI();
        setVisible(true);

        if (!playlist.isEmpty()) playTrack(0);
        startNewRound(0);
    }

    private void checkMusicEnvironment() {
        System.out.println("\n--- 🔍 音乐环境体检 ---");
        File folder = new File("resources/res");
        if (!folder.exists()) folder = new File("src/main/resources/res");
        if (folder.exists()) {
            System.out.println("✅ 成功定位文件夹: " + folder.getAbsolutePath());
        }
        System.out.println("--- 🏁 体检结束 ---\n");
    }

    private void loadPlaylist() {
        File folder = new File("resources/res");
        if (!folder.exists()) folder = new File("src/main/resources/res");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
            if (files != null && files.length > 0) {
                playlist.addAll(Arrays.asList(files));
                Collections.shuffle(playlist);
            }
        }
    }

    private void playTrack(int index) {
        if (playlist.isEmpty()) return;
        if (index < 0) index = playlist.size() - 1;
        if (index >= playlist.size()) index = 0;
        currentTrackIndex = index;
        try {
            if (musicClip != null) { musicClip.stop(); musicClip.close(); }
            File musicFile = playlist.get(currentTrackIndex);
            AudioInputStream stream = AudioSystem.getAudioInputStream(musicFile);
            musicClip = AudioSystem.getClip();
            musicClip.open(stream);
            if (nowPlayingLabel != null) nowPlayingLabel.setText("🎵 正在播放: " + musicFile.getName());
            musicClip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP && !isPaused) {
                    if (musicClip.getMicrosecondPosition() >= musicClip.getMicrosecondLength()) {
                        SwingUtilities.invokeLater(() -> playTrack(currentTrackIndex + 1));
                    }
                }
            });
            FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(-15.0f);
            musicClip.start();
            isPaused = false;
            if(ppBtn != null) ppBtn.setText("⏸");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupUI() {
        setTitle("🎲 大话骰逻辑全修版 - " + playerName);
        setSize(1100, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(30, 33, 39));
        setLayout(new BorderLayout(10, 10));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);
        northPanel.setPreferredSize(new Dimension(0, 100));

        statusLabel = new JLabel("游戏准备中...", JLabel.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 26));
        statusLabel.setForeground(new Color(97, 175, 239));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        JPanel playerContainer = new JPanel();
        playerContainer.setLayout(new BoxLayout(playerContainer, BoxLayout.Y_AXIS));
        playerContainer.setOpaque(false);
        playerContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        nowPlayingLabel = new JLabel("🎵 正在播放: ---", JLabel.CENTER);
        nowPlayingLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        nowPlayingLabel.setForeground(new Color(152, 195, 121));
        nowPlayingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel musicCtrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        musicCtrl.setOpaque(false);
        JButton prevBtn = new JButton("⏮");
        ppBtn = new JButton("⏸");
        JButton nextBtn = new JButton("⏭");

        for (JButton b : new JButton[]{prevBtn, ppBtn, nextBtn}) {
            b.setFont(new Font("SansSerif", Font.PLAIN, 30));
            b.setForeground(Color.WHITE);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            musicCtrl.add(b);
        }

        prevBtn.addActionListener(e -> playTrack(currentTrackIndex - 1));
        nextBtn.addActionListener(e -> playTrack(currentTrackIndex + 1));
        ppBtn.addActionListener(e -> {
            if (musicClip == null) return;
            if (musicClip.isRunning()) {
                clipTimePosition = musicClip.getMicrosecondPosition();
                musicClip.stop();
                isPaused = true;
                ppBtn.setText("▶");
            } else {
                musicClip.setMicrosecondPosition(clipTimePosition);
                musicClip.start();
                isPaused = false;
                ppBtn.setText("⏸");
            }
        });

        playerContainer.add(nowPlayingLabel);
        playerContainer.add(musicCtrl);
        northPanel.add(statusLabel);
        northPanel.add(playerContainer);
        add(northPanel, BorderLayout.NORTH);

        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setBackground(new Color(40, 44, 52));
        logArea.setForeground(new Color(171, 178, 191));
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        logArea.setMargin(new Insets(20, 20, 20, 20));
        StyledDocument doc = logArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        logArea.setParagraphAttributes(center, false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        add(logScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        dicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        dicePanel.setBackground(new Color(40, 44, 52));
        dicePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " 我的骰子 ", 0, 0, null, Color.WHITE));
        bottom.add(dicePanel, BorderLayout.NORTH);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 25));
        ctrl.setBackground(new Color(30, 33, 39));
        qtyCombo = new JComboBox<>();
        qtyCombo.setPreferredSize(new Dimension(100, 50));
        qtyCombo.setFont(new Font("Arial", Font.BOLD, 26));

        // 监听下拉框展开，刷新选项
        qtyCombo.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateQtyOptions();
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        faceCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        faceCombo.setPreferredSize(new Dimension(100, 50));
        faceCombo.setFont(new Font("Arial", Font.BOLD, 26));
        faceCombo.addActionListener(e -> {
            updateQtyOptions();
        });

        bidFeiBtn = new JButton(" 叫飞 ");
        styleBtn(bidFeiBtn, new Color(152, 195, 121), Color.BLACK);
        bidFeiBtn.addActionListener(e -> handleBid(false));
        bidZhaiBtn = new JButton(" 叫斋 ");
        styleBtn(bidZhaiBtn, new Color(97, 175, 239), Color.BLACK);
        bidZhaiBtn.addActionListener(e -> handleBid(true));
        openBtn = new JButton(" 开牌 ");
        styleBtn(openBtn, new Color(224, 108, 117), Color.WHITE);
        openBtn.addActionListener(e -> handleOpen());

        ctrl.add(new JLabel("<html><font color='white'>数量:</font></html>")); ctrl.add(qtyCombo);
        ctrl.add(new JLabel("<html><font color='white'>点数:</font></html>")); ctrl.add(faceCombo);
        ctrl.add(bidFeiBtn); ctrl.add(bidZhaiBtn); ctrl.add(openBtn);
        bottom.add(ctrl, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private Image loadOneImage(String fileName) {
        String[] paths = {"resources/res/" + fileName, "src/main/resources/res/" + fileName, "res/" + fileName};
        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) return new ImageIcon(f.getPath()).getImage();
        }
        URL url = getClass().getResource("/res/" + fileName);
        if (url != null) return new ImageIcon(url).getImage();
        return null;
    }

    private void loadResources() {
        try {
            Image iconImg = loadOneImage("logo.png");
            if (iconImg != null) {
                this.setIconImage(iconImg);
                if (Taskbar.isTaskbarSupported()) Taskbar.getTaskbar().setIconImage(iconImg);
            }
            for (int i = 1; i <= 6; i++) {
                Image diceImg = loadOneImage(i + ".jpg");
                if (diceImg == null) diceImg = loadOneImage(i + ".png");
                if (diceImg != null) diceIcons[i] = new ImageIcon(diceImg.getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateQtyOptions() {
        if (game.getCurrentPlayer() == null || !game.getCurrentPlayer().getName().equals(playerName)) return;

        int totalDiceInGame = game.getPlayers().size() * 5; // 动态上限：人数 * 5
        int[] curBid = game.getCurrentBid();
        int previousQty = (curBid != null) ? curBid[0] : 1;

        // 初始最小值通常从 玩家人数 开始（比如2人玩，最少叫2个）
        int startMin = Math.max(game.getPlayers().size(), previousQty - 1);

        Integer currentSelected = (Integer) qtyCombo.getSelectedItem();
        qtyCombo.removeAllItems();

        // 循环到总骰子数为止
        for (int i = startMin; i <= totalDiceInGame; i++) {
            qtyCombo.addItem(i);
        }

        if (currentSelected != null && currentSelected >= startMin && currentSelected <= totalDiceInGame) {
            qtyCombo.setSelectedItem(currentSelected);
        } else {
            qtyCombo.setSelectedIndex(0);
        }
    }

    private void handleBid(boolean isZhai) {
        int face = (int) faceCombo.getSelectedItem();
        if (face == 1) isZhai = true;

        int[] curBid = game.getCurrentBid();
        int n = game.getPlayers().size();

        // 获取当前下拉框选择的数量
        Object selected = qtyCombo.getSelectedItem();
        if (selected == null) return;
        int q = (int) selected;

        if (curBid != null) {
            int prevQty = curBid[0];
            int prevFace = curBid[1];
            boolean prevIsZhai = (curBid[2] == 1);

            if (!isZhai && prevIsZhai) {
                // --- 🔴 核心修正：斋变飞逻辑 ---
                // 规则：数量必须【等于】2倍
                int requiredQty = prevQty * 2;
                if (q != requiredQty) {
                    JOptionPane.showMessageDialog(this, "规则错误：斋变飞数量必须恰好等于 2 倍（即 " + requiredQty + " 个）");
                    // 自动纠正下拉框，方便玩家直接点击
                    qtyCombo.setSelectedItem(requiredQty);
                    return;
                }
            } else {
                // --- 其他常规校验（如：飞变斋、斋变斋等） ---
                int minAllowed = n;
                if (isZhai) {
                    if (!prevIsZhai) minAllowed = Math.max(n, prevQty - 1); // 飞变斋
                    else minAllowed = (face > prevFace) ? prevQty : prevQty + 1; // 斋变斋
                } else {
                    // 飞变飞
                    minAllowed = (face > prevFace) ? prevQty : prevQty + 1;
                }

                if (q < minAllowed) {
                    JOptionPane.showMessageDialog(this, "数量不足！当前操作至少需要叫 " + minAllowed + " 个");
                    return;
                }
            }
        }

        // 执行叫点
        if (game.placeBid(q, face, isZhai)) {
            log("▶ " + playerName + ": " + q + "个" + face + (isZhai ? " [斋]" : " [飞]") + "\n");
            checkTurn();
        }
    }

    // --- 🚨 修改点：增加退出确认对话框 ---
    private void showVisualResult(String textResult) {
        JDialog dialog = new JDialog(this, "开牌结算", true);
        dialog.getContentPane().setBackground(new Color(40, 44, 52));
        dialog.setUndecorated(true);
        ((JPanel)dialog.getContentPane()).setBorder(BorderFactory.createLineBorder(new Color(97, 175, 239), 3));
        dialog.setLayout(new BorderLayout());
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        int rowCount = 0;
        for (Dice_Player.Player p : game.getPlayers()) {
            gbc.gridy = rowCount++; gbc.insets = new Insets(10, 30, 10, 30);
            gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel nLbl = new JLabel(p.getName() + ": ");
            nLbl.setForeground(new Color(229, 192, 123)); nLbl.setFont(new Font("微软雅黑", Font.BOLD, 17));
            mainContent.add(nLbl, gbc);
            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JPanel dRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); dRow.setOpaque(false);
            for (Dice_Player.Dice d : p.getDice()) {
                JLabel iLbl = new JLabel("", JLabel.CENTER); iLbl.setPreferredSize(new Dimension(46, 46));
                iLbl.setOpaque(true); iLbl.setBackground(Color.WHITE);
                iLbl.setBorder(BorderFactory.createLineBorder(new Color(171, 178, 191), 1));
                if (diceIcons[d.getValue()] != null) {
                    iLbl.setIcon(new ImageIcon(diceIcons[d.getValue()].getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
                } else { iLbl.setText(String.valueOf(d.getValue())); iLbl.setFont(new Font("Arial", Font.BOLD, 20)); }
                dRow.add(iLbl);
            }
            mainContent.add(dRow, gbc);
        }
        JScrollPane sp = new JScrollPane(mainContent); sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setBorder(null);
        JPanel sPnl = new JPanel(); sPnl.setLayout(new BoxLayout(sPnl, BoxLayout.Y_AXIS)); sPnl.setOpaque(false);
        JLabel rLbl = new JLabel("<html><div style='text-align: center; color: #61afef; width: 450px;'>" + textResult.replaceAll("\n", "<br>") + "</div></html>", JLabel.CENTER);
        rLbl.setAlignmentX(Component.CENTER_ALIGNMENT); rLbl.setFont(new Font("微软雅黑", Font.BOLD, 16)); rLbl.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        JPanel bPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20)); bPnl.setOpaque(false);

        JButton nBtn = new JButton(" 继续游戏 "); styleBtn(nBtn, new Color(152, 195, 121), Color.BLACK);
        JButton qBtn = new JButton(" 不玩了 "); styleBtn(qBtn, new Color(224, 108, 117), Color.WHITE);

        nBtn.addActionListener(e -> dialog.dispose());

        // --- 修改部分：增加二次确认弹窗 ---
        qBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    dialog,
                    "确定要退出游戏吗？",
                    "退出确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
            // 如果选 NO，什么也不做，对话框关闭，回到结算界面
        });

        bPnl.add(nBtn); bPnl.add(qBtn); sPnl.add(rLbl); sPnl.add(bPnl);
        dialog.add(sp, BorderLayout.CENTER); dialog.add(sPnl, BorderLayout.SOUTH);
        dialog.setSize(750, 600); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private void handleOpen() {
        String res = game.challenge(); log("\n" + res + "\n"); showVisualResult(res); startNewRound(game.getLastLoserIndex());
    }

    private void updateDice() {
        dicePanel.removeAll();
        for (Dice_Player.Player p : game.getPlayers()) {
            if (p.getName().equals(playerName)) {
                for (Dice_Player.Dice d : p.getDice()) {
                    JLabel l = new JLabel("", JLabel.CENTER); l.setPreferredSize(new Dimension(65, 65));
                    l.setOpaque(true); l.setBackground(Color.WHITE); l.setBorder(BorderFactory.createLineBorder(new Color(97, 175, 239), 3));
                    if (diceIcons[d.getValue()] != null) l.setIcon(diceIcons[d.getValue()]);
                    else { l.setText(String.valueOf(d.getValue())); l.setFont(new Font("Arial", Font.BOLD, 28)); }
                    dicePanel.add(l);
                }
            }
        }
        dicePanel.revalidate(); dicePanel.repaint();
    }

    private void checkTurn() {
        Dice_Player.Player actor = game.getCurrentPlayer();
        if (actor.getName().equals(playerName)) {
            statusLabel.setText("🟢 你的回合");
            if (new Random().nextInt(100) < 20) log("🤖 AI 盯：「" + getTalk(TALK_PRESSURE, pressurePool) + "」\n");
            setUIEnabled(true);
            updateQtyOptions();
            openBtn.setEnabled(game.getCurrentBid() != null);
        } else {
            statusLabel.setText("🤖 " + actor.getName() + " 思考中..."); setUIEnabled(false); runAI();
        }
    }

    private void runAI() {
        new SwingWorker<int[], Void>() {
            final String aiName = game.getCurrentPlayer().getName();
            @Override protected int[] doInBackground() throws Exception {
                Thread.sleep(1500 + new Random().nextInt(1000));
                return ((AI_Logic.AIPlayer) game.getCurrentPlayer()).makeDecision(game.getCurrentBid(), game.getPlayers().size()*5);
            }
            @Override protected void done() {
                try {
                    int[] d = get();
                    if (d == null) {
                        log("💥 " + aiName + "：「" + getTalk(TALK_OPEN, openPool) + "」 开牌！\n");
                        handleOpen();
                    } else {
                        game.placeBid(d[0], d[1], d[2] == 1);
                        String content = "▶ " + aiName + ": " + d[0] + "个" + d[1] + (d[2]==1?" [斋]":" [飞]");
                        if (new Random().nextInt(100) < 30) content += " 💬 「" + getTalk(TALK_BID, bidPool) + "」";
                        log(content + "\n"); checkTurn();
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void startNewRound(int loserIdx) {
        game.startNewRound(loserIdx); roundCounter++; logArea.setText("");
        String line = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        log("\n" + line + String.format("第 %d 局 比赛\n", roundCounter) + line + "\n");
        updateDice(); checkTurn();
    }

    private void styleBtn(JButton b, Color bg, Color fg) {
        b.setPreferredSize(new Dimension(120, 50)); b.setBackground(bg); b.setForeground(fg);
        b.setOpaque(true); b.setBorderPainted(false); b.setFont(new Font("微软雅黑", Font.BOLD, 16)); b.setFocusPainted(false);
    }

    private void setUIEnabled(boolean b) {
        qtyCombo.setEnabled(b); faceCombo.setEnabled(b);
        bidFeiBtn.setEnabled(b);
        bidZhaiBtn.setEnabled(b); openBtn.setEnabled(b);
    }

    private String getTalk(String[] origin, ArrayList<String> pool) {
        if (pool.isEmpty()) { pool.addAll(Arrays.asList(origin)); Collections.shuffle(pool); }
        return pool.remove(0);
    }

    private void initSettings() {
        String n = JOptionPane.showInputDialog(null, "输入名字:", "角色", JOptionPane.QUESTION_MESSAGE);
        if (n != null && !n.isEmpty()) playerName = n;
        String c = JOptionPane.showInputDialog(null, "AI 数量:", "设置", JOptionPane.QUESTION_MESSAGE);
        try { if (c != null) aiCount = Math.max(1, Integer.parseInt(c)); } catch (Exception e) { aiCount = 1; }
    }

    private void log(String msg) {
        try { Document doc = logArea.getDocument(); doc.insertString(doc.getLength(), msg, null); logArea.setCaretPosition(doc.getLength()); } catch (Exception e) {}
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Game_GUI::new); }
}