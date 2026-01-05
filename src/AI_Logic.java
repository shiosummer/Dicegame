import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AI_Logic {
    class AIPlayer extends Dice_Player.Player {
        private int difficulty; // 难度选择
        private Random random = new Random();
        public AIPlayer(Dice_Player outer, String name, int difficulty) {
            outer.super(name);
            this.difficulty = difficulty;
        }
        @Override
        public int[] makeDecision(int[] currentBid, int totalDiceCount) {
            List<Dice_Player.Dice> myHand = getDice();
            // 计算最低起叫数量
            int playerCount = Math.max(2, totalDiceCount / 5);
            int minStartQty = playerCount + 1;
            // 初级AI实现
            if (difficulty == 0) {
                if (currentBid == null) return new int[]{minStartQty, 2, 0};
                return new int[]{currentBid[0] + 1, currentBid[1], 0};
            }
            // 统计点数
            Map<Integer, Integer> myCounts = new HashMap<>();
            for (int i = 1; i <= 6; i++) myCounts.put(i, 0);
            for (Dice_Player.Dice d : myHand) {
                myCounts.put(d.getValue(), myCounts.get(d.getValue()) + 1);
            }
            // 解析当前叫点
            int currentFace = (currentBid == null) ? 0 : currentBid[1];
            int currentQty = (currentBid == null) ? 0 : currentBid[0];
            boolean isZhai = (currentBid != null && currentBid.length > 2) && (currentBid[2] == 1);
            // 判断是否开前一个人
            if (difficulty == 2 && currentBid != null) {
                double probability = calculateProbability(currentQty, currentFace, isZhai, totalDiceCount);
                if (probability < 0.15) return null;
            }
            // 首叫逻辑
            if (currentBid == null) {
                int bestFace = 2;
                int maxC = -1;
                for (int i = 2; i <= 6; i++) {
                    if (myCounts.get(i) > maxC) {
                        maxC = myCounts.get(i);
                        bestFace = i;
                    }
                }
                return new int[]{minStartQty, bestFace, 0};
            }

            // --- 核心逻辑开始 ---
            int nextFace = currentFace;
            boolean nextZhai = isZhai;
            // 1. 先计算跳叫步长 (addQty)
            int addQty = 1;
            double qtyRand = Math.random();
            if (difficulty > 0) {
                if (qtyRand > 0.4) addQty = 3;
                else if (qtyRand > 0.1) addQty = 2;
                else addQty = 1;
            }
            // 2. 预设基础数量
            int newQty = currentQty + addQty;
            // 3. 随机进退斋 (在此处应用 addQty，确保跳叫不被覆盖)
            if (difficulty > 0 && Math.random() < 0.5) {
                if (!isZhai) {
                    nextZhai = true;
                    // 飞变斋：规则允许减1，加上跳叫偏移
                    newQty = Math.max(minStartQty, currentQty - 1 + addQty);
                } else {
                    nextZhai = false;
                    // 斋变飞：翻倍
                    newQty = currentQty * 2;
                }
            }
            // 4. 随机变换点数
            boolean faceChanged = false;
            if (Math.random() < 0.66) {
                int oldFace = nextFace;
                boolean isBluffing = Math.random() < 0.3;
                if (isBluffing) { // 吹牛模式
                    int bluffFace = currentFace;
                    while (bluffFace == currentFace) {
                        bluffFace = random.nextInt(5) + 2;
                    }
                    nextFace = bluffFace;
                } else { // 老实人模式
                    int bestSwitchFace = getBestFaceExcluding(myCounts, currentFace);
                    if (bestSwitchFace != -1) nextFace = bestSwitchFace;
                }
                if (nextFace != oldFace) faceChanged = true;
            }
            // 5. 最后的保险逻辑
            // 只有点数没变 且 斋飞状态没变时，才强制数量递增
            boolean zhaiStatusChanged = (nextZhai != isZhai);
            if (!faceChanged && !zhaiStatusChanged) {
                if (newQty <= currentQty) {
                    newQty = currentQty + 1;
                }
            }
            // 确保不低于最低起叫
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
        private int[] randomBid(int[] currentBid) {
            return null;
        }
    }
}