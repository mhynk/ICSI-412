public class TLBTest {

    static class TLBFilling extends UserlandProcess {
        @Override
        public void main() {
            System.out.println("TLB Filling process started, PID = " + OS.GetPID());

            Hardware.updateTLB(0, 3);
            Hardware.updateTLB(1, 7);

            cooperate();

            OS.Exit();
        }
    }

    static class TLBCheckProcess extends UserlandProcess {
        @Override
        public void main() {
            System.out.println("TLB Check process started, PID = " + OS.GetPID());

            //real verification (Does TLB is all empty?)
            boolean cleared = true;
            for (int i = 0; i < 2; i++) {
                if (Hardware.TLB[i][0] != -1 || Hardware.TLB[i][1] != -1) {
                    cleared = false;
                }
            }

            if (cleared) {
                System.out.println("Success : TLB cleared");
            } else {
                System.out.println("Failure : TLB not cleared");
            }
            OS.Exit();
        }
    }

    /*private static void printTLB() {
        for(int i = 0; i < 2; i++) {
            System.out.println("TLB[" + i + "] VP=" + Hardware.TLB[i][0] + ", PP=" + Hardware.TLB[i][1]);
        }
    }*/
}
