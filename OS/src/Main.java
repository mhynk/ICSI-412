public class Main {
    public static void main(String[] args) {
        OS.Startup(new Init());
    }
}

//syscall의 핵심 흐름(유저 → OS → Kernel → Scheduler)
