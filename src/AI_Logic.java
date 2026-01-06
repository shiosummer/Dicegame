import java.util.*;

public class AI_Logic {
    // 核心修复：确保 AIPlayer 继承自 Dice_Player.Player
    public class AIPlayer extends Dice_Player.Player {
        private int difficulty;
        private Random random = new Random();

        public AIPlayer(Dice_Player outer, String name, int difficulty) {
            outer.super(name); // 必须调用父类构造函数以绑定外部类实例
            this.difficulty = difficulty;
        }

        @Override
        public int[] makeDecision(int[] currentBid, int totalDiceCount) {
            List<Dice_Player.Dice> myHand = getDice();

            // 1. 基础参数计算
            int playerCount = Math.max(2, totalDiceCount / 5);
            int minStartQty = playerCount + 1; // 规则起叫底线

            // 2. 统计手牌点数（用于后续决策）
            Map<Integer, Integer> myCounts = new HashMap<>();
            for (int i = 1; i <= 6; i++) myCounts.put(i, 0);
            for (Dice_Player.Dice d : myHand) {
                myCounts.put(d.getValue(), myCounts.get(d.getValue()) + 1);
            }

            // 3. 解析当前局面信息
            int currentFace = (currentBid == null) ? 0 : currentBid[1];
            int currentQty = (currentBid == null) ? 0 : currentBid[0];
            boolean isZhai = (currentBid != null && currentBid.length > 2) && (currentBid[2] == 1);

            // 4. 质疑逻辑：深度胜率评估
            if (difficulty == 2 && currentBid != null) {
                double probability = calculateProbability(currentQty, currentFace, isZhai, totalDiceCount);
                if (probability < 0.15) return null; // 预期概率过低，选择“开牌”
            }

            // 5. 首叫逻辑（作为发起者）
            if (currentBid == null) {
                int bestFace = getBestFaceExcluding(myCounts, -1); // 选手里最多的点数
                return new int[]{minStartQty, (bestFace == -1 ? 2 : bestFace), 0};
            }

            // --- 核心博弈决策区 ---
            int nextFace = currentFace;
            boolean nextZhai = isZhai;
            int newQty = currentQty;
            boolean ruleChanged = false; // 标记是否发生了斋飞转换

            // A. 规则跳变决策（斋飞转换）
            // 只有高难度 AI 会尝试变换规则，概率设为 50%
            if (difficulty > 0 && Math.random() < 0.5) {
                if (!isZhai) {
                    // 【飞变斋】：允许数量减产，但不能低于起叫线
                    if (currentQty > minStartQty) {
                        nextZhai = true;
                        newQty = (int) Math.ceil(currentQty / 2.0); // 优化：按规则减产
                        if (newQty < minStartQty) newQty = minStartQty;
                        ruleChanged = true;
                    }
                } else {
                    // 【斋变飞】：数量强制翻倍
                    nextZhai = false;
                    newQty = currentQty * 2;
                    ruleChanged = true;
                }
            }

            // 如果没有发生规则跳变，执行常规加数逻辑
            if (!ruleChanged) {
                int addQty = 1;
                double qtyRand = Math.random();
                if (qtyRand > 0.4) addQty = 3;      // 60% 概率大幅跳叫
                else if (qtyRand > 0.1) addQty = 2; // 30% 概率中幅跳叫
                else addQty = 1;                    // 10% 概率稳扎稳打
                newQty = currentQty + addQty;
            }

            // B. 点数变换决策
            boolean faceChanged = false;

            // 【互斥优化】：如果规则变了(ruleChanged)，保持点数不变；否则按概率换点
            if (!ruleChanged && Math.random() < 0.66) {
                int bestSwitchFace = getBestFaceExcluding(myCounts, currentFace);
                if (Math.random() < 0.3 || bestSwitchFace == -1) {
                    // 吹牛：随机选一个点数
                    int bluffFace = currentFace;
                    while (bluffFace == currentFace) {
                        bluffFace = random.nextInt(5) + 2;
                    }
                    nextFace = bluffFace;
                } else {
                    nextFace = bestSwitchFace;
                }
                faceChanged = true;
            } else if (ruleChanged) {
                nextFace = currentFace; // 规则变，点数锁死
                faceChanged = false;
            }

            // C. 逻辑安全网
            if (!faceChanged && (nextZhai == isZhai)) {
                if (newQty <= currentQty) newQty = currentQty + 1;
            }
            if (newQty < minStartQty) newQty = minStartQty;

            return new int[]{newQty, nextFace, nextZhai ? 1 : 0};
        }

        private int getBestFaceExcluding(Map<Integer, Integer> counts, int excludeFace) {
            int bestFace = -1;
            int maxCount = -1;
            for (int i = 2; i <= 6; i++) {
                if (i == excludeFace) continue;
                int count = counts.get(i);
                if (count > maxCount) {
                    maxCount = count;
                    bestFace = i;
                }
            }
            return bestFace;
        }

        private double calculateProbability(int qty, int face, boolean zhai, int totalDice) {
            double p = zhai ? (1.0 / 6.0) : (1.0 / 3.0);
            double expected = totalDice * p;
            if (qty > expected + 3) return 0.05;
            if (qty >= expected + 1.5) return 0.12;
            if (qty >= expected + 1.0) return 0.20;
            return 0.4;
        }
    }
}