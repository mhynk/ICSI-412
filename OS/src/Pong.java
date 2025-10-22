public class Pong extends UserlandProcess {
    @Override
    public void main() {
        int myPid = OS.GetPID();
        int pingPid = OS.GetPidByName("ping");
        System.out.println("I am pong, ping = " + pingPid);

        for(int i = 0; i < 3; i++) {
            KernelMessage msg = OS.WaitForMessage();

            if (msg != null) {
                System.out.println(" Pong: from: " + msg.senderPid + " to: " + msg.receiverPid + " what: " + msg.what);

                KernelMessage reply = new KernelMessage(pingPid, msg.what, null);
                OS.SendMessage(reply);
            }
        }
        OS.Exit();
    }
}
