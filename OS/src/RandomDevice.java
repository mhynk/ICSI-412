import java.util.Random;

public class RandomDevice implements Device {
    private Random[] randoms = new Random[10];

    @Override
    public int Open(String s) {
        for (int i = 0; i < randoms.length; i++) {
            if (randoms[i] == null) {
                if (s != null && !s.isEmpty()) {
                    int seed = Integer.parseInt(s);
                    randoms[i] = new Random(seed);
                } else {
                    randoms[i] = new Random();
                }
                return i;
            }
        }
       return -1;
    }

    @Override
    public void Close(int id){
        randoms[id] = null;
    }

    @Override
    public byte[] Read(int id, int size){
        byte[] data = new byte[size]; //buffer(temp) array
        randoms[id].nextBytes(data);
        return data;
    }

    @Override
    public void Seek(int id, int to) {
        byte[] tmp = new byte[to]; //just read as much as to and throw it out
        randoms[id].nextBytes(tmp);
    }

    @Override
    public int Write(int id, byte[] data){
        return 0;
    }
}
