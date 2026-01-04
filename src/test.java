import java.util.List;

public class test {
    public static void main(String[] args) {
        // 1. 创建外部类实例
        Dice_Player outer = new Dice_Player();

        // 2. 创建一个具体的玩家（这里使用匿名内部类来实现抽象方法）
        Dice_Player.Player player = outer.new Player("张三") {
            @Override
            public int[] makeDecision(int[] currentBid, int totalDiceCount) {
                // 简单的测试逻辑：总是比上家多喊一个
                if (currentBid == null) return new int[]{2, 1}; // 初始叫 2个1
                return new int[]{currentBid[0] + 1, currentBid[1]};
            }
        };

        System.out.println("玩家 " + player.getName() + " 已就绪。");

        // 3. 测试摇骰子
        System.out.println("--- 正在摇骰子 ---");
        player.rollDice();

        // 4. 打印骰子点数
        List<Dice_Player.Dice> hand = player.getDice();
        System.out.print("手牌点数：");
        for (Dice_Player.Dice d : hand) {
            System.out.print("[" + d.getValue() + "] ");
        }
        System.out.println();

        // 5. 测试决策逻辑
        int[] myBid = player.makeDecision(new int[]{3, 4}, 10);
        System.out.println("上家叫 3个4，" + player.getName() + " 叫: " + myBid[0] + "个" + myBid[1]);
    }
}