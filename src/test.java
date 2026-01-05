public class test {
    public static void main(String[] args) {
        // 1. 实例化最外层的环境类
        Dice_Player gameContext = new Dice_Player();
        AI_Logic aiLogicContext = new AI_Logic();

        // 2. 创建一个高级 AI 玩家 (Difficulty = 2)
        // 注意：内部类的实例化语法为 aiLogicContext.new AIPlayer(...)
        AI_Logic.AIPlayer bot = aiLogicContext.new AIPlayer(gameContext, "智脑阿强", 2);

        System.out.println("=== 游戏模拟开始 ===");
        System.out.println("玩家名称: " + bot.getName());

        // 3. 模拟摇骰子
        bot.rollDice();
        System.out.print("AI 手里的骰子: ");
        for (Dice_Player.Dice d : bot.getDice()) {
            System.out.print("[" + d.getValue() + "] ");
        }
        System.out.println("\n----------------------------");

        // 4. 测试场景 A：AI 作为先手叫点 (currentBid 为 null)
        // 假设全场共有 2 个玩家，总骰子数为 10
        int totalDice = 10;
        int[] firstBid = bot.makeDecision(null, totalDice);
        System.out.println("场景 A (AI先手) -> AI 叫点: " +
                firstBid[0] + "个" + firstBid[1] + (firstBid[2] == 1 ? " (斋)" : ""));

        // 5. 测试场景 B：上家叫了一个非常夸张的点数 (测试概率判定)
        // 叫 8 个 6（一共才 10 个骰子，这显然是吹牛）
        int[] highBid = {8, 6, 0};
        int[] reaction = bot.makeDecision(highBid, totalDice);

        System.out.print("场景 B (上家叫 8个6) -> AI 反应: ");
        if (reaction == null) {
            System.out.println("“我不信！开你！” (AI 返回 null)");
        } else {
            System.out.println("“我跟！” AI 叫: " + reaction[0] + "个" + reaction[1]);
        }

        // 6. 测试场景 C：上家叫了一个合理的点数
        int[] normalBid = {3, 4, 0};
        int[] followBid = bot.makeDecision(normalBid, totalDice);
        System.out.println("场景 C (上家叫 3个4) -> AI 反应: AI 叫: " +
                followBid[0] + "个" + followBid[1]);
    }
}