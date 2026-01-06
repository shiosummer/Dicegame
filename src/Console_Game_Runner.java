import java.util.Scanner;
import java.util.Random;

public class Console_Game_Runner {
    private static Scanner scanner = new Scanner(System.in);
    private static Random rand = new Random();

    public static void main(String[] args) throws InterruptedException {
        Dice_Player engine = new Dice_Player();
        AI_Logic aiFactory = new AI_Logic();
        Game_Logic game = new Game_Logic();

        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║        🎲 大话骰 (Liar's Dice) 🎲       ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println("🎲 正在准备骰子和桌台...");
        System.out.print("➤ 输入你的名字: ");
        String playerName = scanner.nextLine();
        if (playerName.trim().isEmpty()) playerName = "玩家(你)";

        System.out.print("➤ 陪玩 AI 数量: ");
        int aiCount = scanner.hasNextInt() ? scanner.nextInt() : 1;
        scanner.nextLine();

        game.addPlayer(aiFactory.new AIPlayer(engine, playerName, 0));
        for (int i = 1; i <= aiCount; i++) {
            game.addPlayer(aiFactory.new AIPlayer(engine, "AI-小" + i, 2));
        }

        int nextStarterIndex = 0;
        int totalDice = (aiCount + 1) * 5;
        int roundCount = 1;

        while (true) {
            System.out.println("\n" + "🏮".repeat(5) + " 第 " + (roundCount++) + " 局 " + "🏮".repeat(5));
            game.startNewRound(nextStarterIndex);

            while (true) {
                Dice_Player.Player actor = game.getCurrentPlayer();
                int[] decision = null;

                if (actor.getName().equals(playerName)) {
                    System.out.print("\n🙈 你的骰子: ");
                    for (Dice_Player.Dice d : actor.getDice()) System.out.print("[" + d.getValue() + "] ");
                    System.out.println();
                    decision = getHumanDecision(game.getCurrentBid(), (aiCount + 1));
                } else {
                    // --- 模拟 AI 5-10秒随机思考 ---
                    int thinkTime = rand.nextInt(5001);
                    System.out.print("🤖 " + actor.getName() + " 正在盯着你的眼睛看");

                    long start = System.currentTimeMillis();
                    int dotCount = 0;
                    while (System.currentTimeMillis() - start < thinkTime) {
                        Thread.sleep(700);
                        System.out.print(".");
                        dotCount++;
                        if (dotCount > 5) {
                            System.out.print("\b\b\b\b\b\b      \b\b\b\b\b\b");
                            dotCount = 0;
                        }
                    }
                    System.out.println(" 决定了！");

                    decision = ((AI_Logic.AIPlayer) actor).makeDecision(game.getCurrentBid(), totalDice);
                }

                if (decision == null) {
                    System.out.println("\n💥 [" + actor.getName() + "] 猛拍桌子：\"开牌！！\"");
                    System.out.println(game.challenge());
                    nextStarterIndex = game.getLastLoserIndex();
                    break;
                } else {
                    // 修正逻辑：如果叫的是1，强制设为斋
                    if (decision[1] == 1) decision[2] = 1;

                    boolean ok = game.placeBid(decision[0], decision[1], decision[2] == 1);
                    if (ok) {
                        System.out.printf("▶ %s: %d个%d %s\n",
                                actor.getName(), decision[0], decision[1], (decision[2] == 1 ? "斋" : "飞"));
                    } else if (actor.getName().equals(playerName)) {
                        System.out.println("❌ 叫号不合法！数量必须更多，或点数更大。");
                    }
                }
            }

            System.out.print("\n继续游戏? (y/n): ");
            if (!scanner.next().equalsIgnoreCase("y")) break;
        }
    }

    private static int[] getHumanDecision(int[] currentBid, int minLimit) {
        while (true) {
            System.out.println("----------------------------------------");
            if (currentBid != null) {
                System.out.printf("当前场面: %d个%d %s\n", currentBid[0], currentBid[1], (currentBid[2] == 1 ? "斋" : "飞"));
                System.out.print("➤ 输入 'Q' 开牌，或 '数量 点数 斋(1/0)': ");
            } else {
                System.out.printf("➤ 你是首叫(数量需 > %d)，请输入 '数量 点数 斋(1/0)': ", minLimit);
            }

            String input = scanner.next();
            if (input.equalsIgnoreCase("Q")) return null;

            try {
                int q = Integer.parseInt(input);
                int f = scanner.nextInt();
                int z = scanner.nextInt();
                if (f == 1) z = 1; // 强制叫1必斋
                return new int[]{q, f, z};
            } catch (Exception e) {
                System.out.println("⚠️ 输入错误！示例: '5 6 0' (5个6飞) 或 'Q'。");
                scanner.nextLine();
            }
        }
    }
}