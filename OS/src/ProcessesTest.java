public class ProcessesTest {

    //realtime - long : check demotion
    static class RealtimeLongProcess extends UserlandProcess {
        @Override
        public void main() {
            for (int i = 0; i < 50; i++) {
                System.out.println("Real time Iteration: " + i);
                cooperate();
            }
            System.out.println("Real time Iteration: 51");
            OS.Exit();
        }
    }

    //realtime - sleep : no demotion
    static class RealtimeSleepProcess extends UserlandProcess {
        @Override
        public void main() {
            for (int i = 0; i < 5; i++) {
                System.out.println("Real time sleep..Good night");
                OS.Sleep(300);
                System.out.println("Real time sleep..Wake up!");
                cooperate();
            }
            OS.Exit();
        }
    }

    static class RealtimeProcess extends UserlandProcess {
        @Override
        public void main() {
            for (int i = 0; i < 5; i++) {
                System.out.println("Real time Iteration: " + i);
                cooperate();
            }
            OS.Exit();
        }
    }

    //interactive process check
    static class InteractiveProcess extends UserlandProcess {
        /*@Override
        public void main() {
            System.out.println(">>> Interactive started, PID=" + OS.GetPID());
            for (int i = 0; i < 20; i++) {
                System.out.println("Interactive Iteration: " + i);
                cooperate();
            }
            OS.Exit();
        }*/
        @Override
        public void main() {
            System.out.println(">>> Interactive process started, PID=" + OS.GetPID());
            for (int i = 0; i < 50; i++) {
                System.out.println("Interactive Iteration: " + i);
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                cooperate();
            }
            OS.Exit();
        }
    }

    //background process check
    static class BackgroundProcess extends UserlandProcess {
        /*@Override
        public void main() {
            System.out.println(">>> Background started, PID=" + OS.GetPID());

            for (int i = 0; i < 20; i++) {
                System.out.println("Background Iteration: " + i);
                cooperate();
            }
            OS.Exit();
        }*/
        @Override
        public void main() {
            System.out.println(">>> Background process started, PID=" + OS.GetPID());
            for (int i = 0; i < 5; i++) {
                System.out.println("Background Iteration: " + i);
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                cooperate();
            }
            OS.Exit();
        }
    }
}
