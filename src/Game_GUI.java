import javax.swing.*;
import javax.swing.text.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

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

    // --- 🎭 垃圾话系统数据库 (完整保留) ---
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

        // 自动体检
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

    // --- 🔍 音乐体检工具 (针对 resources/res 路径) ---
    private void checkMusicEnvironment() {
        System.out.println("\n--- 🔍 音乐环境体检 (resources/res) ---");
        // 尝试两种常见的 resources 路径表示
        File folder = new File("resources/res");
        if (!folder.exists()) {
            folder = new File("src/main/resources/res"); // 适配 Maven 结构
        }

        if (!folder.exists()) {
            System.err.println("❌ 仍然找不到文件夹！");
            System.err.println("   尝试路径: " + folder.getAbsolutePath());
        } else {
            System.out.println("✅ 成功定位文件夹: " + folder.getAbsolutePath());
            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                System.err.println("❌ 文件夹是空的！");
            } else {
                for (File f : files) {
                    if (f.getName().toLowerCase().endsWith(".wav"))
                        System.out.println("🎵 发现音乐: " + f.getName());
                    else if (f.getName().toLowerCase().endsWith(".mp3"))
                        System.err.println("⚠️ 发现 MP3 (Java不支持): " + f.getName());
                }
            }
        }
        System.out.println("--- 🏁 体检结束 ---\n");
    }

    // --- 🎼 音乐控制逻辑 ---
    private void loadPlaylist() {
        // 优先找 resources/res
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
            if (musicClip != null) {
                musicClip.stop();
                musicClip.close();
            }
            AudioInputStream stream = AudioSystem.getAudioInputStream(playlist.get(currentTrackIndex));
            musicClip = AudioSystem.getClip();
            musicClip.open(stream);

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

    private String getTalk(String[] origin, ArrayList<String> pool) {
        if (pool.isEmpty()) {
            pool.addAll(Arrays.asList(origin));
            Collections.shuffle(pool);
        }
        return pool.remove(0);
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
                if (diceImg != null) {
                    diceIcons[i] = new ImageIcon(diceImg.getScaledInstance(60, 60, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Image loadOneImage(String fileName) {
        // 尝试多种路径，确保能读到 resources/res
        String[] paths = {
                "resources/res/" + fileName,
                "src/main/resources/res/" + fileName,
                "res/" + fileName
        };
        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) return new ImageIcon(f.getPath()).getImage();
        }
        // 尝试类加载器
        URL url = getClass().getResource("/res/" + fileName);
        if (url != null) return new ImageIcon(url).getImage();
        return null;
    }

    private void setupUI() {
        setTitle("🎲 大话骰竞技场 - " + playerName);
        setSize(1100, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(30, 33, 39));
        setLayout(new BorderLayout(10, 10));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);
        northPanel.setPreferredSize(new Dimension(0, 140));

        statusLabel = new JLabel("游戏准备中...", JLabel.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 26));
        statusLabel.setForeground(new Color(97, 175, 239));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JPanel musicCtrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        musicCtrl.setOpaque(false);

        JButton prevBtn = new JButton("⏮");
        ppBtn = new JButton("⏸");
        JButton nextBtn = new JButton("⏭");

        for (JButton b : new JButton[]{prevBtn, ppBtn, nextBtn}) {
            b.setFont(new Font("SansSerif", Font.PLAIN, 32));
            b.setForeground(Color.WHITE);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
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

        northPanel.add(statusLabel);
        northPanel.add(musicCtrl);
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

        faceCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        faceCombo.setPreferredSize(new Dimension(100, 50));
        faceCombo.setFont(new Font("Arial", Font.BOLD, 26));
        faceCombo.addActionListener(e -> {
            boolean isOne = (int)faceCombo.getSelectedItem() == 1;
            if(bidFeiBtn != null) bidFeiBtn.setEnabled(!isOne);
            updateQtyOptions(false);
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
            gbc.gridy = rowCount++;
            gbc.insets = new Insets(10, 30, 10, 30);
            gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel nameLbl = new JLabel(p.getName() + ": ");
            nameLbl.setForeground(new Color(229, 192, 123));
            nameLbl.setFont(new Font("微软雅黑", Font.BOLD, 17));
            mainContent.add(nameLbl, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JPanel diceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            diceRow.setOpaque(false);
            for (Dice_Player.Dice d : p.getDice()) {
                JLabel imgLbl = new JLabel("", JLabel.CENTER);
                imgLbl.setPreferredSize(new Dimension(46, 46));
                imgLbl.setOpaque(true); imgLbl.setBackground(Color.WHITE);
                imgLbl.setBorder(BorderFactory.createLineBorder(new Color(171, 178, 191), 1));
                if (diceIcons[d.getValue()] != null) {
                    Image small = diceIcons[d.getValue()].getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    imgLbl.setIcon(new ImageIcon(small));
                } else {
                    imgLbl.setText(String.valueOf(d.getValue()));
                    imgLbl.setFont(new Font("Arial", Font.BOLD, 20));
                }
                diceRow.add(imgLbl);
            }
            mainContent.add(diceRow, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setOpaque(false); scrollPane.getViewport().setOpaque(false); scrollPane.setBorder(null);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setOpaque(false);

        JLabel resLbl = new JLabel("<html><div style='text-align: center; color: #61afef; width: 450px;'>" +
                textResult.replaceAll("\n", "<br>") + "</div></html>", JLabel.CENTER);
        resLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        resLbl.setFont(new Font("微软雅黑", Font.BOLD, 16));
        resLbl.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        btnPanel.setOpaque(false);
        JButton nextBtn = new JButton(" 继续游戏 "); styleBtn(nextBtn, new Color(152, 195, 121), Color.BLACK);
        JButton quitBtn = new JButton(" 不玩了 "); styleBtn(quitBtn, new Color(224, 108, 117), Color.WHITE);
        nextBtn.addActionListener(e -> dialog.dispose());
        quitBtn.addActionListener(e -> System.exit(0));
        btnPanel.add(nextBtn); btnPanel.add(quitBtn);
        southPanel.add(resLbl); southPanel.add(btnPanel);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(southPanel, BorderLayout.SOUTH);
        dialog.setSize(750, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateQtyOptions(boolean isZhaiIntent) {
        if (game.getCurrentPlayer() == null || !game.getCurrentPlayer().getName().equals(playerName)) return;
        qtyCombo.removeAllItems();
        int n = game.getPlayers().size();
        int[] cur = game.getCurrentBid();
        int face = (int) faceCombo.getSelectedItem();
        if (face == 1) isZhaiIntent = true;
        int startMin;
        if (cur == null) {
            startMin = isZhaiIntent ? n : (n + 1);
        } else {
            int curQty = cur[0], curFace = cur[1];
            boolean curZhai = (cur[2] == 1);
            if (!curZhai && isZhaiIntent) startMin = Math.max(n, curQty - 1);
            else if (curZhai && !isZhaiIntent) startMin = curQty * 2;
            else { startMin = (face > curFace) ? curQty : curQty + 1; }
        }
        for (int i = startMin; i <= n * 5; i++) qtyCombo.addItem(i);
    }

    private void handleBid(boolean isZhai) {
        if (qtyCombo.getSelectedItem() == null) return;
        int q = (int) qtyCombo.getSelectedItem();
        int f = (int) faceCombo.getSelectedItem();
        if (f == 1) isZhai = true;
        if (game.placeBid(q, f, isZhai)) {
            log("▶ " + playerName + ": " + q + "个" + f + (isZhai ? " [斋]" : " [飞]") + "\n");
            checkTurn();
        }
    }

    private void handleOpen() {
        String res = game.challenge();
        log("\n" + res + "\n");
        showVisualResult(res);
        startNewRound(game.getLastLoserIndex());
    }

    private void updateDice() {
        dicePanel.removeAll();
        for (Dice_Player.Dice d : game.getPlayers().get(0).getDice()) {
            int val = d.getValue();
            JLabel l = new JLabel("", JLabel.CENTER);
            l.setPreferredSize(new Dimension(65, 65));
            l.setOpaque(true); l.setBackground(Color.WHITE);
            l.setBorder(BorderFactory.createLineBorder(new Color(97, 175, 239), 3));
            if (diceIcons[val] != null) l.setIcon(diceIcons[val]);
            else { l.setText(String.valueOf(val)); l.setFont(new Font("Arial", Font.BOLD, 28)); }
            dicePanel.add(l);
        }
        dicePanel.revalidate(); dicePanel.repaint();
    }

    private void checkTurn() {
        Dice_Player.Player actor = game.getCurrentPlayer();
        if (actor.getName().equals(playerName)) {
            statusLabel.setText("🟢 你的回合");
            if (new Random().nextInt(100) < 20) {
                log("🤖 AI 盯着你幽幽地说：「" + getTalk(TALK_PRESSURE, pressurePool) + "」\n");
            }
            setUIEnabled(true); updateQtyOptions(false);
            openBtn.setEnabled(game.getCurrentBid() != null);
        } else {
            statusLabel.setText("🤖 " + actor.getName() + " 思考中...");
            setUIEnabled(false); runAI();
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
                        log("💥 " + aiName + " 拍案而起：「" + getTalk(TALK_OPEN, openPool) + "」 开牌！\n");
                        handleOpen();
                    } else {
                        game.placeBid(d[0], d[1], d[2] == 1);
                        String content = "▶ " + aiName + ": " + d[0] + "个" + d[1] + (d[2]==1?" [斋]":" [飞]");
                        if (new Random().nextInt(100) < 30) content += "  💬 「" + getTalk(TALK_BID, bidPool) + "」";
                        log(content + "\n");
                        checkTurn();
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void startNewRound(int loserIdx) {
        game.startNewRound(loserIdx);
        roundCounter++;
        logArea.setText("");
        String line = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        String title = String.format("第 %d 局 比赛\n", roundCounter);
        log("\n" + line + title + line + "\n");
        updateDice();
        checkTurn();
    }

    private void styleBtn(JButton b, Color bg, Color fg) {
        b.setPreferredSize(new Dimension(120, 50));
        b.setBackground(bg); b.setForeground(fg);
        b.setOpaque(true); b.setBorderPainted(false);
        b.setFont(new Font("微软雅黑", Font.BOLD, 16));
        b.setFocusPainted(false);
    }

    private void setUIEnabled(boolean b) {
        qtyCombo.setEnabled(b); faceCombo.setEnabled(b);
        bidFeiBtn.setEnabled(b && (int)faceCombo.getSelectedItem() != 1);
        bidZhaiBtn.setEnabled(b); openBtn.setEnabled(b);
    }

    private void initSettings() {
        String n = JOptionPane.showInputDialog(null, "输入名字:", "角色", JOptionPane.QUESTION_MESSAGE);
        if (n != null && !n.isEmpty()) playerName = n;
        String c = JOptionPane.showInputDialog(null, "AI 数量:", "设置", JOptionPane.QUESTION_MESSAGE);
        try { if (c != null) aiCount = Math.max(1, Integer.parseInt(c)); } catch (Exception e) { aiCount = 1; }
    }

    private void log(String msg) {
        try {
            Document doc = logArea.getDocument();
            doc.insertString(doc.getLength(), msg, null);
            logArea.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Game_GUI::new); }
}