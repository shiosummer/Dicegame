import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AI_Logic {
    class AIPlayer extends Dice_Player.Player {
        private int difficulty;//难度选择//

        public AIPlayer(Dice_Player outer, String name, int difficulty) {
            outer.super(name);
            this.difficulty = difficulty;
        }

        @Override
        public int[] makeDecision(int[] currentBid, int totalDiceCount) {
            List<Dice_Player.Dice> myHand = getDice();
            //计算最低起叫数量
            int playerCount = totalDiceCount / 5;
            int minStartQty = playerCount + 1;
            //初级ai实现//
            if (difficulty == 0) {
                if (currentBid == null){
                    return new int[]{minStartQty, 2, 0};
                }
                return new int[]{currentBid[0] + 1, currentBid[1],0};
            }
            //高级ai实现，统计点数//
            Map<Integer, Integer> myCounts = new HashMap<>();
            for (int i = 1; i <= 6 ;i++) myCounts.put(i, 0);
            for (Dice_Player.Dice d : myHand) {
                int val = d.getValue();
                myCounts.put(d.getValue(), myCounts.get(d.getValue()) + 1);
            }
            //解析当前叫点//
            int currentFace = (currentBid == null) ? 0 : currentBid[1];
            int currentQty = (currentBid == null) ? 0 : currentBid[0];
            boolean isZhai = (currentBid !=null && currentBid.length > 2) && (currentBid[2] == 1);
            //判断是否开前一个人//
            if (difficulty == 2 && currentBid != null) {
                double probability = calculateProbability(currentQty,currentFace,isZhai,totalDiceCount);
                if (probability < 0.15) return null;
            }
            //决策如何叫点
            if (currentBid == null){
                int bestFace = 2;
                int maxC = -1;
                for (int i=2; i<=6; i++){
                    if (myCounts.get(i) > maxC){
                        maxC = myCounts.get(i);
                        bestFace = i;
                    }
                }
                //返回结果【数量 点数 是否斋】
                return new int[]{minStartQty, bestFace, 0};
            }
            //正常跟注：数量+1
            return new int[]{currentQty + 1, currentFace, isZhai ? 1 : 0};
        }
        //简单ai辅助//
        private  double calculateProbability(int qty,int face,boolean zhai,int totalDice){
            //基础概率，期望值，风险评估
            double p = zhai ? (1.0/6.0) : (1.0/3.0);
            double expected = totalDice * p;
            if (qty >expected + 3)return 0.05;
            if (qty == expected + 1.5)return 0.12;
            return 0.4;
        }
        private int[] randomBid(int[] currentBid){
            return null;
        }
    }
}
