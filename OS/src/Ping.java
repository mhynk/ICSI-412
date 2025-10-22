public class Ping extends UserlandProcess {
    @Override
    public void main() {
        int myPid = OS.GetPID();
        //int pongPid = OS.GetPidByName("Pong");
        int pongPid = -1;
        while (pongPid == -1) {
            pongPid = OS.GetPidByName("Pong");
            if (pongPid == -1) OS.Sleep(10);
        }

        System.out.println("I am ping, pong = " + pongPid);

        for(int i = 0; i < 3; i++) {
            KernelMessage km = new KernelMessage(pongPid, i, null);
            OS.SendMessage(km);

            KernelMessage reply = OS.WaitForMessage();
            if (reply != null) {
                System.out.println(" PING: from: " + reply.senderPid + " to: " + reply.receiverPid + " what: " + reply.what);
            }
        }

        OS.Exit();
    }
}
