public class test {
    public static void main(String[] args) throws InterruptedException {
        Dice_Player game = new Dice_Player();
        AI_Logic logic = new AI_Logic();

        // 创建两个不同风格或难度的 AI
        AI_Logic.AIPlayer p1 = logic.new AIPlayer(game, "AI-阿强", 2);
        AI_Logic.AIPlayer p2 = logic.new AIPlayer(game, "AI-阿珍", 2);

        int totalGames = 100;

        for (int gameNum = 1; gameNum <= totalGames; gameNum++) {
            System.out.println("\n" + "=".repeat(20) + " 第 " + gameNum + " 局开始 " + "=".repeat(20));

            p1.rollDice();
            p2.rollDice();

            // 打印手牌，方便你判断 AI 是否在吹牛
            System.out.println(p1.getName() + " 手牌: " + getHand(p1));
            System.out.println(p2.getName() + " 手牌: " + getHand(p2));
            System.out.println("-".repeat(50));

            int[] currentBid = null;
            AI_Logic.AIPlayer attacker = p1;
            int totalDice = 10;
            int round = 1;

            while (true) {
                int[] decision = attacker.makeDecision(currentBid, totalDice);

                if (decision == null) {
                    System.out.println("\n🔥 [" + attacker.getName() + "] 拍桌子喊道：\"开牌！！\"");
                    // 这里可以加逻辑计算谁赢了
                    break;
                }

                // 逻辑变化标注
                String tags = "";
                if (currentBid != null) {
                    // 1. 检测跳叫
                    if (decision[0] - currentBid[0] > 1) tags += " [🚀跳叫+" + (decision[0] - currentBid[0]) + "]";
                    // 2. 检测换点
                    if (decision[1] != currentBid[1]) tags += " [🎲换点" + currentBid[1] + "->" + decision[1] + "]";
                    // 3. 检测斋飞转换
                    if (currentBid[2] == 0 && decision[2] == 1) tags += " [★飞变斋]";
                    if (currentBid[2] == 1 && decision[2] == 0) tags += " [💥斋变飞x2]";
                }

                System.out.printf("回合 %-2d | %-5s 叫号: %d个%d %-2s %s\n",
                        round++, attacker.getName(), decision[0], decision[1],
                        (decision[2] == 1 ? "斋" : "飞"), tags);

                currentBid = decision;
                attacker = (attacker == p1) ? p2 : p1; // 交换攻守方

                Thread.sleep(800); // <-- 这里的 800 毫秒让你有时间阅读每一行
            }

            System.out.println("本局结束，准备进入下一局...");
            Thread.sleep(2000); // 局与局之间停顿 2 秒
        }
    }

    private static String getHand(AI_Logic.AIPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (Dice_Player.Dice d : p.getDice()) {
            sb.append("[").append(d.getValue()).append("] ");
        }
        return sb.toString();
    }
}