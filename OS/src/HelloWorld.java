public class HelloWorld extends UserlandProcess{
    @Override
    public void main() {
        while (true) {
            System.out.println("Hello World");
            cooperate();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
