import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AI_Logic {
    class AIPlayer extends Dice_Player.Player {
        private int difficulty;
        private Random random = new Random();

        public AIPlayer(Dice_Player outer, String name, int difficulty) {
            outer.super(name);
            this.difficulty = difficulty;
        }

        @Override
        public int[] makeDecision(int[] currentBid, int totalDiceCount) {
            List<Dice_Player.Dice> myHand = getDice();

            // 1. 基础参数计算
            int playerCount = Math.max(2, totalDiceCount / 5);
            int minStartQty = playerCount + 1; // 规则起叫底线

            // 2. 统计手牌点数（用于后续“老实人”模式决策）
            Map<Integer, Integer> myCounts = new HashMap<>();
            for (int i = 1; i <= 6; i++) myCounts.put(i, 0);
            for (Dice_Player.Dice d : myHand) {
                myCounts.put(d.getValue(), myCounts.get(d.getValue()) + 1);
            }

            // 3. 解析当前局面信息
            int currentFace = (currentBid == null) ? 0 : currentBid[1];
            int currentQty = (currentBid == null) ? 0 : currentBid[0];
            boolean isZhai = (currentBid != null && currentBid.length > 2) && (currentBid[2] == 1);

            // 4. 质疑逻辑：胜率评估
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
                    // 【飞变斋】：允许数量减产
                    // 仅当当前数量大于起叫底线时执行，否则会造成非法叫号
                    if (currentQty > minStartQty) {
                        nextZhai = true;
                        newQty = currentQty - 1;
                        ruleChanged = true;
                    } else {
                        newQty = currentQty + 1; // 数量不够减，退回常规加数
                    }
                } else {
                    // 【斋变飞】：数量强制翻倍
                    nextZhai = false;
                    newQty = currentQty * 2;
                    ruleChanged = true;
                }
            } else {
                // --- 常规叫号决策（跳变步长） ---
                int addQty = 1;
                double qtyRand = Math.random();
                if (qtyRand > 0.4) addQty = 3;      // 60% 概率大幅跳叫
                else if (qtyRand > 0.1) addQty = 2; // 30% 概率中幅跳叫
                else addQty = 1;                    // 10% 概率稳扎稳打
                newQty = currentQty + addQty;
            }

            // B. 点数变换决策
            boolean faceChanged = false;

            // 【优化逻辑】：如果规则已经变了(ruleChanged)，则保持点数不变，增加迷惑性
            // 如果规则没变，则按 66% 的概率尝试换点叫号
            if (!ruleChanged && Math.random() < 0.66) {
                // 优先选手里有的点数（老实人），否则吹牛
                int bestSwitchFace = getBestFaceExcluding(myCounts, currentFace);
                if (Math.random() < 0.3 || bestSwitchFace == -1) {
                    int bluffFace = currentFace;
                    while (bluffFace == currentFace) {
                        bluffFace = random.nextInt(5) + 2; // 吹牛点数选 2-6
                    }
                    nextFace = bluffFace;
                } else {
                    nextFace = bestSwitchFace;
                }
                faceChanged = true;
            } else if (ruleChanged) {
                // 规则变了，强制锁定点数不许变
                nextFace = currentFace;
                faceChanged = false;
            }

            // C. 逻辑安全网（保险逻辑）
            boolean zhaiStatusChanged = (nextZhai != isZhai);

            // 1. 如果点数、规则都没变，数量必须绝对递增，否则是废号
            if (!faceChanged && !zhaiStatusChanged) {
                if (newQty <= currentQty) newQty = currentQty + 1;
            }
            // 2. 如果发生了“飞变斋”，点数没变是合法的，但需确保数量不低于起叫线
            else if (nextZhai && !isZhai && !faceChanged) {
                if (newQty < minStartQty) newQty = minStartQty;
            }

            // 最终兜底：任何决策不得低于起叫底线
            if (newQty < minStartQty) newQty = minStartQty;

            return new int[]{newQty, nextFace, nextZhai ? 1 : 0};
        }

        /**
         * 寻找手牌中除了排除点数外，数量最多的点数
         */
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

        /**
         * 简单概率估算逻辑
         */
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