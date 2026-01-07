import java.util.*;

public class Game_Logic {
    private List<Dice_Player.Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int[] currentBid;
    private int lastLoserIndex = 0;

    public void addPlayer(Dice_Player.Player p) { players.add(p); }

    public void startNewRound(int startIndex) {
        for (Dice_Player.Player p : players) p.rollDice();
        currentBid = null;
        currentPlayerIndex = startIndex % players.size();
    }

    private boolean isStraight(List<Dice_Player.Dice> diceList) {
        if (diceList.size() < 5) return false;
        int[] vals = new int[5];
        for (int i = 0; i < 5; i++) vals[i] = diceList.get(i).getValue();
        Arrays.sort(vals);
        for (int i = 0; i < vals.length - 1; i++) {
            if (vals[i] == vals[i+1]) return false;
        }
        return true;
    }

    public boolean placeBid(int qty, int face, boolean isZhai) {
        if (face == 1) isZhai = true;
        if (face < 1 || face > 6) return false;

        int n = players.size();

        if (currentBid == null) {
            // ✨ 核心修正：起叫判定逻辑
            // 斋局（含1点）底线是 n，飞局底线是 n + 1
            int minStart = isZhai ? n : (n + 1);
            if (qty < minStart) return false;
        } else {
            int oldQty = currentBid[0];
            int oldFace = currentBid[1];
            boolean oldZhai = (currentBid[2] == 1);

            if (!oldZhai && isZhai) {
                // 飞转斋：允许减 1（qty >= oldQty - 1 且不低于人头数）
                if (qty < oldQty - 1 || qty < n) return false;
            } else if (oldZhai && !isZhai) {
                // 斋转飞：必须翻倍
                if (qty < oldQty * 2) return false;
            } else {
                if (face == oldFace && qty <= oldQty) return false;
                if (face < oldFace && qty <= oldQty) return false;
                if (face > oldFace && qty < oldQty) return false;
            }
        }

        currentBid = new int[]{qty, face, isZhai ? 1 : 0};
        currentPlayerIndex = (currentPlayerIndex + 1) % n;
        return true;
    }

    public String challenge() {
        if (currentBid == null) return "没人叫号";
        int bidQty = currentBid[0];
        int bidFace = currentBid[1];
        boolean isZhai = (currentBid[2] == 1);
        int total = 0;

        StringBuilder sb = new StringBuilder("🎲 --- 结算展示 ---\n");
        for (Dice_Player.Player p : players) {
            sb.append(String.format("%-8s: ", p.getName()));
            boolean straight = isStraight(p.getDice());
            int count = 0;
            for (Dice_Player.Dice d : p.getDice()) {
                int v = d.getValue();
                sb.append("[").append(v).append("] ");
                if (!straight) {
                    if (v == bidFace || (!isZhai && v == 1)) count++;
                }
            }
            sb.append(straight ? " (顺子自爆)" : " (计:" + count + ")");
            if (!straight) total += count;
            sb.append("\n");
        }

        int bidderIdx = (currentPlayerIndex - 1 + players.size()) % players.size();
        boolean win = total >= bidQty;
        lastLoserIndex = win ? currentPlayerIndex : bidderIdx;

        sb.append("--------------------\n");
        sb.append("结果: 共 ").append(total).append(" 个 ").append(bidFace).append("\n");
        sb.append("输家: ").append(players.get(lastLoserIndex).getName());
        return sb.toString();
    }

    public List<Dice_Player.Player> getPlayers() { return players; }
    public int[] getCurrentBid() { return currentBid; }
    public int getLastLoserIndex() { return lastLoserIndex; }
    public Dice_Player.Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
}