import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Dice_Player {
    class Dice{
        private int value;
        private Random random = new Random();
        public void roll(){
            this.value = random.nextInt(6)+1;
        }
        //摇单独的一个骰子//
        public int getValue(){
            return value;
        }
    }

    class DiceCup{
        private List<Dice> dicelist;
        public DiceCup(int count){
            dicelist = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                dicelist.add(new Dice());
            }//将摇出来的骰子的结果加入到列表中//
        }
        public void shake(){
            for (Dice dice : dicelist) dice.roll();
        }//摇骰子，并把结果返回出来//
        public List<Dice> getDice(){
            return dicelist;
        }
    }

    abstract class Player{
        protected String name;
        protected DiceCup diceCup;
        protected boolean isopen;
        public Player(String name){
            this.name = name;
            this.diceCup = new DiceCup(5);
        }
        public void rollDice(){
            diceCup.shake();
        }
        public String getName(){
            return name;
        }
        public List <Dice> getDice(){
            return diceCup.getDice();
        }
        public abstract int[] makeDecision(int[] currentBid,int totalDiceCount);
    }
}
