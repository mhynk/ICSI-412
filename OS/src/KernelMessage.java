import java.util.Arrays;

public class KernelMessage {
    public int senderPid;
    public int receiverPid;
    public int what; //header, tag
    public byte[] data; //real data

    public KernelMessage(int receiverPid, int what, byte[] data) {
        this.receiverPid = receiverPid;
        this.what = what;
        this.data = data;
    }

    public KernelMessage(KernelMessage original) {
        this.senderPid = original.senderPid;
        this.receiverPid = original.receiverPid;
        this.what = original.what;
        this.data = (original.data != null) ? original.data.clone() : null; //ensuring separate memory spaces
    }

    @Override
    public String toString() {
        return "From: " + senderPid + " To: " + receiverPid + " What: " + what;
    }
}
