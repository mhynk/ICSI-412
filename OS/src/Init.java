public class Init extends UserlandProcess { //for cooperate

    @Override
    public void main() {
        //OS.CreateProcess(new HelloWorld(), OS.PriorityType.interactive);
        //OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.interactive);

        /*
        while (true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            cooperate();
        }
        */

        /*
        OS.CreateProcess(new ProcessesTest.RealtimeLongProcess(), OS.PriorityType.realtime);
        OS.CreateProcess(new ProcessesTest.RealtimeSleepProcess(), OS.PriorityType.realtime);
        OS.CreateProcess(new ProcessesTest.RealtimeProcess(), OS.PriorityType.realtime);
        OS.CreateProcess(new ProcessesTest.InteractiveProcess(), OS.PriorityType.interactive);
        OS.CreateProcess(new ProcessesTest.BackgroundProcess(), OS.PriorityType.background);
         */

        //OS.CreateProcess(new DeviceTest(), OS.PriorityType.interactive);

        OS.CreateProcess(new Pong(), OS.PriorityType.interactive); //PID = 2
        OS.CreateProcess(new Ping(), OS.PriorityType.interactive); //PID = 3
        //OS.Sleep(50);
        //OS.CreateProcess(new Pong(), OS.PriorityType.interactive);
        OS.Exit();
    }
}
