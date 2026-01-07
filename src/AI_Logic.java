import java.util.*;

public class AI_Logic {
    public class AIPlayer extends Dice_Player.Player {
        private int difficulty;
        private Random random = new Random();
        private Dice_Player outer;

        public AIPlayer(Dice_Player outer, String name, int difficulty) {
            outer.super(name);
            this.outer = outer;
            this.difficulty = difficulty;
        }

        @Override
        public int[] makeDecision(int[] currentBid, int totalDiceCount) {
            List<Dice_Player.Dice> myHand = getDice();

            // ✨ 修复 1：正确计算人头数
            int playerCount = totalDiceCount / 5;

            // 2. 统计手牌点数
            Map<Integer, Integer> myCounts = new HashMap<>();
            for (int i = 1; i <= 6; i++) myCounts.put(i, 0);
            for (Dice_Player.Dice d : myHand) {
                myCounts.put(d.getValue(), myCounts.get(d.getValue()) + 1);
            }

            // 3. 解析局面
            int currentFace = (currentBid == null) ? 0 : currentBid[1];
            int currentQty = (currentBid == null) ? 0 : currentBid[0];
            boolean isZhai = (currentBid != null && currentBid.length > 2) && (currentBid[2] == 1);

            // 4. 质疑逻辑 (保持你的原始胜率评估)
            if (difficulty == 2 && currentBid != null) {
                double probability = calculateProbability(currentQty, currentFace, isZhai, totalDiceCount);
                if (probability < 0.15) return null;
            }

            // ✨ 修复 2：首叫逻辑 (增加“飞”局人头数+1的规则)
            if (currentBid == null) {
                int bestFace = getBestFaceExcluding(myCounts, -1);
                int face = (bestFace == -1 ? 2 : bestFace);

                // 判断首叫是斋还是飞（通常首叫选飞，除非选1点）
                boolean startZhai = (face == 1);
                // 🚀 核心规则：起叫飞必须人头数+1，起叫斋只需人头数
                int startQty = startZhai ? playerCount : (playerCount + 1);

                return new int[]{startQty, face, startZhai ? 1 : 0};
            }

            // --- 核心博弈决策区 ---
            int nextFace = currentFace;
            boolean nextZhai = isZhai;
            int newQty = currentQty;
            boolean ruleChanged = false;

            // A. 规则跳变决策 (飞转斋-1，斋转飞翻倍)
            if (difficulty > 0 && Math.random() < 0.5) {
                if (!isZhai) {
                    nextZhai = true;
                    newQty = Math.max(playerCount, currentQty - 1); // 飞转斋：减产
                    ruleChanged = true;
                } else {
                    nextZhai = false;
                    newQty = currentQty * 2; // 斋转飞：翻倍
                    ruleChanged = true;
                }
            }

            // 你的 0.4/0.1 概率跳叫逻辑
            if (!ruleChanged) {
                int addQty;
                double qtyRand = Math.random();
                if (qtyRand > 0.4) addQty = 3;
                else if (qtyRand > 0.1) addQty = 2;
                else addQty = 1;
                newQty = currentQty + addQty;
            }

            // B. 点数变换决策 (完全保留你的 0.66/0.3 逻辑)
            boolean faceChanged = false;
            if (!ruleChanged && Math.random() < 0.66) {
                int bestSwitchFace = getBestFaceExcluding(myCounts, currentFace);
                if (Math.random() < 0.3 || bestSwitchFace == -1) {
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
                nextFace = currentFace;
                faceChanged = false;
            }

            // C. 1点必斋强制校验
            if (nextFace == 1) {
                if (!isZhai) {
                    newQty = Math.max(playerCount, currentQty - 1);
                }
                nextZhai = true;
            }

            // 逻辑安全网
            if (!faceChanged && (nextZhai == isZhai)) {
                if (newQty <= currentQty) newQty = currentQty + 1;
            }

            // 确保不低于当前的最低起叫线
            int absoluteMin = nextZhai ? playerCount : (playerCount + 1);
            if (newQty < absoluteMin) newQty = absoluteMin;

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