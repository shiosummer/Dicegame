import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

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

    /**
     * 判断是否为顺子：将骰子排序后检查是否连续或满足无重复的5个不同数字
     */
    private boolean isStraight(List<Dice_Player.Dice> diceList) {
        if (diceList.size() < 5) return false;
        int[] vals = new int[5];
        for (int i = 0; i < 5; i++) vals[i] = diceList.get(i).getValue();
        Arrays.sort(vals);

        // 检查是否有重复（顺子不能有重复数字）
        for (int i = 0; i < vals.length - 1; i++) {
            if (vals[i] == vals[i+1]) return false;
        }
        // 因为已经排序且无重复，如果最大减最小等于4，则是连续顺子(如12345或23456)
        // 在某些地方规则中，只要5个数字各不相同即算顺子，此处采用“无重复即顺子”的通用逻辑
        return true;
    }

    public boolean placeBid(int qty, int face, boolean isZhai) {
        if (face == 1) isZhai = true;
        if (currentBid == null) {
            if (qty <= players.size()) return false;
        } else {
            int oldQty = currentBid[0];
            int oldFace = currentBid[1];
            boolean oldZhai = (currentBid[2] == 1);
            if (isZhai == oldZhai) {
                if (!(qty > oldQty || (qty == oldQty && face > oldFace))) return false;
            }
        }
        currentBid = new int[]{qty, face, isZhai ? 1 : 0};
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return true;
    }

    public String challenge() {
        if (currentBid == null) return "还没有人叫号！";
        int bidQty = currentBid[0];
        int bidFace = currentBid[1];
        boolean zhai = (currentBid[2] == 1);
        int total = 0;

        StringBuilder details = new StringBuilder("\n📊 【全场开号统计】\n");
        details.append("----------------------------------------\n");

        for (Dice_Player.Player p : players) {
            details.append(String.format("%-10s: ", p.getName()));
            boolean straight = isStraight(p.getDice());
            int countInHand = 0;

            for (Dice_Player.Dice d : p.getDice()) {
                int val = d.getValue();
                details.append("[").append(val).append("] ");
                // 如果是顺子，该玩家贡献为 0，不进入统计
                if (!straight) {
                    if (val == bidFace || (!zhai && val == 1)) {
                        countInHand++;
                    }
                }
            }

            if (straight) {
                details.append(" 🚫(顺子自爆，计0个)");
            } else {
                details.append(" (贡献: ").append(countInHand).append("个)");
                total += countInHand;
            }
            details.append("\n");
        }
        details.append("----------------------------------------\n");

        int bidderIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
        int challengerIndex = currentPlayerIndex;

        String summary;
        if (total >= bidQty) {
            lastLoserIndex = challengerIndex;
            summary = String.format("🔥 场上共有 %d 个 %d%s\n✅ 目标 %d 个，叫号者 [%s] 赢了！",
                    total, bidFace, (zhai ? "(斋)" : ""), bidQty, players.get(bidderIndex).getName());
        } else {
            lastLoserIndex = bidderIndex;
            summary = String.format("💨 场上只有 %d 个 %d%s\n❌ 目标 %d 个，叫号者 [%s] 吹牛！",
                    total, bidFace, (zhai ? "(斋)" : ""), bidQty, players.get(bidderIndex).getName());
        }

        return details.toString() + summary + "\n💀 输家是: " + players.get(lastLoserIndex).getName();
    }

    public int getLastLoserIndex() { return lastLoserIndex; }
    public Dice_Player.Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public int[] getCurrentBid() { return currentBid; }
}