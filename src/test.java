import java.util.List;

public class test {
    public static void main(String[] args) throws InterruptedException {
        Dice_Player game = new Dice_Player();
        AI_Logic logic = new AI_Logic();

        AI_Logic.AIPlayer p1 = logic.new AIPlayer(game, "AI-阿强", 2);
        AI_Logic.AIPlayer p2 = logic.new AIPlayer(game, "AI-阿珍", 2);

        // --- 统计变量 ---
        int p1Wins = 0, p2Wins = 0;
        int totalDecisions = 0;
        int jumpCount = 0;
        int faceChangeCount = 0;
        int ruleChangeCount = 0;
        int ruleAndFaceBugCount = 0; // Bug检测：斋飞变且点数变

        int totalGames = 100; // 你可以根据需要调大到 10000

        for (int gameNum = 1; gameNum <= totalGames; gameNum++) {
            System.out.println("\n" + "==".repeat(10) + " 第 " + gameNum + " 局 " + "==".repeat(10));

            p1.rollDice();
            p2.rollDice();

            int[] lastBid = null; // 记录上一次的叫号
            AI_Logic.AIPlayer currentAttacker = p1;
            AI_Logic.AIPlayer lastBidder = null;
            int totalDiceCount = 10;
            int round = 1;

            while (true) {
                int[] decision = currentAttacker.makeDecision(lastBid, totalDiceCount);

                // --- 情况一：开牌 ---
                if (decision == null) {
                    System.out.println("\n🔥 [" + currentAttacker.getName() + "] 拍桌子大喊：\"开牌！！\"");

                    // 计算结果
                    int targetFace = lastBid[1];
                    int targetQty = lastBid[0];
                    boolean isZhai = (lastBid[2] == 1);

                    // 统计场上实际点数
                    int actualCount = countDice(p1, p2, targetFace, isZhai);
                    System.out.printf("📊 结果核对：叫号 [%d个%d %s] | 场上实际有: %d个\n",
                            targetQty, targetFace, (isZhai ? "斋" : "飞"), actualCount);

                    if (actualCount >= targetQty) {
                        System.out.println("🚩 结果：点数够了！[" + currentAttacker.getName() + "] 挑战失败，[" + lastBidder.getName() + "] 获胜！");
                        if (lastBidder == p1) p1Wins++; else p2Wins++;
                    } else {
                        System.out.println("🚩 结果：点数不够！[" + lastBidder.getName() + "] 吹牛被抓，[" + currentAttacker.getName() + "] 获胜！");
                        if (currentAttacker == p1) p1Wins++; else p2Wins++;
                    }
                    break;
                }

                // --- 情况二：叫号 ---
                totalDecisions++;
                String tags = "";
                if (lastBid != null) {
                    boolean isJump = (decision[0] - lastBid[0] > 1);
                    boolean isFaceChanged = (decision[1] != lastBid[1]);
                    boolean isRuleChanged = (decision[2] != lastBid[2]);

                    if (isJump) { tags += " [🚀跳叫+" + (decision[0] - lastBid[0]) + "]"; jumpCount++; }
                    if (isFaceChanged) { tags += " [🎲换点" + lastBid[1] + "->" + decision[1] + "]"; faceChangeCount++; }
                    if (isRuleChanged) {
                        tags += (decision[2] == 1) ? " [★飞变斋]" : " [💥斋变飞x2]";
                        ruleChangeCount++;
                        // Bug检测：斋飞变的同时点数也变了
                        if (isFaceChanged) ruleAndFaceBugCount++;
                    }
                }

                System.out.printf("回合 %-2d | %-5s 叫号: %d个%d %-2s %s\n",
                        round++, currentAttacker.getName(), decision[0], decision[1],
                        (decision[2] == 1 ? "斋" : "飞"), tags);

                lastBid = decision;
                lastBidder = currentAttacker;
                currentAttacker = (currentAttacker == p1) ? p2 : p1;

                if (totalGames <= 100) Thread.sleep(600);
            }
        }

        // --- 最终大数据总结 ---
        System.out.println("\n" + "=".repeat(15) + " 测试总结与 Bug 检测 " + "=".repeat(15));
        System.out.printf("总局数: %d | 阿强胜率: %.2f%% | 阿珍胜率: %.2f%%\n",
                totalGames, (p1Wins * 100.0 / totalGames), (p2Wins * 100.0 / totalGames));
        System.out.println("----------------------------------------------");
        System.out.printf("跳叫次数: %d | 换点次数: %d | 斋飞跳变: %d\n", jumpCount, faceChangeCount, ruleChangeCount);

        System.out.print("🔎 逻辑合规检测: ");
        if (ruleAndFaceBugCount == 0) {
            System.out.println("✅ 完美！斋飞变化时点数始终保持一致。");
        } else {
            System.out.println("❌ 警告！发现 " + ruleAndFaceBugCount + " 次在斋飞转换时点数发生了非预期跳变。");
        }
        System.out.println("=".repeat(50));
    }

    // 核心统计逻辑
    private static int countDice(AI_Logic.AIPlayer p1, AI_Logic.AIPlayer p2, int face, boolean isZhai) {
        int count = 0;
        count += countSinglePlayer(p1, face, isZhai);
        count += countSinglePlayer(p2, face, isZhai);
        return count;
    }

    private static int countSinglePlayer(AI_Logic.AIPlayer p, int face, boolean isZhai) {
        int count = 0;
        for (Dice_Player.Dice d : p.getDice()) {
            int val = d.getValue();
            if (val == face) {
                count++;
            } else if (!isZhai && val == 1) {
                // 如果不是斋，1点可以当成任何点数
                count++;
            }
        }
        return count;
    }

    private static String getHand(AI_Logic.AIPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (Dice_Player.Dice d : p.getDice()) {
            sb.append("[").append(d.getValue()).append("] ");
        }
        return sb.toString();
    }
}