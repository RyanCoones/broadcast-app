public class RunAllServers {

    public static void main(String[] args) {
        // Start ChatServer
        new Thread(() -> {
            try {
                Chat.ChatServer.main(new String[0]);
            } catch (Exception e) {
                System.err.println("DrawBoardServer failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        // Start DrawBoardServer
        new Thread(() -> {
            try {
                Drawboard.DrawBoardServer drawBoardServer = new Drawboard.DrawBoardServer(5050);
            } catch (Exception e) {
                System.err.println("DrawBoardServer failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        // Start FileShareServer
        new Thread(() -> {
            try {
                FileShare.FileShareServer.main(new String[0]);
            } catch (Exception e) {
                System.err.println("FileShareServer failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        // Start TicTacToeServer
        new Thread(() -> {
            try {
                tictactoe.TicTacToeServer.main(new String[0]);
            } catch (Exception e) {
                System.err.println("TicTacToeServer failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        System.out.println("All servers are starting...");
    }
}
